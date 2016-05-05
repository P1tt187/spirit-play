package logic.actors.schedule

import javax.inject._

import akka.actor.{Actor, ActorRef}
import logic.actors.schedule.GroupParseActor.ParseGroups
import logic.actors.schedule.ScheduleDownloadActor._
import model.schedule.meta.{EMode, ScheduleDate, SemesterMode}
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import play.api.Configuration

import scala.collection.JavaConversions._

/**
  * @author fabian 
  *         on 03.04.16.
  */

object CheckScheduleDateActor {

  case object CheckScheduleDate

}
/** this actor check the last modifiacation of public schedule */
@Singleton
class CheckScheduleDateActor @Inject()(configuration: Configuration, @Named("scheduleDownloader") scheduleDownloadActor: ActorRef, @Named("groupParseActor") groupParseActor: ActorRef) extends Actor {

  import CheckScheduleDateActor._

  val baseUrl = configuration.underlying.getString("schedule.baseUrl")
  val headPart = configuration.underlying.getString("schedule.head")

  override def receive = {
    case CheckScheduleDate =>

      val headContent = Jsoup.connect(baseUrl + headPart).get
      val bolds = headContent.select("B")

      bolds.foreach {
        b =>
          if (b.text().matches("\\d{1,2}[.]\\d{1,2}[.]\\d{4}")) {
            import model.database.ScheduleDateDA._

            val scheduleDate = DateTimeFormat.forPattern("dd.MM.yyyy").withZone(DateTimeZone.forID("Europe/Berlin")).parseDateTime(b.text())
            findAllExtended[ScheduleDate]().headOption match {
              case None => insert(ScheduleDate(scheduleDate))
              case Some(sd) =>
                val id = sd.id
                update(id, ScheduleDate(scheduleDate))
            }
          }
      }

      scheduleDownloadActor ! DownloadSchedule
      groupParseActor ! ParseGroups

      import model.database.SemesterModeDA._
      val mode = if (headContent.text().contains("Sommer")) {
        EMode.SUMMER
      } else {
        EMode.WINTER
      }
      findAllExtended[SemesterMode]().headOption match {
        case Some(sm) =>
          val id = sm.id
          update(id, SemesterMode(mode.name()))
        case None =>
          insert(SemesterMode(mode.name()))
      }

  }
}
