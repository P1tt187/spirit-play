package model.schedule.data

import play.api.libs.json.Json

/**
  * @author fabian 
  *         on 12.04.16.
  */

object Student {
  implicit val format = Json.format[Student]
}

case class Student(firstName:String, lastname:String)

object Group {
  implicit val format = Json.format[Group]
}

case class Group(className:String, groupType:String, groupIndex:Int, students: List[Student])



