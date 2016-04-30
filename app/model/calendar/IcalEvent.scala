package model.calendar

import model.schedule.data.Lecture
import net.fortuna.ical4j.model
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.{Description, Location, RRule, Uid}
import org.fhs.spirit.scheduleparser.enumerations.{EDuration, EWeekdays}
import org.joda.time.DateTime


/**
  * @author fabian 
  *         on 25.04.16.
  */
case class IcalEvent(startTime: DateTime, endTime: DateTime, lecture: Lecture) {

  def mkEvent = {
    val weekDay = WeekdayMapper.weekdayMap(EWeekdays.findConstantByName(lecture.time.weekday).get())
    val time = lecture.time
    val duration = EDuration.valueOf(lecture.duration)
    val startPoint = if (EDuration.WEEKLY == duration) {
      startTime
    } else if (EDuration.EVEN == duration && startTime.getWeekOfWeekyear % 2 != 0) {
      startTime.weekOfWeekyear().addToCopy(1)
    } else if (EDuration.UNEVEN == duration && startTime.getWeekOfWeekyear % 2 == 0) {
      startTime.weekOfWeekyear().addToCopy(1)
    } else {
      startTime
    }

    val eventStart = startPoint.dayOfWeek().setCopy(weekDay).hourOfDay().setCopy(time.startHour).minuteOfHour().setCopy(time.startMinute)
    val eventStop = startPoint.dayOfWeek().setCopy(weekDay).hourOfDay().setCopy(time.stopHour).minuteOfHour().setCopy(time.stopMinute)
    val event = new VEvent(new model.DateTime(eventStart.toDate), new model.DateTime(eventStop.toDate), lecture.lectureName)
    event.getProperties.add(new Location(lecture.room))
    val freqSuffix = if (duration != EDuration.WEEKLY) {
      ";INTERVAL=2"
    } else {
      ""
    }

    event.getProperties.add(new RRule("FREQ=WEEKLY;UNTIL=" + new Date(endTime.getMillis) + freqSuffix))
    event.getProperties.add(new Uid(lecture.uuid))
    event.getProperties.add(new Description(mkDescription(lecture)))
    event
  }

  private def mkDescription(lecture: Lecture) = {
    val groupSuffix = if (lecture.groupIndex.nonEmpty) {
      "Gruppe: " + lecture.groupIndex
    } else {
      ""

    }
    val sb = new StringBuilder
    sb append lecture.longTitle append " \n"
    sb append lecture.course.mkString(" ") append "\n"
    sb append groupSuffix append "\n"
    sb append lecture.docents.mkString(" ") append "\n\n"
    if (lecture.alternatives.nonEmpty) {
      sb append "Alternativen: " append "\n"
      lecture.alternatives.foreach {
        alt =>
          sb append "Stunde: " append alt.hour append "\n"
          sb append "Wochentag:" append EWeekdays.valueOf(alt.weekday).getGermanName append "\n"
          sb append "Raum: " append alt.room append "\n\n"
      }
    }
    sb.toString()
  }

}
