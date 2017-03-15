package logic.actors.rss

import java.util.regex.Pattern
import javax.inject._

import akka.actor._
import helpers.SpiritHelper
import model.database.LatestNumberDA
import model.database.NewsEntryDA._
import model.news.{LatestNumber, NewsEntry}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.{Configuration, Logger}

import scala.xml.{Elem, Node, XML}

/**
  * @author fabian 
  * @since 27.03.16.
  */
object RSSParseActor {

  case class RSSFeed(feedContent: String)

}

/** this will parse an rss feed an create a news entry */
class RSSParseActor @Inject()(configuration: Configuration,
                              @Named("tweetActor") tweetActor: ActorRef) extends Actor with SpiritHelper {

  import RSSParseActor._

  override def receive: Receive = {
    case RSSFeed(feedContent) =>

      val (id, latestNumber) = LatestNumberDA.findAllExtended[LatestNumber]().headOption match {
        case None => ("", LatestNumber(0))
        case Some(n) => (n.id, n.doc)
      }
      val feedXml = XML.loadString(feedContent)
      val newEntrys = extractEntrys(feedXml)

      val srcLinks = newEntrys.map(_.srcLink)

      val existingItems = findAllExtended[NewsEntry].filter(ei => srcLinks.contains(ei.doc.srcLink))

      val insertOrUpdableItems = newEntrys.par.filterNot {
        entry =>
          existingItems.exists(ei => ei.doc.newsMessage.equals(entry.newsMessage))
      }.seq

      var updateableEntrys = insertOrUpdableItems.filter {
        entry =>
          existingItems.exists(ei => ei.doc.srcLink.equals(entry.srcLink) && !ei.doc.newsMessage.equals(entry.newsMessage))
      }


      var insertableEntrys = insertOrUpdableItems.diff(updateableEntrys)
      Logger.debug(this.getClass.getSimpleName + " " + insertableEntrys.size)
      var number = latestNumber.number

      //insertableEntrys.foreach( ie=> insertJson( Json.stringify(Json.toJson(ie)) ))
      if (insertableEntrys.nonEmpty) {
        insertableEntrys = insertableEntrys.map { ie =>
          number += 1
          val numberedEntry = ie.copy(number = number)
          insert(numberedEntry)
          numberedEntry
        }
      }
      if (updateableEntrys.nonEmpty) {
        updateableEntrys = updateableEntrys.map {
          ue =>
            val id = existingItems.find {
              ei =>
                ei.doc.srcLink.equals(ue.srcLink)
            }.get.id
            number += 1
            val updateString = "[Update]"
            val newTitle = if (ue.title.contains(updateString)) {
              updateString + ue.title
            } else {
              ue.title
            }
            val numberedEntry = ue.copy(number = number, title = newTitle)
            update(id, numberedEntry)
            numberedEntry
        }
      }
      if (insertableEntrys.nonEmpty || updateableEntrys.nonEmpty) {
        val results = insertableEntrys ++ updateableEntrys
        results.foreach(ne => tweetActor ! ne)
        val theEntryNumbers = results.map(_.number)
        //Logger.debug(theEntryNumbers.toString())
        val maxNumber = theEntryNumbers.max

        if (id.nonEmpty) {
          //Logger.debug("update LatestNumberDA")
          LatestNumberDA.update(id, LatestNumber(maxNumber))
        } else {
          //Logger.debug("insert LatestNumberDA " + maxNumber)
          LatestNumberDA.insert(LatestNumber(maxNumber))
        }
        sessionCache.remove("news")
      }
  }

  def extractTags(title: String, content: String, courseNames: List[String]) = {
    def extractTagsInternal(s: String) = {
      courseNames.map {
        cn =>
          val p = Pattern.compile("(?i:.*" + cn + "[,:]?\\s+.*)")
          //val p = Pattern.compile("\\A|\\W" + cn + "[,]?\\W+")
          val matcher = p.matcher(s)
          if (matcher.matches()) {
            Some(cn)
          } else {
            None
          }
      }.filter(_.nonEmpty).map(_.get)
    }

    def removeWhitespaces(s: String): String = {
      s.toLowerCase().replaceAll("(^|\\s)(ba)\\s", "ba").replaceAll("(^|\\s)(ma)\\s", "ma")
    }

    val editetTitle = removeWhitespaces(title)

    val tags = extractTagsInternal(title) ++ extractTagsInternal(content) ++ extractTagsInternal(editetTitle)
    if (tags.isEmpty) {
      List("Alle", "alte_semester")
    } else {
      tags.distinct
    }
  }

  def extractEntrys(feedXml: Elem) = {
    def now = new DateTime()

    val courseNames = uncachedCourseNames
    (feedXml \\ "item").map {
      item =>
        val title = (item \ "title") text
        val srcLink = (item \ "link") text
        val newsMessage = extractMessage(item)
        val author = (item \\ "contributor") text

        val date: DateTime = extractPubDate(item)
        val expireDate = now.plusMonths(1)
        NewsEntry(title, author, newsMessage, date, srcLink, expireDate, extractTags(title, newsMessage, courseNames), 0)
    }.toList.sortBy(_.pubDate.getMillis)
  }

  def extractPubDate(item: Node): DateTime = {
    val timeZone = DateTimeZone.forID("Europe/Berlin")
    val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mmZ")

    val date = dateTimeFormat.withZone(timeZone).parseDateTime((item \\ "date" text))
    date
  }

  private def extractMessage(item: Node): String = {
    ((item \ "description") text)
      .replaceAll("\\p{javaSpaceChar}+", " ").trim
  }

}
