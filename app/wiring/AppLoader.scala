package wiring

import play.api.ApplicationLoader.Context
import play.api._

import com.typesafe.config.ConfigFactory

class AppLoader extends ApplicationLoader {

  val config = ConfigFactory.load()
  val enableExactStringMatching = config.getBoolean("redacted-exact-strings.enabled")
  val enableGreedyNameMatching = config.getBoolean("greedy-name-match.enabled")
  val enableNewPageSplittingAndDeletion = config.getBoolean("new-page-split-behaviour.enabled")

  Logger.info("Starting Application with the following configuration")
  Logger.info(s"Exact Redacted Strings matching is set to: $enableExactStringMatching")
  Logger.info(s"Greedy Candidate Name matching is set to: $enableGreedyNameMatching")
  Logger.info(s"Alternative Page Splitting and Deleting Cover Page is set to: $enableNewPageSplittingAndDeletion")


  override def load(context: Context): Application = {

    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }

    (new BuiltInComponentsFromContext(context) with AppComponents).application
  }
}