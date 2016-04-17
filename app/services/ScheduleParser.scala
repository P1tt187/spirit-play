package services

import java.time.{Clock, Instant}
import javax.inject._

import akka.actor.{ActorRef, ActorSystem, Cancellable}
import play.api.Logger
import play.api._
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future
import logic.actors.schedule.CheckScheduleDateActor._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author fabian 
  *         on 03.04.16.
  */
@Singleton
class ScheduleParser @Inject()(clock: Clock, appLifecycle: ApplicationLifecycle,system: ActorSystem , env: Environment,
                               @Named("checkSchedule") checkScheduleDateActor: ActorRef) {

  // This code is called when the application starts.
  private val start: Instant = clock.instant
  private var cancellable1:Cancellable = null



  Logger.info(s"ScheduleParser: Starting application at ${start}.")

  if(env.mode == Mode.Prod || env.mode == Mode.Dev) {
    cancellable1 = system.scheduler.schedule(2 second, 1 day ,checkScheduleDateActor , CheckScheduleDate)
  }

  // When the application starts, register a stop hook with the
  // ApplicationLifecyle object. The code inside the stop hook wil
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    val stop: Instant = clock.instant
    val runningTime: Long = stop.getEpochSecond - start.getEpochSecond
    Logger.info(s"ScheduleParser: Stopping application at ${clock.instant} after ${runningTime}s.")
    if(cancellable1 != null){
      cancellable1.cancel()
    }
    Future.successful(())
  }

}
