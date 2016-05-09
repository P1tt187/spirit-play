package errorhandler

import javax.inject._

import controllers.WebJarAssets
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router

import scala.concurrent._

/**
  * @author fabian 
  *         on 09.05.16.
  */
@Singleton
class ErrorHandler @Inject()(implicit val messagesApi: MessagesApi, env: Environment,
                             config: Configuration,
                             sourceMapper: OptionalSourceMapper,
                             router: Provider[Router]) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) with I18nSupport {
  /** provide custom 404 page */
  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {

    implicit val req = request.asInstanceOf[Request[_]]

    implicit val webJarAssets: WebJarAssets = new WebJarAssets(this, config, env)
    Future.successful(NotFound(views.html.errorpages.pageNotFound()))
  }
}
