package errorhandler

import javax.inject._

import controllers.WebJarAssets
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.http.Status._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import views.html.errorpages._

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
    Future.successful(NotFound(pageNotFound()))
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    implicit val req = request.asInstanceOf[Request[_]]

    implicit val webJarAssets: WebJarAssets = new WebJarAssets(this, config, env)
    statusCode match {
      case NOT_FOUND => onNotFound(request, message)
      case FORBIDDEN => Future.successful(Forbidden(errorPage(statusCode.toString, message)))
      case BAD_REQUEST => Future.successful(BadRequest(errorPage(statusCode.toString, message)))
      case _ => Future.successful(Ok(errorPage(statusCode.toString, message)))
    }
  }
}
