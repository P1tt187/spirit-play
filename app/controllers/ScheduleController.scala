package controllers

import javax.inject._

import model.database.ScheduleDateDA
import model.schedule.data.MSchedule
import model.schedule.meta.ScheduleDate
import org.joda.time.DateTime
import play.api.mvc._
import views.html.schedule._

/**
  * @author fabian 
  *         on 03.04.16.
  */
class ScheduleController @Inject()(mSchedule: MSchedule) extends AbstractController {

  def index() = sessionCache.getOrElse("schedule") {
   val result = Action {
      implicit request =>

        val courses = courseNames.flatMap(_._2)

        val lectures = mSchedule.getSchedule().filter(l => courses.exists(c => l.course.contains(c)))
        val timeRanges = mSchedule.getTimeRages(lectures)
        val weekDays = mSchedule.getWeekdays(lectures)

        val scheduleDate = ScheduleDateDA.findAll[ScheduleDate]().headOption match {
          case Some(sd) => sd
          case None => ScheduleDate(new DateTime())
        }
        Ok(scheduleMain(m("SCHEDULE.PAGETITLE"), courseNames, scheduleDate.date, getSemesterMode(), timeRanges, weekDays, lectures))
    }
    sessionCache.set("schedule", result)
    result
  }

  def blocks() = sessionCache.cached("blocks") {
    Action {
      implicit request =>
        val schedules = mSchedule.getBlocks()
        val blockList: List[(Id, CourseName, Blockname)] = schedules.map {
          s =>
            (s.id, s.doc.scheduleData.head.course.head, s.doc.title)
        }.sortBy(s => (s._2, s._3))
        Ok(blockMain(blockList))
    }
  }

  def block(id: Id) = sessionCache.cached(id) {
    Action {
      implicit request =>
        val block = mSchedule.getBlock(id)
        val timeRanges = mSchedule.getTimeRages(block.scheduleData)
        val weekDays = mSchedule.getWeekdays(block.scheduleData)
        val customWeekdays = mSchedule.getCustomWeedays(block.scheduleData)

        Ok(blockView(block.title, timeRanges, weekDays, block.scheduleData, customWeekdays))
    }
  }

  def personalschedule() = sessionCache.cached("personalSchedule") {
    Action {
      implicit request =>
        val courses = courseNames.flatMap(_._2)
        val lectures = mSchedule.getSchedule().filter(l => courses.exists(c => l.course.contains(c)))
        val timeRanges = mSchedule.getTimeRages(lectures)
        val weekDays = mSchedule.getWeekdays(lectures)

        val scheduleDate = ScheduleDateDA.findAll[ScheduleDate]().headOption match {
          case Some(sd) => sd
          case None => ScheduleDate(new DateTime())
        }
        Ok(personalSchedule(m("SCHEDULE.PERSONALSCHEDULE"), scheduleDate.date, getSemesterMode(), timeRanges, weekDays, lectures))
    }
  }

}
