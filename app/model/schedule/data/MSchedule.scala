package model.schedule.data

import javax.inject._

import model.database.{LectureDA, ScheduleDA}
import org.fhs.spirit.scheduleparser.enumerations.EWeekdays

/**
  * @author fabian 
  *         on 04.04.16.
  */
@Singleton
class MSchedule {

  def getTimeRages(lectures: List[Lecture]) = {
    lectures.toSet.map {
      l: Lecture =>
        TimeRange.time2TimeRange(l.time)
    }.toList.sorted
  }

  def getWeekdays(lectures: List[Lecture]): List[EWeekdays] = {
    lectures.map {
      l =>
        EWeekdays.findConstantByName(l.time.weekday)
    }.filter(_.isPresent).map(_.get()).distinct.sorted
  }

  def getCustomWeedays(lectures: List[Lecture]) = {
    lectures.filter(l=> !EWeekdays.findConstantByName(l.time.weekday).isPresent).map(_.time.weekday).distinct
  }

  def getSchedule() = {
    LectureDA.findAll[Lecture]()
  }

  def getBlocks() = {
    ScheduleDA.findAllExtended[Schedule]()
  }

  def getBlock(id: String) = {
    ScheduleDA.findById[Schedule](id)
  }

}
