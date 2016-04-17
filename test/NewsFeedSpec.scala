import model.database.{LatestNumberDA, NewsEntryDA, SemesterModeDA}
import model.news.{LatestNumber, NewsEntry}
import model.schedule.meta.SemesterMode
import org.scalatestplus.play._
import play.api._
import play.api.cache.{CacheApi, EhCacheModule}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WsTestClient
import util.FakeCache
import play.api.inject.bind

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author fabian 
  * @since 27.03.16.
  */
class NewsFeedSpec extends PlaySpec with OneAppPerSuite {

  implicit override lazy val app = new GuiceApplicationBuilder()
    .overrides(bind[CacheApi].to[FakeCache])
    .loadConfig(env => Configuration.load(env))
    .in(Mode.Test)
    .build

  val url = app.configuration.underlying.getString("rss.uri")

  "Connect to RSS Feed" should {

    "delete all entrys" in {
      import NewsEntryDA._

      findAllExtended[NewsEntry]().foreach{
        elem=>
          delete(elem.id)
      }

      LatestNumberDA.findAllExtended[LatestNumber]().foreach(  elem => LatestNumberDA.delete(elem.id) )

      findAll().isEmpty mustBe true
      LatestNumberDA.findAllExtended[LatestNumber]().isEmpty mustBe true
      SemesterModeDA.findAllExtended[SemesterMode]().foreach(elem => SemesterModeDA.delete(elem.id))
      SemesterModeDA.findAll().isEmpty mustBe true
    }

    "latest number increment" in {
      LatestNumberDA.findAllExtended[LatestNumber]().foreach(  elem => LatestNumberDA.delete(elem.id) )
      LatestNumberDA.insert(LatestNumber(1))
      val id = LatestNumberDA.findAllExtended[LatestNumber]().head.id
      LatestNumberDA.update(id, LatestNumber(2))
      val entry = LatestNumberDA.findAllExtended[LatestNumber]().head.doc

      entry.number mustBe 2
      LatestNumberDA.findAllExtended[LatestNumber]().foreach(  elem => LatestNumberDA.delete(elem.id) )
      LatestNumberDA.findAllExtended[LatestNumber]().isEmpty mustBe true
    }

    "connect to rss feed" in {
      WsTestClient.withClient {
        ws =>
          val wsRequest = ws.url(url)
          val expectedResult = true

          val result = Await.result(wsRequest.get(), 10 seconds)

          val success: Boolean = result.status == 200
          expectedResult mustBe success
      }
    }
  }
}
