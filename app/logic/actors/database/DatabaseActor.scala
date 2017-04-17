package logic.actors.database

import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorSystem}
import logic.actors.database.DatabaseActor._
import model.database.{LatestNumberDA, NewsEntryDA, ScheduleDateDA, SemesterModeDA}
import model.news.{LatestNumber, NewsEntry}
import model.schedule.data.{Group, Lecture, Schedule}
import model.schedule.meta.{EMode, ScheduleDate, SemesterMode}
import org.joda.time.DateTime
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * @author fabian 
  *         on 09.04.17.
  */

object DatabaseActor {

  /** upstart */
  case object StartUp

  case object IsDataBaseReady

  case object DatabaseIsReady

  case object DatabaseIsNotReady

  /** Lectures */
  case class StoreLectures(lectures: List[Lecture])

  case object GiveLectures

  case class Lectures(lectures: List[Lecture])

  /** Schedules (blocks) */
  case class StoreSchedules(schedule: List[Schedule])

  case object GiveSchedules

  case class Schedules(schedule: List[Schedule])

  /** Semestermode */
  case class StoreSemesterMode(mode: SemesterMode)

  case object GiveSemesterMode

  /** ScheduleDate */
  case class StoreScheduleDate(date: ScheduleDate)

  case object GiveScheduleDate

  /** Groups */
  case class StoreGroups(g: List[Group])

  case object GiveGroups

  case class Groups(g: List[Group])

  /** LatestNumber */
  case class StoreLatestNumber(num: LatestNumber)

  case object GiveLatestNumber

  /** NewsEntry */
  case object GiveNewsEntrys

  case class NewsEntrys(newsEntrys: List[NewsEntry])

  case class UpdateNewsEntry(entry: NewsEntry)

  case class InsertNewsEntry(entry: NewsEntry)

  case class DeleteEntry(entry: NewsEntry)

  /** sync entrys with real database */
  case object SyncDatabase

}

@Singleton
class DatabaseActor @Inject()(system: ActorSystem) extends Actor {

  private var groups = List[Group]()
  private var latestNumber = LatestNumber(0)
  private var lectures = List[Lecture]()
  private var newsEntry = List[NewsEntry]()
  private var schedule = List[Schedule]()
  private var scheduleDate = ScheduleDate(new DateTime())
  private var semesterMode = SemesterMode(EMode.SUMMER.name())
  private var databaseIsReady = false

  override def receive: Receive = {
    case StartUp =>
      Logger.debug("Database starting up")
      try {

        newsEntry = NewsEntryDA.findAll[NewsEntry]().get
        semesterMode = SemesterModeDA.findAll[SemesterMode]().get.headOption match {
          case Some(mode) => mode
          case None => SemesterMode(EMode.SUMMER.name())
        }
        scheduleDate = ScheduleDateDA.findAll[ScheduleDate]().get.headOption match {
          case Some(d) => d
          case None => ScheduleDate(new DateTime())
        }
        val (id, lLatestNumber) = LatestNumberDA.findAllExtended[LatestNumber]().get.headOption match {
          case None => ("", LatestNumber(0))
          case Some(n) => (n.id, n.doc)
        }
        latestNumber = lLatestNumber
        databaseIsReady = true
        Logger.debug("successfully starting up")
      } catch {
        case e: Exception =>
          Logger.debug("error on initialisation", e)
          system.scheduler.scheduleOnce(10 minutes, self, StartUp)
      }
    case StoreLectures(l) => lectures = l
    case StoreSchedules(s) => schedule = s
    case StoreSemesterMode(mode) => semesterMode = mode
    case StoreScheduleDate(date) => scheduleDate = date
    case StoreGroups(g) => groups = g
    case StoreLatestNumber(num) => latestNumber = num
    case GiveLatestNumber => sender ! latestNumber
    case GiveNewsEntrys => sender ! NewsEntrys(newsEntry)
    case IsDataBaseReady => if (databaseIsReady) {
      sender ! DatabaseIsReady
    } else {
      sender ! DatabaseIsNotReady
    }
    case UpdateNewsEntry(entry) =>
      newsEntry = newsEntry.filterNot(_.srcLink.equals(entry.srcLink)) :+ entry
    case InsertNewsEntry(entry) => newsEntry :+= entry
    case GiveLectures => sender ! Lectures(lectures)
    case GiveScheduleDate => sender ! scheduleDate
    case GiveSchedules => sender ! Schedules(schedule)
    case GiveGroups => sender ! Groups(groups)
    case GiveSemesterMode => sender ! semesterMode

    case SyncDatabase =>
      try {
        Logger.debug("try syncing the database")
        val existingNews = NewsEntryDA.findAllExtended[NewsEntry]().get

        NewsEntryDA.delete(existingNews.map(_.id))
        newsEntry.foreach(NewsEntryDA.insert)

        val existingNumber = LatestNumberDA.findAllExtended[LatestNumber]().get
        LatestNumberDA.delete(existingNumber.map(_.id))
        LatestNumberDA.insert(latestNumber)

        val existingScheduleDate = ScheduleDateDA.findAllExtended[ScheduleDate]().get
        ScheduleDateDA.delete(existingScheduleDate.map(_.id))
        ScheduleDateDA.insert(scheduleDate)

        val existingSemesterMode = SemesterModeDA.findAllExtended[SemesterMode]().get
        SemesterModeDA.delete(existingSemesterMode.map(_.id))
        SemesterModeDA.insert(semesterMode)

        Logger.debug("Database synced")
      } catch {
        case e: Exception =>
          Logger.debug("error on syncing, trying it later", e)
      }
    case DeleteEntry(entry) =>
      newsEntry = newsEntry.filterNot( _ == entry )
  }

}
