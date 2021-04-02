package com.netflix.errordetection.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

/**
 * Represents a single client error
 */
case class ApplicationErrorEvent(time: LocalTime, buildVersion: String, errorCode: Int)

/**
 * A record parsing failure.
 */
case class InvalidInput(record: String, exception: Throwable)

object ApplicationErrorEvent {

  /**
   * @param csv A string in the form of `H:mm:ss,Build_Version,Error_Code`
   */
  def fromCSV(csv: String): Either[InvalidInput, ApplicationErrorEvent] = {
    val columns = csv.split(',')
    if(columns.length != 3) {
      Left(InvalidInput(csv, new IllegalArgumentException("Invalid csv, expected 3 columns")))
    } else {
      val time = Try(LocalTime.parse(columns(0), DateTimeFormatter.ofPattern("H:mm:ss")))
      val errorCode = Try(columns(2).toInt)
      val appEvent = time.flatMap(ts => errorCode.map(code => {
        ApplicationErrorEvent(time = ts, buildVersion = columns(1), errorCode = code)
      }))
      appEvent match  {
        case Success(event) => Right(event)
        case Failure(e) => Left(InvalidInput(csv, e))
      }
    }
  }

}

