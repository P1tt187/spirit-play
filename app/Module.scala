import java.time.Clock

import com.google.inject.AbstractModule
import helpers.{SemesterModeCache, SessionCache}
import logic.actors.rss.{DeleteNewsActor, NewsReaderActor, RSSParseActor}
import logic.actors.schedule._
import logic.actors.spread.TweetActor
import model.schedule.data.MSchedule
import play.api.libs.concurrent.AkkaGuiceSupport
import services.{NewsFeedReader, ScheduleParser}

/**
  * This class is a Guice module that tells Guice how to bind several
  * different types. This Guice module is created when the Play
  * application starts.
  *
  * Play will automatically use any class called `Module` that is in
  * the root package. You can create modules in other locations by
  * adding `play.modules.enabled` settings to the `application.conf`
  * configuration file.
  */
class Module extends AbstractModule with AkkaGuiceSupport {

  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // bind actors
    bindActor[RSSParseActor]("rssParser")
    bindActor[NewsReaderActor]("NewsReader")
    bindActor[DeleteNewsActor]("deleteNews")
    // Ask Guice to create an instance of NewsFeedReader when the
    // application starts.
    bind(classOf[NewsFeedReader]).asEagerSingleton()


    bindActor[CheckScheduleDateActor]("checkSchedule")
    bind(classOf[ScheduleParser]).asEagerSingleton()

    bindActor[ScheduleDownloadActor]("scheduleDownloader")
    bindActor[ShortCutParseActor]("shortcutParser")
    bindActor[ScheduleParseActor]("parseActor")
    bind(classOf[MSchedule])
    bindActor[GroupParseActor]("groupParseActor")
    bindActor[TweetActor]("tweetActor")

    bind(classOf[SemesterModeCache])
    bind(classOf[SessionCache])
  }

}
