package model.calendar

import org.fhs.spirit.scheduleparser.enumerations.EWeekdays
import org.joda.time.DateTimeConstants

/**
  * @author fabian 
  *         on 25.04.16.
  */
object WeekdayMapper {

  val weekdayMap = Map(EWeekdays.MONDAY -> DateTimeConstants.MONDAY,
    EWeekdays.TUESDAY -> DateTimeConstants.TUESDAY,
    EWeekdays.WEDNESDAY -> DateTimeConstants.WEDNESDAY,
    EWeekdays.THURSDAY -> DateTimeConstants.THURSDAY,
    EWeekdays.FRIDAY -> DateTimeConstants.FRIDAY,
    EWeekdays.SATURDAY -> DateTimeConstants.SATURDAY,
    EWeekdays.SUNDAY -> DateTimeConstants.SUNDAY)

}
