package logic.actors.schedule

import javax.inject._

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import logic.actors.database.DatabaseActor.{DatabaseIsNotReady, DatabaseIsReady, _}
import logic.actors.schedule.ScheduleDownloadActor._
import model.schedule.meta.{EMode, ScheduleDate, SemesterMode}
import model.spread.Tweet
import model.spread.Tweet._
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import play.api.Configuration

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * @author fabian 
  *         on 03.04.16.
  */

object CheckScheduleDateActor {

  case object CheckScheduleDate

}

/** this actor check the last modifiacation of public schedule */
@Singleton
class CheckScheduleDateActor @Inject()(configuration: Configuration, @Named("scheduleDownloader") scheduleDownloadActor: ActorRef, @Named("tweetActor") tweetActor: ActorRef, @Named("databaseActor") databaseActor: ActorRef, system: ActorSystem) extends Actor {

  import CheckScheduleDateActor._

  val baseUrl = configuration.underlying.getString("schedule.baseUrl")
  val headPart = configuration.underlying.getString("schedule.head")

  override def receive = {
    case CheckScheduleDate =>

      implicit val timeout = Timeout(30 seconds)
      val future = databaseActor ? IsDataBaseReady
      val result = Await.result(future, timeout.duration)

      val allowedToWork = result match {
        case DatabaseIsNotReady => false
        case DatabaseIsReady => true
        case _ => false
      }
      if (!allowedToWork) {
        system.scheduler.scheduleOnce(12 minutes, self, CheckScheduleDate)
      } else {

        val headContent = Jsoup.connect(baseUrl + headPart).get
        val bolds = headContent.select("B")

        bolds.foreach {
          b =>
            if (b.text().matches("\\d{1,2}[.]\\d{1,2}[.]\\d{4}")) {
              val scheduleDate = DateTimeFormat.forPattern("dd.MM.yyyy").withZone(DateTimeZone.forID("Europe/Berlin")).parseDateTime(b.text())

              databaseActor ! StoreScheduleDate(ScheduleDate(scheduleDate))
            }
        }


        scheduleDownloadActor ! DownloadSchedule


        val mode = if (headContent.text().contains("Sommer")) {
          EMode.SUMMER
        } else {
          EMode.WINTER
        }

        val currentMode = Await.result(databaseActor ? GiveSemesterMode, 30 seconds).asInstanceOf[SemesterMode]


        if (mode != EMode.valueOf(currentMode.mode)) {
          tweetActor ! Tweet("Neuer Stundenplan online", hostUrl, "")
        }

        databaseActor ! StoreSemesterMode(SemesterMode(mode.name()))
      }
  }

}
