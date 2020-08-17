package services

import com.google.inject.{ImplementedBy, Singleton}
import db.ShortUrlDb

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@ImplementedBy(classOf[UrlShorteningServiceImpl])
trait UrlShorteningService {

  def shortenUrl(clientId: Long, longUrl: String): Future[String]

  def getOriginalUrl(shortUrl: String): Future[Option[String]]

  def getHitCount(shortUrl: String): Future[Option[Int]]
}

object UrlShorteningService {
  final val baseUrl = "http://localhost:9000/"

}

@Singleton
class UrlShorteningServiceImpl extends UrlShorteningService {
  import ShortUrlDb._
  import scala.concurrent.ExecutionContext.Implicits.global

  override def shortenUrl(clientId: Long, longUrl: String): Future[String] = Future {
    clientUrlMap.get(clientId).flatMap(_.get(longUrl)) match {
      case Some(url) => UrlShorteningService.baseUrl+url._1
      case None =>
        val sUrl = clientUrlMap.contains(clientId) match {
          case true =>
            val urlMaps = clientUrlMap(clientId)
            val urlId: Long = urlMaps.size+1
            val shortUrl = shorten(clientId, urlId)
            val withNewUrl = urlMaps + (longUrl -> (shortUrl, urlId))
            clientUrlMap = clientUrlMap.removed(clientId) + (clientId -> withNewUrl)
            shortUrl
          case false =>
            val shortUrl = shorten(clientId, 0)
            clientUrlMap = clientUrlMap + (clientId -> Map(longUrl -> (shortUrl, 0)))
            shortUrl
        }
        shortUrls = shortUrls + (sUrl -> UrlDetails(longUrl, 1))
        println(UrlShorteningService.baseUrl+sUrl)
        UrlShorteningService.baseUrl+sUrl
    }
  }

  override def getOriginalUrl(shortUrl: String): Future[Option[String]] = Future {
    shortUrls.contains(shortUrl) match {
      case true =>
        val urlDetails = shortUrls(shortUrl)
        val hitCount = urlDetails.hitCount
        shortUrls = shortUrls.removed(shortUrl)
        shortUrls =  shortUrls + (shortUrl -> urlDetails.copy(hitCount = hitCount+1))
      case false => None
    }
    shortUrls.get(shortUrl).map(_.longUrl)
  }

  override def getHitCount(shortUrl: String): Future[Option[Int]] = Future {
    println(shortUrl)
    println(shortUrls)
    shortUrls.get(shortUrl).map(_.hitCount)
  }

  private def shorten(clientId: Long, urlId: Long): String =  {
    val base62 = new Base62()
    val clientIdHash = base62.encode(clientId)
    val urlIdHash = base62.encode(urlId)
    println(clientId+" "+urlId)
    println(clientIdHash+" "+urlIdHash)

    val encodedStr = clientIdHash+urlIdHash

    if(encodedStr.length < 8) {
      val sb = new StringBuilder(encodedStr).append(randomAlphaNumericString(8-encodedStr.length))
      sb.toString()
    } else if(encodedStr.length > 8) {
      encodedStr.substring(0, 8)
    } else encodedStr
  }

  def randomAlphaNumericString(length: Int): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
    randomStringFromCharList(length, chars)
  }
  def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }
}

