package wiring

import akka.actor.ActorSystem
import play.api.routing.Router
import play.api.BuiltInComponentsFromContext
import controllers.{AssetsComponents, HomeController}
import play.filters.HttpFiltersComponents

trait AppComponents extends AssetsComponents
  with HttpFiltersComponents {
  self: BuiltInComponentsFromContext =>

  implicit val as: ActorSystem = actorSystem

  lazy val assetController = new controllers.Assets(httpErrorHandler, assetsMetadata)

  override lazy val router: Router = new _root_.router.Routes(
    httpErrorHandler,
    new HomeController(controllerComponents, executionContext),
    assetController
  )
}
