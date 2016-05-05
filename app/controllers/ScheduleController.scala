package controllers

import javax.inject._

import helpers.ICalBuilder
import model.database.{LectureDA, ScheduleDateDA}
import model.schedule.data.MSchedule
import model.schedule.meta.ScheduleDate
import org.joda.time.{DateTime, DateTimeConstants}
import play.api.i18n.Messages
import play.api.mvc._
import views.html.schedule._

/**
  * @author fabian 
  *         on 03.04.16.
  */
@Singleton
class ScheduleController @Inject()(mSchedule: MSchedule) extends AbstractController {

  /** this will create a page containing all lectures of this semester */
  def index() = sessionCache.getOrElse("schedule") {
    val result = Action {
      implicit request =>

        /** courses are needed for filtering the displayd lectures */
        val courses = courseNames.flatMap(_._2)

        val lectures = mSchedule.getSchedule().filter(l => courses.exists(c => l.course.contains(c)))
        /** timeranges are dynamic createt from lecture data and used in the table */
        val timeRanges = mSchedule.getTimeRages(lectures)
        /** we show all weekdays which are contained in the schedule */
        val weekDays = mSchedule.getWeekdays(lectures)

        /** this value displays the date when the schedule was updatet last time */
        val scheduleDate = ScheduleDateDA.findAll[ScheduleDate]().headOption match {
          case Some(sd) => sd
          case None => ScheduleDate(new DateTime())
        }
        Ok(scheduleMain(Messages("SCHEDULE.PAGETITLE"), courseNames, scheduleDate.date, getSemesterMode(), timeRanges, weekDays, lectures))
    }
    sessionCache.set("schedule", result)
    result
  }

  /** main part of block lectures */
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

  /** display the block with dis id */
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

  /** personal schedule is most identical to the normal schedule but loads all the data from localstorage */
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
        Ok(personalSchedule(Messages("SCHEDULE.PERSONALSCHEDULE"), scheduleDate.date, getSemesterMode(), timeRanges, weekDays, lectures))
    }
  }

  /** this will create an ical file containing all selected schedule data */
  def icalExport = Action {
    implicit request =>
      val input = request.body.asFormUrlEncoded.get
      val icalInput = input("icalInput").head.split(";").toSet

      val now = DateTime.now()
      val nowMonth = now.getMonthOfYear

      val startTime = if (nowMonth < DateTimeConstants.APRIL) {
        now.minusYears(1).monthOfYear().setCopy(DateTimeConstants.OCTOBER).dayOfMonth().withMinimumValue()
      } else if (nowMonth >= DateTimeConstants.OCTOBER && nowMonth <= DateTimeConstants.DECEMBER) {
        now.monthOfYear().setCopy(DateTimeConstants.OCTOBER).dayOfMonth().withMinimumValue()
      } else {
        now.monthOfYear().setCopy(DateTimeConstants.APRIL).dayOfMonth().withMinimumValue()
      }

      val endTime = if (startTime.getMonthOfYear == DateTimeConstants.OCTOBER) {
        startTime.year().addToCopy(1).monthOfYear().setCopy(DateTimeConstants.MARCH).dayOfMonth().withMinimumValue()
      } else {
        startTime.monthOfYear().setCopy(DateTimeConstants.SEPTEMBER).dayOfMonth().withMinimumValue()
      }

      val lectures = LectureDA.findUids(icalInput.toList)
      val result = ICalBuilder(startTime, endTime, lectures)

      Ok(result).as("text/calendar;Content-Disposition: attachment; filename=\"plan.ics\"")
  }

}
