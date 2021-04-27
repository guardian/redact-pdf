package wiring

import akka.actor.ActorSystem
import play.api.routing.Router
import play.api.BuiltInComponentsFromContext
import controllers.{Application, AssetsComponents}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import play.filters.hosts.AllowedHostsFilter

trait AppComponents extends AssetsComponents
  with HttpFiltersComponents with AhcWSComponents {
  self: BuiltInComponentsFromContext =>

  implicit val as: ActorSystem = actorSystem

  override def httpFilters: Seq[EssentialFilter] =
    super.httpFilters.filterNot(_.getClass == classOf[AllowedHostsFilter])

  lazy val assetController = new controllers.Assets(httpErrorHandler, assetsMetadata)

  override lazy val router: Router = new _root_.router.Routes(
    httpErrorHandler,
    new Application(controllerComponents, configuration, wsClient),
    assetController
  )
}
