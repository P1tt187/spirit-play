import javax.xml.ws.WebServiceProvider

import akka.actor.{ActorRef, ActorSystem, Props}
import controllers._
import logic.actors.spread.TweetActor
import model.news.NewsEntry
import model.spread.Tweet
import org.joda.time.DateTime
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.cache.CacheApi
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient
import play.api.test.Helpers._
import play.api.test.WsTestClient
import play.api.{Configuration, Logger, Mode}
import util.FakeCache
import scala.concurrent.duration._

import scala.concurrent.Await

/**
  * @author fabian 
  *         on 16.04.16.
  */
class TweetSpec extends PlaySpec with OneAppPerSuite {

  val actorSystem = ActorSystem("test")

  implicit override lazy val app = new GuiceApplicationBuilder()
    .overrides(bind[CacheApi].to[FakeCache])
    .overrides(bind[ActorSystem] toInstance actorSystem)
    .loadConfig(env => Configuration.load(env))
    .in(Mode.Test)
    .build

  "Tweet test" should {
    "Convert news to tweet" in {
      val result = Tweet.newsToTweet(
        NewsEntry("foo", "bar", "foobar", new DateTime(), "", new DateTime(), List("foo", "bar"), 42))
      val expected = Tweet("foo", "http://localhost:9000" + routes.NewsPageController.newsEntry(42).url, "#foo #bar")
      result mustEqual expected
    }

    "tweetmessage" in {
      val result = Tweet("foo", routes.NewsPageController.newsEntry(42).url, "#foo #bar").mkTweet
      val expected = "foo /newsEntry/42 #foo #bar"
      result mustEqual expected
    }

    "tweetLongMessage" in {
      val result = Tweet("ffffffffffffffffffffffffffffffffffffffffffffffooooooooooooooo ooooooooooooo bbbbbbbbbbbbbbbbbbbbbbbbbbbbb aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr", routes.NewsPageController.newsEntry(42).url, "#foo #bar").mkTweet
      val expected = "ffffffffffffffffffffffffffffffffffffffffffffffooooooooooooooo ooooooooooooo bbbbbbbbbbbbbbbbbbbbbbbbbbbbb aaaaaaa... /newsEntry/42 #foo #bar"
      result mustEqual expected
    }

    "tweetActorTest" in {
      running(app) {
        val injector = app.injector
        val actor = injector.instanceOf(BindingKey(classOf[ActorRef]).qualifiedWith("tweetActor"))
        actor ! Tweet("Test from Spirit2", "", "#tweet")
      }
    }

    "testshorter" in {
      running(app) {
        WsTestClient.withClient { client =>
          val longUrl = "https://fsi.fh-schmalkalden.de/spirit/"
          val request = client.url("https://is.gd/create.php").withQueryString(("url", longUrl),("format","simple"))
          val response = Await.result(request.get(), 10 seconds)
          val shortUrl = response.body
          Logger.debug(shortUrl)
          shortUrl.equalsIgnoreCase("Error: Please enter a valid URL to shorten") mustEqual false
        }
      }
    }
  }
}
