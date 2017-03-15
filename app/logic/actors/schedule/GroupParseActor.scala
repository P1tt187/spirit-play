package logic.actors.schedule

/**
  * @author fabian 
  *         on 12.04.16.
  */

import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import javax.inject._

import akka.actor.Actor
import helpers.SpiritHelper
import logic.actors.schedule.GroupParseActor.ParseGroups
import model.database.GroupDA
import model.schedule.data.{Group, Student}
import org.jsoup.Jsoup
import play.api.Logger
import play.api.libs.ws.WSClient

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.xml.{NodeSeq, XML}

object GroupParseActor {

  case object ParseGroups

}

/** this actor parse all groups */
@Singleton
class GroupParseActor @Inject()(ws: WSClient) extends Actor with SpiritHelper {

  override def receive: Receive = {
    case ParseGroups =>
      Logger.debug("begin parse groups")

      GroupDA.findAllExtended[Group]().foreach(g => GroupDA.delete(g.id))
      val baseUrl = configuration.underlying.getString("schedule.baseUrl")
      val outcome = configuration.underlying.getString("schedule.groups")
      val httpResult = Await.result(ws.url(baseUrl + outcome).get(), 10 seconds)
      if (httpResult.status != 404) {
        val responseString = httpResult.bodyAsBytes.decodeString(StandardCharsets.ISO_8859_1.toString)
        val responseDoc = Jsoup.parse(responseString)
        responseDoc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
        val parsedHtml = XML.loadString(responseDoc.toString)
        //Logger.debug(parsedHtml.toString())
        val body = parsedHtml \ "body"
        /** first: create a list of tuples with course and grouptype */
        val groupTypes = createCourseGrouptypes(((body \\ "p") \ "font") \ "a")
        /** second: collect all data from grouptyes */
        val studentStructure = (body \\ "table").map {
          t =>
            (t \\ "td").map {
              td =>
                td.toString.replaceAll("<td valign=\"middle\" rowspan=\"1\" colspan=\"1\" align=\"left\">", "").replaceAll("<td align=\"left\" valign=\"middle\"> ", "").replaceAll("</td>", "").replaceAll("<br clear=\"none\"/>", ";").replaceAll("<br/>", ";").split(";").map {
                  studentString =>
                    val split = studentString.split(",")
                    (split.lift(1).getOrElse("").trim, split(0).trim)
                }
            }
        }

        /** third: combine all data and save it */
        for (groupIndex <- groupTypes.indices) {
          val (theClass, groupType) = groupTypes(groupIndex)

          var i = 0
          studentStructure(groupIndex).foreach {
            groupList =>
              i += 1

              val studentList = groupList.map {
                case (firstname, lastname) =>
                  Student(firstname, lastname)
              }.toList.filterNot(s => s.firstName.isEmpty && s.lastname.isEmpty)

              val group = Group(theClass, groupType, i, studentList)

              GroupDA.insert(group)
          }
        }
        Logger.debug("finished parse groups")
        sessionCache.clear()
      }
  }

  private def createCourseGrouptypes(elements: NodeSeq) = {

    val pattern = Pattern.compile("^[bm]a*", Pattern.CASE_INSENSITIVE)

    val buf = scala.collection.mutable.ListBuffer.empty[(String, String)]

    val allclassesLowercase = courseNames.flatMap(_._2).map(_.toLowerCase)

    @tailrec
    def doPrivate(elements: NodeSeq): List[(String, String)] = {
      //logger debug elements
      if (elements.isEmpty) {
        buf.toList
      } else {
        val element = elements.head

        if (pattern.matcher(element.text).find()) {

          val textLower = element.text.toLowerCase
          val text = element.text
          val theClassList = allclassesLowercase.filter(className => textLower.contains(className))

          if (theClassList.nonEmpty) {
            val theClass = theClassList.head
            val groupType = text.substring(text.indexOf('(') + 1, text.indexOf(')'))

            val append = (theClass, groupType)


            buf += append
          }
        }
        doPrivate(elements.tail)

      }
    }

    doPrivate(elements)
  }

}
