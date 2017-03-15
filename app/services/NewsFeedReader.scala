package services

import java.time.{Clock, Instant}
import javax.inject._

import akka.actor.{ActorRef, ActorSystem, Cancellable}
import logic.actors.rss.DeleteNewsActor._
import logic.actors.rss.NewsReaderActor._
import play.api.{Logger, _}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * This class demonstrates how to run code when the
  * application starts and stops. It starts a timer when the
  * application starts. When the application stops it prints out how
  * long the application was running for.
  *
  * This class is registered for Guice dependency injection in the
  * [[Module]] class. We want the class to start when the application
  * starts, so it is registered as an "eager singleton". See the code
  * in the [[Module]] class to see how this happens.
  *
  * This class needs to run code when the server stops. It uses the
  * application's [[ApplicationLifecycle]] to register a stop hook.
  */
@Singleton
class NewsFeedReader @Inject()(clock: Clock, appLifecycle: ApplicationLifecycle,
                               system: ActorSystem, @Named("NewsReader") newsReaderActor: ActorRef,
                               @Named("deleteNews") deleteNewsActor: ActorRef, env: Environment) {

  // This code is called when the application starts.
  private val start: Instant = clock.instant
  private var cancellableFeedParser: Cancellable = null
  private var cancellableNewsDeleter: Cancellable = null

  Logger.info(s"NewsFeedReader: Starting application at ${start}.")

  Logger.info("Mode: " + env.mode)
  if (env.mode == Mode.Prod || env.mode == Mode.Dev) {
    cancellableFeedParser = system.scheduler.schedule(3 second, 1 hour, newsReaderActor, ReadNews)
    cancellableNewsDeleter = system.scheduler.schedule(10 seconds, 6 hours, deleteNewsActor, DeleteNews)
  }


  // When the application starts, register a stop hook with the
  // ApplicationLifecyle object. The code inside the stop hook wil
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    val stop: Instant = clock.instant
    val runningTime: Long = stop.getEpochSecond - start.getEpochSecond
    Logger.info(s"NewsFeedReader: Stopping application at ${clock.instant} after ${runningTime}s.")
    if (cancellableFeedParser != null) {
      cancellableFeedParser.cancel()
    }
    if (cancellableNewsDeleter != null) {
      cancellableNewsDeleter.cancel()
    }
    Future.successful(())
  }
}
