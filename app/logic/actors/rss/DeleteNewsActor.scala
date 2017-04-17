package logic.actors.rss

import javax.inject._

import akka.actor.{Actor, ActorRef}
import logic.actors.database.DatabaseActor.{DeleteEntry, GiveNewsEntrys, NewsEntrys}
import model.database.NewsEntryDA
import model.news.NewsEntry
import org.joda.time.DateTime
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author fabian 
  *         on 10.04.16.
  *         this actor delete old news
  */
object DeleteNewsActor {

  case object DeleteNews

}

@Singleton
class DeleteNewsActor @Inject()(@Named("databaseActor") databaseActor: ActorRef) extends Actor {

  import DeleteNewsActor._

  override def receive: Receive = {
    case DeleteNews =>
      implicit val timeout = Timeout(30 seconds)
      val now = new DateTime()
      val allNews = Await.result(databaseActor ? GiveNewsEntrys, timeout.duration).asInstanceOf[NewsEntrys].newsEntrys
      allNews.foreach {
        news =>
          if (now.isAfter(news.expireDate)) {
            databaseActor ! DeleteEntry(news)
          }
      }
  }
}
