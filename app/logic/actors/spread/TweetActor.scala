package logic.actors.spread

import javax.inject._

import akka.actor.Actor
import helpers.SpiritHelper
import model.news.NewsEntry
import model.spread.Tweet
import play.api.libs.ws.WSClient
import play.api.{Logger, Mode}
import twitter4j.conf.ConfigurationBuilder
import twitter4j.{Twitter, TwitterFactory}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author fabian 
  *         on 16.04.16.
  */
@Singleton
class TweetActor @Inject()(ws: WSClient) extends Actor with SpiritHelper {

  /**
    * config for twitter
    *
    * twitter {
    * Consumer=xxxxxxxxxxxxxxxxx
    * ConsumerSecret=xxxxxxxxxxxxxxxxxxxxxxxxxxx
    * Token=xxxxxxxx-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
    * TokenSecret=xxxxxxxxxxxxxxxxxxxxxxxxx
    * }
    */
  private var tweetEnabled = false
  private var twitter: Twitter = null
  private var hostUrl: String = null

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    val consumer = configuration.getString("twitter.Consumer").getOrElse("")
    val consumerSecret = configuration.getString("twitter.ConsumerSecret").getOrElse("")
    val token = configuration.getString("twitter.Token").getOrElse("")
    val tokenSecret = configuration.getString("twitter.TokenSecret").getOrElse("")
    val cb = new ConfigurationBuilder()
    if (env.mode != Mode.Prod) {
      cb.setDebugEnabled(true)
    }

    cb.setOAuthConsumerKey(consumer)
    cb.setOAuthConsumerSecret(consumerSecret)
    cb.setOAuthAccessToken(token)
    cb.setOAuthAccessTokenSecret(tokenSecret)
    tweetEnabled = List(consumer, consumerSecret, tokenSecret, token).forall(_.nonEmpty) && configuration.getBoolean("twitter.enabled").getOrElse(false)
    twitter = new TwitterFactory(cb.build()).getInstance()

    hostUrl = configuration.getString("spirit.host").getOrElse("http://localhost:9000")
  }

  override def receive: Receive = {
    case n: NewsEntry =>
      var tweet: Tweet = n
      val longUrl = tweet.url
      val shortUrl = if (tweetEnabled) {
        val response = Await.result(ws.url("http://is.gd/api.php").withQueryString(("longurl", longUrl)).get(), 10 seconds)
        response.body
      } else {
        longUrl
      }
      tweet = tweet.copy(url = shortUrl)
      self ! tweet

    case t: Tweet =>
      if (tweetEnabled) {
        twitter.updateStatus(t.mkTweet())
      } else {
        Logger.error("Dummy Tweet: " + t.mkTweet())
      }
  }
}
