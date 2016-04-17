package controllers

import javax.inject._

import akka.stream.scaladsl.Source
import model.database.NewsEntryDA
import model.news.NewsEntry
import org.joda.time.format.ISODateTimeFormat
import play.api.cache.Cached
import play.api.http.ContentTypes
import play.api.libs.Comet
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import views.html.news._


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
class NewsPageController @Inject()(materializer: akka.stream.Materializer, cached: Cached) extends AbstractController {


  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index = Action {
    implicit request =>

      Ok(views.html.news.index(m("NEWSPAGE.PAGETITLE"), courseNames))
  }

  def newsStream(number: Long, minNumber: Long, searchTag: String) =
    Action {
      implicit req =>
        implicit val mat = materializer
        val newsHost = configuration.getString("spirit.host").getOrElse("http://localhost:9000")
        def filterNews = {
          var result = if (number != -1) {
            NewsEntryDA.findAll[NewsEntry]().filter(_.number == number)
          } else {
            NewsEntryDA.findAll[NewsEntry]()
          }

          result = if (minNumber != -1) {
            result.filter(_.number > minNumber)
          } else {
            result
          }

          if (!searchTag.equals("-1")) {
            result.filter(_.tags.contains(searchTag))
          } else {
            result
          }
        }

        def newsSource = Source(filterNews.sortBy(entry => entry.number).map {
          news =>
            Json.obj("html" -> views.html.news.newsEntry(news, newsHost).toString(), "number" -> news.number)
        })

        Ok.chunked(newsSource via Comet.json("parent.newsStream")).as(ContentTypes.HTML)
    }

  def newsEntry(number: Long) = Action {
    implicit request =>
      Ok(views.html.news.index(m("NEWSPAGE.PAGETITLE"), courseNames, number = number))
  }

  def newsTag(tag:String) = Action{
    implicit request=>
      Ok(views.html.news.index(m("NEWSPAGE.TITLE"), courseNames, searchTag = tag))
  }

  /**
    * creates RSS2 feed with latest messages
    *
    * @return XML structure for feed
    */
  def feed = cached("rssfeed") {
    Action {
      implicit request =>
        val hostUrl = configuration.getString("spirit.host").getOrElse("http://localhost:9000")
        val news = NewsEntryDA.findAll[NewsEntry]().sortBy(-_.number)

        def cdata(data: String) = {
          scala.xml.Unparsed("<![CDATA[%s]]>".format(data))
        }

        val rssFeed =
          <rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
            <channel>
              <title>Spirit @ FH-Schmalkalden - RSS Feed</title>
              <link>
                {hostUrl}
              </link>
              <description>RSS Feed für die aktuellen Meldungen am Fachbereich Informatik</description>
              <atom:link href={request.path} rel="self" type="application/rss+xml"/>
              <language>de-de</language>
              <image>
                <url>
                  {hostUrl + routes.Assets.versioned("images/logo_spirit.png")}
                </url>
                <title>Spirit @ FH-Schmalkalden - RSS Feed</title>
                <link>
                  {hostUrl}
                </link>
              </image>
              <ttl>60</ttl>{news.map {
              entry =>
                <item>
                  <title>
                    {entry.title + " " + entry.tags.map("#" + _).mkString(" ")}
                  </title>
                  <author>
                    {cdata(entry.author)}
                  </author>
                  <description>
                    {cdata(entry.newsMessage)}
                  </description>
                  <link>
                    {hostUrl + request.host + routes.NewsPageController.newsEntry(entry.number)}
                  </link>
                  <guid isPermaLink="false">
                    {entry.number}
                  </guid>
                  <pubDate>
                    {ISODateTimeFormat.basicDateTime().print(entry.pubDate)}
                  </pubDate>
                </item>
            }}
            </channel>
          </rss>

        Ok(rssFeed).as("application/rss+xml")
    }
  }

}
