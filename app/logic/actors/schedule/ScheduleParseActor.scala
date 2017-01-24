package logic.actors.schedule

import javax.inject._

import akka.actor.{Actor, ActorRef}
import logic.actors.schedule.ScheduleParseActor.ParseSchedule
import logic.actors.schedule.ShortCutParseActor.ParseShortCuts
import model.database.{LectureDA, ScheduleDA}
import model.schedule.data.Lecture._
import model.schedule.data.Schedule
import org.fhs.spirit.scheduleparser.ScheduleToJSONConverter
import org.fhs.spirit.scheduleparser.enumerations.EScheduleKind
import play.api.Logger
import play.api.libs.json._


/**
  * @author fabian 
  *         on 03.04.16.
  */

object ScheduleParseActor {

  case class ParseSchedule(scheduleList: List[(EScheduleKind, (String, String))])

}

@Singleton
class ScheduleParseActor @Inject()(@Named("shortcutParser") shortcutParser: ActorRef) extends Actor {
  override def receive: Receive = {
    case ParseSchedule(scheduleList) =>
      val parseResults = scheduleList.map {
        element =>
          val (kind, contentParts) = element
          val (content, course) = contentParts
            ScheduleToJSONConverter(content, kind, course = course)
      }.filter(_.nonEmpty).map(_.get).map(result => Json.parse(result).as[Schedule])

      val blocks = parseResults.filter(_.title.nonEmpty)
      val lecturesList = parseResults.filter(_.title.isEmpty).flatMap( _.scheduleData )
      Logger.debug("blocks: " + blocks.size)

      ScheduleDA.insert(blocks)

      /** find lectures for multiple courses */
     val lectures = lecturesList.map(_.uuid).distinct.map{
        uuid=>
          val lects = lecturesList.filter(_.uuid.equals(uuid))
          val course = lects.flatMap(_.course)
          val alternatives = lects.flatMap(_.alternatives).distinct
          lects.head.copy(course = course, alternatives = alternatives)
      }
      Logger.debug("lectures: " + lectures.size)
      LectureDA.insert(lectures)
      Logger.debug("finished parsing")


      shortcutParser ! ParseShortCuts
  }
}
