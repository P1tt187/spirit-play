package services

import java.time.{Clock, Instant}
import javax.inject._

import akka.actor.{ActorRef, ActorSystem, Cancellable}
import logic.actors.fakeactivity.FakeActivityActor.FakeActivity
import model.database.EmbeddedElasticsearchServer
import play.api.inject.ApplicationLifecycle
import play.api.{Logger, _}

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
class ElasticSearchService @Inject()(clock: Clock, appLifecycle: ApplicationLifecycle,
                                     env: Environment) {

  // This code is called when the application starts.
  private val start: Instant = clock.instant

  Logger.info(s"Embedded Elasticsearch: Starting application at ${start}.")

  Logger.info("Mode: " + env.mode)
  EmbeddedElasticsearchServer.start()


  // When the application starts, register a stop hook with the
  // ApplicationLifecyle object. The code inside the stop hook wil
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    val stop: Instant = clock.instant
    val runningTime: Long = stop.getEpochSecond - start.getEpochSecond
    Logger.info(s"FakeActivity: Stopping application at ${clock.instant} after ${runningTime}s.")
    EmbeddedElasticsearchServer.shutDown()

    Future.successful(())
  }
}
