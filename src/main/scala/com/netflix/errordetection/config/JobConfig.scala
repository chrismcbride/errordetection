package com.netflix.errordetection.config

import java.net.URI


/** Provides configuration parameters for [[com.netflix.errordetection.ErrorRateDetectionJob]]
 *
 * @param inputCSVFilePath a file uri for a CSV containing records in the format `H:MM:SS,Build_Version,Error_Code`
 * @param outputCSVFilePath a file uri for outputting a CSV containing time ranges of increased errors in for the
 *                          format `start_time,end_time,Error_code`
 * @param baselineVersionId the currently deployed Build_Version, used as a baseline to measure error increases against
 * @param minErrorPercentIncrease the minimum percentage increase of a specific error seen within a sample window
 *                                required to trigger an alert
 * @param minErrorCountDelta the minimum count of increased errors seen within a sample window required to trigger
 *                           an alert.
 * @param windowSize The timeframe used to sample events. A larger window means a more accurate result at the downside
 *                   of having a slower reaction time to sudden events.
 * @param windowSlideSize The amount the window should slide by after each evaluation.
 */
case class JobConfig(inputCSVFilePath: String,
                     outputCSVFilePath: String,
                     baselineVersionId: String,
                     minErrorPercentIncrease: Int,
                     minErrorCountDelta: Int,
                     windowSizeSeconds: Long,
                     windowSlideSizeSeconds: Long)

object JobConfig {

  // Hard-coding config values here for the purpose of this exercise. A production impl would parse these from a file
  // or a config service depending on how this is deployed.
  def create(inputPath: URI, outputPath: URI): JobConfig = JobConfig(
    inputCSVFilePath = inputPath.toString,
    outputCSVFilePath = outputPath.toString,
    baselineVersionId = "5.0007.510.011",
    minErrorPercentIncrease = 200,
    minErrorCountDelta = 3,
    windowSizeSeconds = 6 * 60,
    windowSlideSizeSeconds = 90
  )

}
