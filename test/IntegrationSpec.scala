import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.{Configuration, Mode}
import play.api.cache.{CacheApi, EhCacheModule}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers._
import util.FakeCache

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class IntegrationSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerTest with HtmlUnitFactory {

  implicit override lazy val app = new GuiceApplicationBuilder()
    .overrides(bind[CacheApi].to[FakeCache])
    .loadConfig(env => Configuration.load(env))
    .in(Mode.Test)
    .build

  "Application" should {

    "work from within a browser" in {

      go to ("http://localhost:" + port)

      pageSource must include ("Aktuelles der Fakultät Informatik")
    }
  }
}
