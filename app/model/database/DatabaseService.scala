package model.database

import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
import com.sksamuel.elastic4s.playjson._
import com.sksamuel.elastic4s.ElasticDsl._
import controllers.CourseName
import model.news.{LatestNumber, NewsEntry}
import model.schedule.data.{Group, Lecture, Schedule}
import model.schedule.meta.{ScheduleDate, SemesterMode}
import org.elasticsearch.action.search.SearchRequestBuilder
import play.api.Logger

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.embedded.LocalNode
/**
  * @author fabian 
  *         on 28.03.16.
  *         Database helper class
  */

object DatabaseService {
  val node = LocalNode("spirit-play","data")
}

sealed trait DatabaseService[A <: AnyRef] {

  val localClient = DatabaseService.node.elastic4sclient()

  /** config for database scheme */
  val config = "spirit-play" / this.getClass.getSimpleName

  def checkIndex(): Unit ={
    val result = localClient.execute{
      indexExists("spirit-play/" + this.getClass.getSimpleName )
    }
    if(!result.value.get.get.isExists){
      localClient.execute{
        createIndex("spirit-play/" + this.getClass.getSimpleName)
      }
    }
  }

  def insert(value :A ): Unit ={
    localClient.execute{
      indexInto(config).doc(value)
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

object LectureDA extends DatabaseService[Lecture] {

  def findUids(uids: List[String]): List[Lecture] = {
    list[Lecture] {
      searcher =>

        searcher.setQuery(termsQuery("uuid", uids.toArray: _*))
        searcher.setSize(10000)
    }
  }

  def findForCourse(courseName: CourseName): List[Lecture] = {
    list[Lecture] {
      searcher =>
        searcher.setQuery(matchQuery("course", courseName))
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
        searcher.setQuery(termsQuery("className", courseNames.toArray: _*))
        searcher.setSize(10000)
    }
  }

}