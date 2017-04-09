package controllers

import javax.inject._

import akka.util.Timeout
import helpers.SpiritHelper
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import scala.concurrent.duration._

/**
  * @author fabian 
  * @since 27.03.16.
  *        Abstract controller which is the base class for all controllers
  *        and contains some vars and methods which are needed on every controller
  */
abstract class AbstractController extends Controller with SpiritHelper with I18nSupport {

  implicit val myCustomCharset = Codec.javaSupported("utf-8")

  /** needed for webjars, css and js librarys as java dependencys */
  @Inject() protected implicit var webJarAssets: WebJarAssets = null
  /** message api for translation */
  @Inject() protected implicit var m: MessagesApi = null

  implicit val timeout = Timeout(60 seconds)

  override def messagesApi: MessagesApi = m

}
