package filters

import javax.inject.Inject

import play.api.Configuration
import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter

/**
  * @author fabian 
  *         on 19.04.16.
  */
class DefaultFilter @Inject() (gzipFilter: GzipFilter, configuration: Configuration) extends HttpFilters {
  def filters = {
    if(configuration.getString("spirit.mode").getOrElse("dev").equals("dev")) {
      Seq(gzipFilter)
    } else {
      Seq.empty
    }
  }

}
