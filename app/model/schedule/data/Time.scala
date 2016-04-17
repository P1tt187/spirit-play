package model.schedule.data

import play.api.libs.json.Json

/**
  * @author fabian 
  *         on 04.04.16.
  */
object Time {
  implicit val format = Json.format[Time]
}

case class Time(startHour: Int, startMinute: Int, stopHour: Int, stopMinute: Int, weekday: String)