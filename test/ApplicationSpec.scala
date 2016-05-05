
import org.scalatestplus.play._
import play.api._
import play.api.cache.CacheApi
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test._
import util.FakeCache

/**
  * Add your spec here.
  * You can mock out a whole application including requests, plugins etc.
  * For more information, consult the wiki.
  */
class ApplicationSpec extends PlaySpec with OneAppPerSuite {

  implicit override lazy val app = new GuiceApplicationBuilder()
    .overrides(bind[CacheApi].to[FakeCache])
    .loadConfig(env => Configuration.load(env))
    .in(Mode.Test)
    .build

  "Routes" should {

    "send 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

  }

  "NewsPageController" should {

    "render the index page" in {
      val home = route(app, FakeRequest(GET, "/")).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include("Aktuelles der Fakultät Informatik")
    }

  }

}
