package logic.actors.rss

import javax.inject._

import akka.actor._
import helpers.SpiritHelper
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author fabian 
  * @since 27.03.16.
  *        The newsreader downloads an rss feed send the content to an RssParser
  */

@Singleton
class NewsReaderActor @Inject()(configuration: Configuration, ws: WSClient,
                                @Named("rssParser") rssParserActor: ActorRef) extends Actor with SpiritHelper {

  import NewsReaderActor._
  import RSSParseActor._

  val feedUrl = configuration.underlying.getString("rss.uri")

  override def receive: Receive = {
    case ReadNews =>
      Logger.debug("Reading news")
      val response = Await.result(ws.url(feedUrl).get(), 10 seconds)
      //Logger.debug(response.statusText)
      if (response.status != 200) {
        Logger.error("error while news reading " + response.statusText)
      } else {
        // val responseString = response.bodyAsBytes.decodeString(StandardCharsets.ISO_8859_1.toString)
        val responseString = response.body
        val cachedValue = sessionCache.getOrElse("feedcontent")("")
        if(!responseString.equals(cachedValue)) {
          sessionCache.set("feedcontent", responseString, Duration.Inf)
          rssParserActor ! RSSFeed(responseString)
        }
      }
    case PoisonPill =>

  }
}

object NewsReaderActor {
  def props = Props[NewsReaderActor]

  case object ReadNews

}