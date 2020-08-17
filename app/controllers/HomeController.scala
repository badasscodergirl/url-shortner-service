package controllers

import javax.inject._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.UrlShorteningService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               urlShorteningService: UrlShorteningService) extends BaseController {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def test() = Action {
    Ok("hello")
  }

  def getShortenedURL() = Action.async(parse.json) { request =>
    val payload: JsValue = request.body
    Try {
      val clientId = (payload \ "client_id").as[Long]
      val longUrl = (payload \ "long_url").as[String].trim

      if(longUrl.nonEmpty)
        urlShorteningService.shortenUrl(clientId, longUrl)
      else throw new Exception("bad request")
    }.toEither match {
      case Left(t: Throwable) => Future.successful(BadRequest("failed_to_parse_request"))
      case Right(result) => result.map(shortUrl => Ok(Json.obj("short_url" -> shortUrl)))
    }
  }

  def getOriginalURL(shortUrl: String) = Action.async { _ =>
    Try {
      if(shortUrl.trim.nonEmpty) {
        val sUrl = shortUrl.trim.replace(UrlShorteningService.baseUrl, "")
        urlShorteningService.getOriginalUrl(sUrl)
      } else throw new Exception("bad request")

    }.toEither match {
      case Left(t: Throwable) => Future.successful(BadRequest("failed_to_parse_request"))
      case Right(result) =>  result.map {
        case Some(longUrl) => Found(longUrl)
        case None => Ok("Not_found")
      }
    }
  }

  def getHitCount(shortUrl: String) = Action.async { _ =>
    Try {
      if(shortUrl.trim.nonEmpty) {
        val sUrl = shortUrl.trim.replace(UrlShorteningService.baseUrl, "")
        println(s"here $sUrl")
        urlShorteningService.getHitCount(sUrl)
      } else throw new Exception("bad request")
    }.toEither match {
      case Left(t: Throwable) => Future.successful(BadRequest("failed_to_parse_request"))
      case Right(result) =>  result.map {
        case Some(hitCount) => Ok(Json.obj("hit_count" -> hitCount))
        case None => Ok("Not_found")
      }
    }
  }
}
