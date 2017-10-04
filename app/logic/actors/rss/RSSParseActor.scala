package logic.actors.rss

import java.time.ZonedDateTime
import java.util.regex.Pattern
import javax.inject._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import helpers.SpiritHelper
import logic.actors.database.DatabaseActor._
import model.news.{LatestNumber, NewsEntry}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.{Configuration, Logger}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
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
                              @Named("tweetActor") tweetActor: ActorRef, @Named("databaseActor") databaseActor: ActorRef) extends Actor with SpiritHelper {

  import RSSParseActor._

  override def receive: Receive = {
    case RSSFeed(feedContent) =>

      implicit val timeout = Timeout(30 seconds)
      val future = databaseActor ? IsDataBaseReady
      val result = Await.result(future, timeout.duration)

      val allowedToWork = result match {
        case DatabaseIsNotReady => false
        case DatabaseIsReady => true
        case _ => false
      }
      if (allowedToWork) {

        val latestNumber = Await.result(databaseActor ? GiveLatestNumber, timeout.duration).asInstanceOf[LatestNumber]

        val feedXml = XML.loadString(feedContent)
        val newEntrys = extractEntrys(feedXml)

        val srcLinks = newEntrys.map(_.srcLink)

        val allExistingItems = Await.result(databaseActor ? GiveNewsEntrys, timeout.duration).asInstanceOf[NewsEntrys].newsEntrys

        val existingItems = allExistingItems.filter(ei => srcLinks.contains(ei.srcLink))

        val insertOrUpdableItems = newEntrys.par.filterNot {
          entry =>
            existingItems.exists(ei => ei.newsMessage.equals(entry.newsMessage))
        }.seq

        var updateableEntrys = insertOrUpdableItems.filter {
          entry =>
            existingItems.exists(ei => ei.srcLink.equals(entry.srcLink) && !ei.newsMessage.equals(entry.newsMessage))
        }


        var insertableEntrys = insertOrUpdableItems.diff(updateableEntrys)
        Logger.debug(this.getClass.getSimpleName + " " + insertableEntrys.size)
        var number = latestNumber.number

        //insertableEntrys.foreach( ie=> insertJson( Json.stringify(Json.toJson(ie)) ))
        if (insertableEntrys.nonEmpty) {
          insertableEntrys = insertableEntrys.map { ie =>
            number += 1
            val numberedEntry = ie.copy(number = number)
            databaseActor ! InsertNewsEntry(numberedEntry)
            numberedEntry
          }
        }
        if (updateableEntrys.nonEmpty) {
          updateableEntrys = updateableEntrys.map {
            ue =>

              number += 1
              val updateString = "[Update]"
              val newTitle = if (ue.title.contains(updateString)) {
                updateString + ue.title
              } else {
                ue.title
              }
              val numberedEntry = ue.copy(number = number, title = newTitle)
              databaseActor ! UpdateNewsEntry(numberedEntry)
              numberedEntry
          }
        }
        if (insertableEntrys.nonEmpty || updateableEntrys.nonEmpty) {
          val results = insertableEntrys ++ updateableEntrys
          results.foreach(ne => tweetActor ! ne)
          val theEntryNumbers = results.map(_.number)
          //Logger.debug(theEntryNumbers.toString())
          val maxNumber = theEntryNumbers.max

          databaseActor ! StoreLatestNumber(LatestNumber(maxNumber))

          sessionCache.remove("news")
        }
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
    def now = ZonedDateTime.now()

    val courseNames = uncachedCourseNames
    (feedXml \\ "item").map {
      item =>
        val title = (item \ "title") text
        val srcLink = (item \ "link") text
        val newsMessage = extractMessage(item)
        val author = (item \\ "contributor") text

        val date: ZonedDateTime = extractPubDate(item)
        val expireDate = now.plusMonths(1)
        NewsEntry(title, author, newsMessage, date, srcLink, expireDate, extractTags(title, newsMessage, courseNames), 0)
    }.toList.sortBy(_.pubDate.toInstant.toEpochMilli)
  }

  def extractPubDate(item: Node): ZonedDateTime = {
    val timeZone = DateTimeZone.forID("Europe/Berlin")
    val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mmZ")

    val date = dateTimeFormat.withZone(timeZone).parseDateTime((item \\ "date" text)).toGregorianCalendar.toZonedDateTime
    date
  }

  private def extractMessage(item: Node): String = {
    ((item \ "description") text)
      .replaceAll("\\p{javaSpaceChar}+", " ").trim
  }

}
