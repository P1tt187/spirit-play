package model.schedule.data

import play.api.libs.json.Json


/**
  * @author fabian 
  *         on 03.04.16.
  */


object Lecture {
  implicit val format = Json.format[Lecture]
}

case class Lecture(duration: String, groupIndex: String, time: Time, docents: List[String], room: String, lectureKind: String, lectureName: String, course: List[String], scheduleKind: String, longTitle: String, uuid: String, alternatives: List[Alternative])

object Schedule {
  implicit val format = Json.format[Schedule]
}

case class Schedule(title: String, scheduleData: List[Lecture])

