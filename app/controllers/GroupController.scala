package controllers

import javax.inject._

import akka.actor.ActorRef
import akka.pattern.ask
import logic.actors.database.DatabaseActor.{GiveGroups, Groups}
import play.api.mvc._
import views.html.groups._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author fabian 
  *         on 14.04.16.
  */
@Singleton
class GroupController @Inject()(@Named("databaseActor") databaseActor: ActorRef) extends AbstractController {
  /** create the grouppage */
  def index() = sessionCache.cached("groups") {
    Action {
      implicit request =>
        val groups = Await.result(databaseActor ? GiveGroups, 1 minute).asInstanceOf[Groups].g.filter(gr => courseNames.flatMap(_._2).contains(gr.className))

        Ok(displayGroups(courseNames, groups))
    }
  }

}
