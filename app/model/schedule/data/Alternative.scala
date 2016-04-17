package model.schedule.data

import play.api.libs.json.Json

/**
  * @author fabian 
  *         on 17.04.16.
  */

object Alternative {
  implicit val format = Json.format[Alternative]
}

case class Alternative(weekday: String, duration: String, hour: String, room: String, lecture: String)
