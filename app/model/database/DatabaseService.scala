package model.database

import jp.co.bizreach.elasticsearch4s._
import model.news.{LatestNumber, NewsEntry}
import model.schedule.data.{Group, Lecture, Schedule}
import model.schedule.meta.{ScheduleDate, SemesterMode}
import org.elasticsearch.action.search.SearchRequestBuilder
import play.api.Logger

import scala.reflect.ClassTag

/**
  * @author fabian 
  *         on 28.03.16.
  *         Database helper class
  */
sealed trait DatabaseService[A <: AnyRef] {

  /** config for database scheme */
  val config: ESConfig = "spirit-play" / this.getClass.getSimpleName

  def apply[A](action: ESClient => A): A = {
    ESClient.using("http://localhost:9200") {
      client =>
        val result = action(client)
        result
    }
  }

  def insert(value: A) {
    apply {
      implicit client =>
        // Logger.debug("insert data " + value)
        client.insert(config, value)
        client.refresh(config)
    }
  }

  def insertJson(json: String): Either[Map[String, Any], Map[String, Any]] = {
    apply {
      implicit client =>
        //  Logger.debug("insert data " + json)
        client.insertJson(config, json)
    }
  }

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

  def find[T](f: SearchRequestBuilder => Unit)(implicit c: ClassTag[T]): Option[(String, T)] = {
    apply {
      implicit client =>
        client.find[T](config)(f)
    }
  }

  def findAsList[T](f: SearchRequestBuilder => Unit)(implicit c: ClassTag[T]): List[(String, T)] = {
    apply {
      implicit client =>
        client.findAsList[T](config)(f)
    }
  }

  def list[T](f: SearchRequestBuilder => Unit)(implicit c: ClassTag[T]): List[T] = {
    try {
      apply {
        implicit client =>
          client.list[T](config)(f).list.map(_.doc)
      }
    } catch {
      case ex: Exception =>
        Logger.error("No Index", ex)
        List[T]()
    }
  }

  def listExtended[T](f: SearchRequestBuilder => Unit)(implicit c: ClassTag[T]): List[ESSearchResultItem[T]] = {
    try {
      apply {
        implicit client =>
          client.list[T](config)(f).list
      }
    } catch {
      case ex: Exception =>
        Logger.error("No Index", ex)
        List[ESSearchResultItem[T]]()
    }
  }

  def findAll[T: ClassTag](): List[T] = {
    list[T] {
      searcher =>
        searcher.setQuery(matchAllQuery)
        searcher.setSize(10000)
    }
  }

  def findAllExtended[T: ClassTag](): List[ESSearchResultItem[T]] = {
    listExtended[T] {
      searcher =>
        searcher.setQuery(matchAllQuery)
        searcher.setSize(10000)
    }
  }

  def findById[T: ClassTag](id: String): T = {
    find[T] {
      searcher =>
        searcher.setQuery(termQuery("_id", id))
    }.get._2
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

object LectureDA extends DatabaseService[Lecture] {

  def findUids(uids: List[String]): List[Lecture] = {
    list[Lecture] {
      searcher =>
        searcher.setQuery(inQuery("uuid", uids.toArray: _*))
        searcher.setSize(10000)
    }
  }

}

object ScheduleDA extends DatabaseService[Schedule] {

}

object GroupDA extends DatabaseService[Group] {

  def findByCourseNames(courseNames: List[String]): List[Group] = {
    list[Group] {
      searcher =>
        searcher.setQuery(inQuery("className", courseNames.toArray: _*))
        searcher.setSize(10000)
    }
  }

}