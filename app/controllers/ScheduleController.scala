package controllers

import javax.inject._

import helpers.ICalBuilder
import model.database.{LectureDA, ScheduleDateDA}
import model.schedule.data.MSchedule
import model.schedule.meta.ScheduleDate
import org.joda.time.{DateTime, DateTimeConstants, Months}
import play.api.i18n.MessagesApi
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

  def icalExport = Action{
    implicit request =>
      val input = request.body.asFormUrlEncoded.get
      val icalInput= input("icalInput").head.split(";").toSet

      val now = DateTime.now()
      val nowMonth = now.getMonthOfYear

      val startTime = if(nowMonth < DateTimeConstants.APRIL){
        now.minusYears(1).monthOfYear().setCopy(DateTimeConstants.OCTOBER).dayOfMonth().withMinimumValue()
      } else if(nowMonth >= DateTimeConstants.OCTOBER && nowMonth<= DateTimeConstants.DECEMBER) {
        now.monthOfYear().setCopy(DateTimeConstants.OCTOBER).dayOfMonth().withMinimumValue()
      } else {
        now.monthOfYear().setCopy(DateTimeConstants.APRIL).dayOfMonth().withMinimumValue()
      }

      val endTime = if(startTime.getMonthOfYear == DateTimeConstants.OCTOBER){
        startTime.year().addToCopy(1).monthOfYear().setCopy(DateTimeConstants.MARCH).dayOfMonth().withMinimumValue()
      } else {
        startTime.monthOfYear().setCopy(DateTimeConstants.SEPTEMBER).dayOfMonth().withMinimumValue()
      }

       val lectures = LectureDA.findUids(icalInput.toList)
      val result = ICalBuilder(startTime,endTime,lectures)

      Ok(result).as("text/calendar;Content-Disposition: attachment; filename=\"plan.ics\"")
  }

}
