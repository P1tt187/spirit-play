package controllers

import javax.inject._

import helpers.{CacheHelper, SpiritHelper}
import play.api.i18n.MessagesApi
import play.api.mvc._

/**
  * @author fabian 
  * @since 27.03.16.
  */
abstract class AbstractController extends Controller with SpiritHelper {

  @Inject() protected implicit var webJarAssets: WebJarAssets = null
  @Inject() protected implicit var m: MessagesApi = null

}
