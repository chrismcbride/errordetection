package com.netflix.errordetection

import com.netflix.errordetection.config.JobConfig
import com.netflix.errordetection.model.{ApplicationErrorEvent, InvalidInput}
import com.netflix.errordetection.operators.BuildVersionErrorCodeAggregator
import com.netflix.errordetection.sinks.{AlertSink, InvalidInputSink}
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time

import java.nio.file.Paths


object ErrorRateDetectionJob {

  @throws[Exception]
  def main(args: Array[String]): Unit = {
    val job = new ErrorRateDetectionJob(
      env = StreamExecutionEnvironment.createLocalEnvironment(),
      conf = JobConfig.create(Paths.get(args(0)).toUri, Paths.get("outputs/").toUri)
    )
    job.run()
  }

}

class ErrorRateDetectionJob(env: StreamExecutionEnvironment, conf: JobConfig) {

  def run(): Unit = {
    val appEvents: DataStream[Either[InvalidInput, ApplicationErrorEvent]] = env
      .readTextFile(conf.inputCSVFilePath)
      .map(ApplicationErrorEvent.fromCSV(_))

    // partition invalid events to another stream
    appEvents
      .filter(e => e.isLeft)
      .map(e => e.left.get)
      .addSink(new InvalidInputSink)
      .name("invalid-input")

    val withTimestampsAndWatermarks: DataStream[ApplicationErrorEvent] = appEvents
      .filter(e => e.isRight)
      .map(e => e.right.get)
      .assignTimestampsAndWatermarks(
        WatermarkStrategy
          .forMonotonousTimestamps()
          .withTimestampAssigner(new SerializableTimestampAssigner[ApplicationErrorEvent] {
            override def extractTimestamp(e: ApplicationErrorEvent, prevTs: Long): Long = e.time.toSecondOfDay * 1000L
          })
      )

    // We're using a sliding window per error code. Within each window we aggregate error counts between the two build
    // versions for that error code and emit the statistics to an alert sink if it meets our threshold. Since we're using
    // a sliding window its possible that there is overlap between adjacent alerts, downstream services will need to
    // handle that appropriately.

    // Assigning these to values outside of the filter closure to prevent flink from trying to serialize this class.
    val minPercent = conf.minErrorPercentIncrease
    val minErrorCount = conf.minErrorCountDelta
    withTimestampsAndWatermarks
      .keyBy(_.errorCode)
      .window(SlidingEventTimeWindows.of(
        Time.seconds(conf.windowSizeSeconds), Time.seconds(conf.windowSlideSizeSeconds)))
      .aggregate(new BuildVersionErrorCodeAggregator(conf.baselineVersionId))
      .filter(out => {
        // were checking both a percentage change and a nominal count of events to prevent noisy alerts. If we only had
        // two errors we may not want to alert on that even though it is a "200%" increase.
        out.percentIncrease >= minPercent && out.eventCountDelta >= minErrorCount
      })
      .sinkTo(AlertSink.csvFileSink(conf.outputCSVFilePath))

    val _ = env.execute("Error Detection")
  }

}

