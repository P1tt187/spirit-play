package controllers

/**
  * @author fabian 
  *         on 11.04.16.
  */
import javax.inject._
import model.database.{LectureDA, ScheduleDA}
import model.schedule.data.{Lecture, Schedule}
import play.api.mvc._
import views.html.shortcuts._
@Singleton
class ShortCutController extends AbstractController {
  /** this will create a Table with the short names and the title of a lecture */
  def index() = sessionCache.cached("shortcuts") {
    Action {
      implicit request =>

        /** we only want the names for this semester so we filter all the lecutes which contains the courses of this semester */
        val courses = courseNames.flatMap(_._2)

        val allLectures = LectureDA.findAll[Lecture]() ++ ScheduleDA.findAll[Schedule]().flatMap(_.scheduleData)
        val filteredLectures = allLectures.filter(l => courses.exists(c => l.course.map(_.toUpperCase).contains(c.toUpperCase())))
        val titleMap = filteredLectures.map(l => (l.lectureName, l.longTitle)).toMap
        Ok(shortcut(titleMap))
    }
  }

}
