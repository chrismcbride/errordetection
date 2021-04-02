package com.netflix.errordetection.sinks

import com.netflix.errordetection.model.InvalidInput
import org.apache.flink.streaming.api.functions.sink.SinkFunction

/**
 * Handles invalid input data.
 *
 * For the purposes of this exercise we are just throwing an exception and crashing the job for invalid records. A
 * production impl of this would most likely emit records to a dead letter queue for offline analysis, or it may fail
 * the job if the rate of invalid data is significant.
 */
class InvalidInputSink extends SinkFunction[InvalidInput] {

  override def invoke(value: InvalidInput, context: SinkFunction.Context): Unit = {
    throw value.exception
  }

}
