package com.netflix.errordetection.operators

import com.netflix.errordetection.model.ApplicationErrorEvent
import org.apache.flink.api.common.functions.AggregateFunction

import java.time.LocalTime


case class BuildVersionErrorCodeAcc(oldVersionCnt: Int = 0,
                                    newVersionCnt: Int = 0,
                                    errorCode: Int = 0,
                                    minTime: LocalTime = LocalTime.MAX,
                                    maxTime: LocalTime = LocalTime.MIN)

/**
 * Provides the percentage and nominal increase in errors for a given code and time range.
 */
case class WindowedErrorStats(percentIncrease: Double,
                              eventCountDelta: Int,
                              startTime: LocalTime,
                              endTime: LocalTime,
                              errorCode: Int)

/**
 * Measures the relative increase in error rates for a given error code when compared to a baseline version id. Must
 * be used with a windowed stream keyed on error code.
 *
 * @param baselineVersion The buildVersionId to compare error increases against.
 */
class BuildVersionErrorCodeAggregator(baselineVersion: String) extends
  AggregateFunction[ApplicationErrorEvent, BuildVersionErrorCodeAcc, WindowedErrorStats]{

  override def createAccumulator(): BuildVersionErrorCodeAcc = BuildVersionErrorCodeAcc()

  override def add(in: ApplicationErrorEvent, acc: BuildVersionErrorCodeAcc): BuildVersionErrorCodeAcc = BuildVersionErrorCodeAcc(
    oldVersionCnt = if(in.buildVersion == baselineVersion) acc.oldVersionCnt + 1 else acc.oldVersionCnt,
    newVersionCnt = if(in.buildVersion != baselineVersion) acc.newVersionCnt + 1 else acc.newVersionCnt,
    errorCode = in.errorCode,
    minTime = if(acc.minTime.isBefore(in.time)) acc.minTime else in.time,
    maxTime = if(acc.maxTime.isAfter(in.time)) acc.maxTime else in.time
  )

  override def getResult(acc: BuildVersionErrorCodeAcc): WindowedErrorStats = WindowedErrorStats(
    percentIncrease = if (acc.oldVersionCnt == 0) {
      acc.newVersionCnt.toDouble * 100.0
    } else {
      (acc.newVersionCnt.toDouble / acc.oldVersionCnt.toDouble) * 100.0
    },
    eventCountDelta = acc.newVersionCnt - acc.oldVersionCnt,
    startTime = acc.minTime,
    endTime = acc.maxTime,
    errorCode = acc.errorCode
  )

  override def merge(acc: BuildVersionErrorCodeAcc, acc1: BuildVersionErrorCodeAcc): BuildVersionErrorCodeAcc = BuildVersionErrorCodeAcc(
    oldVersionCnt = acc.oldVersionCnt + acc1.oldVersionCnt,
    newVersionCnt = acc.newVersionCnt + acc1.newVersionCnt,
    minTime = if(acc.minTime.isBefore(acc1.minTime)) acc.minTime else acc1.minTime,
    maxTime = if(acc.maxTime.isAfter(acc1.maxTime)) acc.maxTime else acc1.maxTime,
    errorCode = acc.errorCode
  )
}
