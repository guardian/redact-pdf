package taleo

import collection.JavaConverters._
import zio.duration.durationInt
import akka.actor.Scheduler
import zio.{Schedule, ZIO}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions, ProfilesIni}
import zio.clock.Clock

object Authentication {
  val geckodriverPath = "./bin/geckodriver-osx"
  val browserProfileName = "default-release-1"
  System.setProperty("webdriver.gecko.driver", geckodriverPath)

  def schedule = Schedule.recurs(60) && Schedule.spaced(1.second)

  def authenticate(taleoUrl: String): ZIO[Clock, String, TaleoCredentials] = {
    val capabilities = new FirefoxOptions()
      .setProfile(new ProfilesIni().getProfile(browserProfileName))

    val driver = new FirefoxDriver(capabilities)

    def quitDriver = ZIO.effect { driver.quit() }.fold(_ => (), identity)

    def isReady = ZIO.effect {
      if (!driver.getCurrentUrl.contains("isNavigationCompleted=true")) {
        throw new Exception("Not ready")
      }
    }

    def getUrl(url: String) = ZIO.effect {
      driver.get(url)
    }

    getUrl(s"$taleoUrl/enterprise/fluid?root=submissions_view&lang=en").mapError(_ => "Failed to load Taleo url").bracket(_ => quitDriver) { _ =>
      for {
        _ <- isReady.retry(schedule).mapError(_ => "Timeout while attempting to authenticate")
        result <- {
          val cookies = driver.manage().getCookies.asScala.map { cookie =>
            cookie.getName -> cookie.getValue
          }.toMap

          val maybeCsrfToken = driver.executeScript("return window.sessionOpts.token") match {
            case result: String => Some(result)
            case _ => None
          }

          (for {
            csrfToken <- maybeCsrfToken
            taleoSession <- cookies.get("taleosession")
            jSessionId <- cookies.get("JSESSIONID")
            sessionId <- cookies.get("arg_sessionid")
          } yield ZIO.succeed(TaleoCredentials(
            sessionId = sessionId,
            jSessionId = jSessionId,
            taleoSession = taleoSession,
            csrfToken = csrfToken,
          ))
            ).getOrElse(ZIO.fail("Authentication failed - missing cookies"))
        }
      } yield result
    }
  }
}
