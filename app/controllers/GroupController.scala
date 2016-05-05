package controllers

import javax.inject._
import model.database.GroupDA
import play.api.mvc._
import views.html.groups._

/**
  * @author fabian 
  *         on 14.04.16.
  */
@Singleton
class GroupController extends AbstractController {
  /** create the grouppage */
  def index() = sessionCache.cached("groups") {
    Action {
      implicit request =>
        val groups = GroupDA.findByCourseNames(courseNames.flatMap(_._2))

        Ok(displayGroups(courseNames, groups))
    }
  }

}
