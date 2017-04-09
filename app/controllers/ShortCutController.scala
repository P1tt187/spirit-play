package controllers

/**
  * @author fabian 
  *         on 11.04.16.
  */

import javax.inject._

import model.schedule.data.MSchedule
import play.api.mvc._
import views.html.shortcuts._

@Singleton
class ShortCutController @Inject()(mSchedule: MSchedule) extends AbstractController {
  /** this will create a Table with the short names and the title of a lecture */
  def index() = sessionCache.cached("shortcuts") {
    Action {
      implicit request =>

        /** we only want the names for this semester so we filter all the lecutes which contains the courses of this semester */
        val courses = courseNames.flatMap(_._2)

        val allLectures = mSchedule.getSchedule() ++ mSchedule.getBlocks().flatMap(_.scheduleData)
        val filteredLectures = allLectures.filter(l => courses.exists(c => l.course.map(_.toUpperCase).contains(c.toUpperCase())))
        val titleMap = filteredLectures.map(l => (l.lectureName, l.longTitle)).toMap
        Ok(shortcut(titleMap))
    }
  }

}
