package db


object ShortUrlDb {

  case class UrlDetails(longUrl: String, hitCount: Int)

  //ClientId -> (LongUrl -> ShortUrl, HitCount))
  var clientUrlMap = Map.empty[Long,  Map[String, (String, Long)]]

  var shortUrls = Map.empty[String, UrlDetails]


}
