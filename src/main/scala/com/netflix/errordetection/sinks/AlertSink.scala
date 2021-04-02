package com.netflix.errordetection.sinks

import org.apache.flink.connector.file.sink.FileSink
import com.netflix.errordetection.operators.WindowedErrorStats
import org.apache.flink.api.common.serialization.Encoder
import org.apache.flink.core.fs.Path

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter

/**
 * Processes time ranges of elevated error rates.
 *
 * A production impl of this may rollback deploys or page engineering teams.
 * For this exercise, we're writing alert time ranges to a CSV for ingestion by visualization tools.
 */
object AlertSink {

  def csvFileSink(outputPath: String): FileSink[WindowedErrorStats] =
    FileSink.forRowFormat(new Path(outputPath), new Encoder[WindowedErrorStats] {
       override def encode(in: WindowedErrorStats, outputStream: OutputStream): Unit = {
         val startTime = in.startTime.format(DateTimeFormatter.ISO_LOCAL_TIME)
         val endTime = in.endTime.format(DateTimeFormatter.ISO_LOCAL_TIME)
         outputStream.write(s"$startTime,$endTime,${in.errorCode}\n".getBytes(StandardCharsets.UTF_8))
       }
     })
    .build()

}
