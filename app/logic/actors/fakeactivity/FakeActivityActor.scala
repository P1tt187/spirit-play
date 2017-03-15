package logic.actors.fakeactivity

import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import logic.actors.fakeactivity.FakeActivityActor.FakeActivity
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author fabian 
  *         on 15.03.17.
  */

object FakeActivityActor {

  case object FakeActivity

}

@Singleton
class FakeActivityActor @Inject()(configuration: Configuration, ws: WSClient) extends Actor {

  val newsHost = configuration.getString("spirit.host").getOrElse("http://localhost:9000")

  override def receive: Receive = {
    case FakeActivity =>
      Logger.debug("fake request to " + newsHost)
      Await.result(ws.url(newsHost).get(), 30 seconds)

    case _ =>
  }
}
