package model.news


import java.time.ZonedDateTime

import play.api.libs.json._

/**
  * @author fabian 
  * @since 28.03.16.
  */
object NewsEntry {
  implicit val newsEntryFormat = Json.format[NewsEntry]
}

case class NewsEntry(title: String, author: String, newsMessage: String, pubDate: ZonedDateTime, srcLink: String, expireDate: ZonedDateTime, tags: List[String], number:Long)
