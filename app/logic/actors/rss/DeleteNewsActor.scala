package logic.actors.rss

import javax.inject._

import akka.actor.Actor
import model.database.NewsEntryDA
import model.news.NewsEntry
import org.joda.time.DateTime

/**
  * @author fabian 
  *         on 10.04.16.
  *         this actor delete old news
  */
object DeleteNewsActor {

  case object DeleteNews

}

@Singleton
class DeleteNewsActor extends Actor {

  import DeleteNewsActor._

  override def receive: Receive = {
    case DeleteNews =>
      val now = new DateTime()
      val allNews = NewsEntryDA.findAllExtended[NewsEntry]()
      allNews.foreach {
        news =>
          if (now.isAfter(news.doc.expireDate)) {
            NewsEntryDA.delete(news.id)
          }
      }
  }
}
