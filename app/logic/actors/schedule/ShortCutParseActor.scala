package logic.actors.schedule

import java.nio.charset.StandardCharsets
import java.util.Scanner
import javax.inject._

import akka.actor.Actor
import helpers.SpiritHelper
import logic.actors.schedule.ShortCutParseActor.ParseShortCuts
import model.database.{LectureDA, ScheduleDA}
import model.schedule.data.{Lecture, Schedule}
import play.api.libs.ws.WSClient

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author fabian 
  *         on 10.04.16.
  */
object ShortCutParseActor {

  case object ParseShortCuts

}

/** this actor will parse all shortcuts */
@Singleton
class ShortCutParseActor @Inject()(ws: WSClient) extends Actor with SpiritHelper {

  private def replaceHTMLExtraSymbols(str: String) = {
    str.replaceAll("&nbsp;", " ")
      .replaceAll("&Auml;", "Ä").replaceAll("&Ouml;", "Ö").replaceAll("&Uuml;", "Ü")
      .replaceAll("&auml;", "ä").replaceAll("&ouml;", "ö").replaceAll("&uuml;", "ü")
      .replaceAll("&amp;", "&").replaceAll("&szlig;", "ß")
  }

  override def receive: Receive = {
    case ParseShortCuts =>


      def removeWhitespace(str: String) = {
        str.replaceAll("\\p{javaSpaceChar}", "")
      }

      val baseurl = configuration.getString("schedule.baseUrl").getOrElse("localhost")
      val outcome = configuration.getString("schedule.shortCuts").getOrElse("abkuerzungsverzeichnis.html")
      val allLectures = LectureDA.findAllExtended[Lecture]()
      val allBlocks = ScheduleDA.findAllExtended[Schedule]()

      val result = Await.result(ws.url(baseurl + outcome).get(), 10 seconds)
      if (result.status != 404) {
        val html = result.bodyAsBytes.decodeString(StandardCharsets.ISO_8859_1.toString)

        var lectureTitles = Map[String, String]()
        val secondTableString = "<td width=50%>"

        val scanner = new Scanner(html)

        while (scanner.hasNextLine) {
          val line = replaceHTMLExtraSymbols(scanner.nextLine())

          if (line.startsWith("<tr>")) {
            val key = removeWhitespace(line.substring(line.indexOf("<b>") + "<b>".length, line.indexOf("</b>") - 2).trim)
            val value = line.substring(line.indexOf(secondTableString) + secondTableString.length, line.lastIndexOf("</td>"))

            lectureTitles += key -> value
          }
        }
        scanner.close()

        allLectures.foreach {
          lec =>
            val id = lec.id
            val titlelong = lectureTitles.get(removeWhitespace(lec.doc.lectureName.trim)) match {
              case Some(v) => v
              case None =>
                lec.doc.lectureName
            }
            val updatetVal = lec.doc.copy(longTitle = titlelong)
            LectureDA.update(id, updatetVal)
        }

        allBlocks.foreach {
          block =>
            val id = block.id
            val updatetBlocks = block.doc.scheduleData.map {
              sd =>
                val titleLong = lectureTitles.get(removeWhitespace(sd.lectureName)) match {
                  case Some(v) => v
                  case None =>
                    sd.lectureName
                }
                sd.copy(longTitle = titleLong)
            }
            ScheduleDA.update(id, block.doc.copy(scheduleData = updatetBlocks))
        }
      }
      sessionCache.clear()
      semesterModeCache.clear()
  }
}
