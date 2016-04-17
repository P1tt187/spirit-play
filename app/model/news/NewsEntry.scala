package model.news

import org.joda.time.DateTime
import play.api.libs.json._

/**
  * @author fabian 
  * @since 28.03.16.
  */
object NewsEntry {
  implicit val newsEntryFormat = Json.format[NewsEntry]
}

case class NewsEntry(title: String, author: String, newsMessage: String, pubDate: DateTime, srcLink: String, expireDate: DateTime, tags: List[String], number:Long)
