package controllers

/**
  * @author fabian 
  *         on 11.04.16.
  */

import model.database.{LectureDA, ScheduleDA}
import model.schedule.data.{Lecture, Schedule}
import play.api.mvc._
import views.html.shortcuts._

class ShortCutController extends AbstractController {

  def index() = Action {
    implicit request =>

      val courses = courseNames.flatMap(_._2)

      val allLectures = LectureDA.findAll[Lecture]() ++ ScheduleDA.findAll[Schedule]().flatMap(_.scheduleData)
      val filteredLectures = allLectures.filter(l => courses.exists(c => l.course.map(_.toUpperCase).contains(c.toUpperCase())))
      val titleMap = filteredLectures.map(l => (l.lectureName, l.longTitle)).toMap
      Ok(shortcut(titleMap))
  }

}
