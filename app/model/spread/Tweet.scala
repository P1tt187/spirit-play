package model.spread

import com.typesafe.config.ConfigFactory
import model.news.NewsEntry
import controllers._
import play.api.{Configuration, Logger}

/**
  * @author fabian 
  *         on 16.04.16.
  */

object Tweet {
  val config = Configuration(ConfigFactory.load())
  lazy val hostUrl =config.getString("spirit.host").getOrElse("http://localhost:9000")
  implicit def newsToTweet(newsEntry: NewsEntry):Tweet = {
    Tweet(newsEntry.title, hostUrl + routes.NewsPageController.newsEntry(newsEntry.number).url, newsEntry.tags.map("#" + _).mkString(" "))
  }
}

case class Tweet(subject: String, url: String, tags: String) {
  def mkTweet() = {
    val tailWithoutSemester = " " + url
    val tailSemester = tailWithoutSemester + " " + tags
    val tail =
      if (tailSemester.length > 130) tailWithoutSemester
      else tailSemester
    val maxlen = 140 - tail.length
    val shortSubject =
      if (subject.length <= maxlen) subject
      else subject.slice(0, maxlen-3)+"..."
    shortSubject + tail
  }
}
