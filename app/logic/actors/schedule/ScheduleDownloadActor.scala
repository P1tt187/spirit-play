package logic.actors.schedule

import java.nio.charset.StandardCharsets
import javax.inject._

import akka.actor.{Actor, ActorRef}
import helpers.SpiritHelper
import logic.actors.schedule.ScheduleDownloadActor.DownloadSchedule
import org.fhs.spirit.scheduleparser.enumerations.EScheduleKind
import org.jsoup.Jsoup
import play.api.libs.ws.WSClient

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._
import logic.actors.schedule.ScheduleParseActor._
import model.database.{LectureDA, ScheduleDA}
import model.schedule.data.{Lecture, Schedule}
/**
  * @author fabian 
  *         on 03.04.16.
  */

object ScheduleDownloadActor {

  case object DownloadSchedule

}

@Singleton
class ScheduleDownloadActor @Inject()(ws: WSClient, @Named("parseActor") parseActor: ActorRef) extends Actor with SpiritHelper {


  override def receive: Receive = {
    case DownloadSchedule =>
      LectureDA.findAllExtended[Lecture]()
        .foreach(lec => LectureDA.delete(lec.id) )
      ScheduleDA.findAllExtended[Schedule]()
        .foreach(sch => ScheduleDA.delete(sch.id))

      val baseUrl = configuration.underlying.getString("schedule.baseUrl")

      val lectureResults = uncachedCourseNames.  map {
        courseName =>
          val outcome =  "s_" + courseName + ".html"
          val httpResult = Await.result(ws.url(baseUrl + outcome).get(), 10 seconds)
          if (httpResult.status != 404) {
            Some((httpResult.bodyAsBytes.decodeString(StandardCharsets.ISO_8859_1.toString), courseName))
          } else {
            None
          }
      }.filter(_.nonEmpty).map(rs => (Jsoup.parse(rs.get._1).toString, rs.get._2)).map(rs => (EScheduleKind.REGULAR, rs))

      val blockBaseResult = Await.result(ws.url(baseUrl + "bindex.html").get(), 10 seconds)
      val bindex = Jsoup.parse(blockBaseResult.bodyAsBytes.decodeString(StandardCharsets.ISO_8859_1.toString))
      val blockRefs = bindex.select("a").map(_.attr("href")).toSet

      val blockResult = blockRefs.map {
        block =>
          val httpResult = Await.result(ws.url(baseUrl + block).get(), 10 seconds)
          if (httpResult.status != 404) {
            Some((httpResult.bodyAsBytes.decodeString(StandardCharsets.ISO_8859_1.toString), block))
          } else {
            None
          }
      }.filter(_.nonEmpty).map(rs => (Jsoup.parse(rs.get._1).toString, rs.get._2)).map(rs => (EScheduleKind.BLOCK, rs))

      parseActor ! ParseSchedule( lectureResults ++ blockResult )
  }
}
