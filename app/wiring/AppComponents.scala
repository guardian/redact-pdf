package wiring

import org.apache.pekko.actor.ActorSystem
import play.api.routing.Router
import play.api.BuiltInComponentsFromContext
import controllers.{AssetsComponents, Application}
import play.api.mvc.EssentialFilter
import play.filters.gzip.GzipFilterComponents
import play.filters.HttpFiltersComponents
import play.filters.hosts.AllowedHostsFilter

trait AppComponents extends AssetsComponents
  with HttpFiltersComponents
  with GzipFilterComponents {
  self: BuiltInComponentsFromContext =>

  implicit val as: ActorSystem = actorSystem

  override def httpFilters: Seq[EssentialFilter] =  gzipFilter +: super.httpFilters.filterNot(_.getClass == classOf[AllowedHostsFilter])

  lazy val assetController = new controllers.Assets(httpErrorHandler, assetsMetadata, environment)

  override lazy val router: Router = new _root_.router.Routes(
    httpErrorHandler,
    new Application(controllerComponents),
    assetController
  )
}
