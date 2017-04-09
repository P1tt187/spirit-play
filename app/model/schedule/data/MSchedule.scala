package model.schedule.data

import javax.inject._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import logic.actors.database.DatabaseActor._
import model.schedule.meta.ScheduleDate
import org.fhs.spirit.scheduleparser.enumerations.EWeekdays

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author fabian 
  *         on 04.04.16.
  */
@Singleton
class MSchedule @Inject()(@Named("databaseActor") databaseActor: ActorRef) {

  implicit val timeout = Timeout(30 seconds)

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
    lectures.filter(l => !EWeekdays.findConstantByName(l.time.weekday).isPresent).map(_.time.weekday).distinct
  }

  def getSchedule() = {
    Await.result(databaseActor ? GiveLectures, 1 minute).asInstanceOf[Lectures].lectures
  }

  def getScheduleDate(): ScheduleDate = {
    Await.result(databaseActor ? GiveScheduleDate, 1 minute).asInstanceOf[ScheduleDate]
  }

  def getBlocks() = {
    Await.result(databaseActor ? GiveSchedules, 1 minute).asInstanceOf[Schedules].schedule
  }

  def getBlock(id: String) = {
    getBlocks().find(_.uid.equals(id))
  }

}
