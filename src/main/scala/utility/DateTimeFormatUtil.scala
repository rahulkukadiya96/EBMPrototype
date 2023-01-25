package utility

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateTimeFormatUtil {
  def fromStrToDate(format: DateTimeFormatter, date: String): Option[LocalDateTime] = {
    try {
      Some(LocalDateTime parse(date, format))
    }
    catch {
      case _: Exception => None
    }
  }

  def fromDateToStr(dateFormat: DateTimeFormatter, date: LocalDateTime): Option[String] = {
    try {
      Some(dateFormat format date)
    }
    catch {
      case _: Exception => None
    }
  }
}
