package controllers

import model.database.GroupDA
import play.api.Logger
import play.api.mvc._
import views.html.groups._

/**
  * @author fabian 
  *         on 14.04.16.
  */
class GroupController extends AbstractController {

  def index() = Action {
    implicit request =>
      val groups = GroupDA.findByCourseNames(courseNames.flatMap(_._2))

      Ok(displayGroups(courseNames, groups))
  }

}
