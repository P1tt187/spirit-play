package model.calendar

import model.schedule.data.Lecture
import net.fortuna.ical4j.model
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.{Description, Location, RRule, Uid}
import org.fhs.spirit.scheduleparser.enumerations.{EDuration, EWeekdays}
import org.joda.time.DateTime


/**
  * @author fabian 
  *         on 25.04.16.
  */
case class IcalEvent(startTime: DateTime, lecture: Lecture) {

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
    val groupSuffix = if(lecture.groupIndex.nonEmpty) {
      "Gruppe: " + lecture.groupIndex
    } else  {
      ""
    }

    event.getProperties.add(new RRule("FREQ=WEEKLY" + freqSuffix))
    event.getProperties.add(new Uid(lecture.uuid))
    event.getProperties.add(new Description(lecture.longTitle + " \n" + lecture.docents.mkString(" ") + "\n" + groupSuffix))
    event
  }

}
