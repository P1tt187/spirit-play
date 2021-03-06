package model.database

import com.ning.http.client.AsyncHttpClientConfig
import com.ning.http.client.Realm.{AuthScheme, RealmBuilder}
import jp.co.bizreach.elasticsearch4s._
import model.news.{LatestNumber, NewsEntry}
import model.schedule.meta.{ScheduleDate, SemesterMode}
import org.codelibs.elasticsearch.querybuilders.SearchDslBuilder

import play.api.Logger

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

/**
  * @author fabian 
  *         on 28.03.16.
  *         Database helper class
  */
sealed trait DatabaseService[A <: AnyRef] {

  /** config for database scheme */
  val config: ESConfig = "spirit-play" / this.getClass.getSimpleName

  @throws[Exception]
  def apply[A](action: ESClient => A): A = {

    val systemEnv = System.getenv()

    val realm = if (systemEnv.get("ELASTIC_PRINCIPAL") != null) {
      new RealmBuilder()
        .setPrincipal(systemEnv.get("ELASTIC_PRINCIPAL")).setPassword(systemEnv.get("ELASTIC_PASSWORD"))
        .setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC)
        .build()
    } else {
      new RealmBuilder().build()
    }

    ESClient.using(systemEnv.getOrDefault("ELASTICSEARCH_URL", "http://localhost:9200"), config = new AsyncHttpClientConfig.Builder()
      .setRealm(realm)
      .build()) {
      client =>
        val result = action(client)
        result
    }
  }

  @throws[Exception]
  def insert(value: A): Either[Map[String, Any], Map[String, Any]] = {
    apply {
      implicit client =>
        // Logger.debug("insert data " + value)
        val res = client.insert(config, value)
        client.refresh(config)
        res
    }
  }

  @throws[Exception]
  def insert(values: Traversable[A]) {
    val notInserted = ListBuffer[A]()
    values.foreach {
      v =>
        apply {
          implicit client =>
            val res = client.insert(config, v)
            res match {
              case Left(_) =>
                Thread.sleep(1000)
                notInserted += v
              case _ =>
            }
        }
    }
    if (notInserted.nonEmpty) {
      Logger.debug("retry inserting: " + notInserted)
      insert(notInserted)
    }
    apply(_.refresh(config))
  }

  @throws[Exception]
  def insertJson(json: String): Either[Map[String, Any], Map[String, Any]] = {
    apply {
      implicit client =>
        //  Logger.debug("insert data " + json)
        client.insertJson(config, json)
    }
  }

  @throws[Exception]
  def update(id: String, entity: A): Either[Map[String, Any], Map[String, Any]] = {
    apply {
      implicit client =>
        val result = client.update(config, id, entity)
        client.refresh(config)
        result
    }
  }


  def updateJson(id: String, json: String): Either[Map[String, Any], Map[String, Any]] = {
    apply {
      implicit client =>
        val result = client.updateJson(config, id, json)
        client.refresh(config)
        result
    }
  }


  def delete(id: String): Either[Map[String, Any], Map[String, Any]] = {
    apply {
      implicit client =>
        val result = client.delete(config, id)
        client.refresh(config)
        result
    }
  }


  def delete(ids: Traversable[String]): Unit = {
    ids.foreach {
      id =>
        apply {
          implicit client =>
            client.delete(config, id)
        }
    }
    apply {
      implicit client =>
        client.refresh(config)
    }
  }


  def find[T](f: SearchDslBuilder => Unit)(implicit c: ClassTag[T]): Option[(String, T)] = {
    apply {
      implicit client =>
        client.find[T](config)(f)
    }
  }

  def findAsList[T](f: SearchDslBuilder => Unit)(implicit c: ClassTag[T]): Option[List[(String, T)]] = {
    try {
      Some(apply {
        implicit client =>
          client.findAsList[T](config)(f)
      })
    } catch {
      case e: Exception =>
        Logger.debug("error", e)
        None
    }
  }


  def list[T](f: SearchDslBuilder => Unit)(implicit c: ClassTag[T]): Option[List[T]] = {
    try {
      Some(apply {
        implicit client =>
          client.list[T](config)(f).list.map(_.doc)
      })
    } catch {
      case e: Exception =>
        Logger.debug("error", e)
        None
    }
  }


  def listExtended[T](f: SearchDslBuilder => Unit)(implicit c: ClassTag[T]): Option[List[ESSearchResultItem[T]]] = {
    try {
      Some(apply {
        implicit client =>
          client.list[T](config)(f).list
      })
    } catch {
      case e: Exception =>
        Logger.debug("error", e)
        None
    }
  }


  def findAll[T: ClassTag](): Option[List[T]] = {
    list[T] {
      searcher =>
        searcher.query(matchAllQuery)
        searcher.size(10000)
    }
  }

  def findAllExtended[T: ClassTag](): Option[List[ESSearchResultItem[T]]] = {
    listExtended[T] {
      searcher =>
        searcher.query(matchAllQuery)
        searcher.size(10000)
    }
  }


  def findById[T: ClassTag](id: String): T = {
    find[T] {
      searcher =>
        searcher.query(termQuery("_id", id))
    }.get._2
  }


  def findByIdOption[T: ClassTag](id: String): Option[T] = {
    find[T] {
      searcher =>
        searcher.query(termQuery("_id", id))
    } match {
      case Some(x) => Some(x._2)
      case None => None
    }
  }
}

object NewsEntryDA extends DatabaseService[NewsEntry] {

}

object LatestNumberDA extends DatabaseService[LatestNumber] {

}

object ScheduleDateDA extends DatabaseService[ScheduleDate] {

}

object SemesterModeDA extends DatabaseService[SemesterMode] {

}
