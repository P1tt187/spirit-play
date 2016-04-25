package helpers

import model.calendar.IcalEvent
import model.schedule.data.Lecture
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.property.{CalScale, Version}
import org.joda.time.DateTime

/**
  * @author fabian 
  *         on 25.04.16.
  */
object ICalBuilder {
  def apply(startTime:DateTime, lectures:List[Lecture]) = {

    val calendar = new Calendar()
    val props = calendar.getProperties
    props.add(Version.VERSION_2_0)
    props.add(CalScale.GREGORIAN)
    /** Magic */

    lectures.foreach {
      l=>
        calendar.getComponents().add(IcalEvent(startTime,l).mkEvent)
    }

    /** End of Magic */
    calendar.toString
  }


}
