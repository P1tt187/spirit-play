package helpers

import javax.inject._

import akka.actor.ActorRef
import model.database.SemesterModeDA
import model.schedule.meta.EMode._
import model.schedule.meta.{EMode, SemesterMode}
import play.api.{Configuration, Environment, Logger}

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import logic.actors.database.DatabaseActor.GiveSemesterMode

/**
  * @author fabian 
  *         on 03.04.16.
  *         Helper class
  */
trait SpiritHelper {

  /** play config  */
  @Inject()
  protected var configuration: Configuration = null

  /** play enviroment */
  @Inject()
  protected var env: Environment = null

  /** which semester is it */
  @Inject()
  protected var semesterModeCache: SemesterModeCache = null

  @Inject()
  protected var sessionCache: SessionCache = null

  @Inject()
  @Named("databaseActor")
  protected var databaseActor:ActorRef = null

  /** current semester filtered by semestermode */
  protected def courseNames: List[(String, List[String])] = {
    semesterModeCache.getOrElse("courselist", 1 day) {

      val courseNames = configuration.getStringList("courses.courseNames").get
      val semesters: Option[Seq[java.lang.Long]] = configuration.getLongSeq("courses.semesters")
      /** bai1, bai2.... */
      val courseSemesters = courseNames.flatMap {
        cn =>
          val idx = courseNames.indexOf(cn)
          val sem: Seq[Long] = semesters.getOrElse(Seq[java.lang.Long]()).map(_.toLong)

          (1l to sem.lift(idx).getOrElse(1l)).map {
            ind =>
              cn + ind
          }
      }.sorted

      val result = courseNames.sorted.map {
        cn =>
          (cn, filterCourses(courseSemesters.filter(cs => cs.contains(cn) && cs.length - 1 == cn.length).toList).sorted)
      }.toList
      semesterModeCache.set("courselist", result)
      result
    }
  }

  private def filterCourses(courses: List[String]) = {

    val mode = getSemesterMode()

    val semesterNumbers = 1 to 20

    val filteredNumbers = if (mode == SUMMER) {
      semesterNumbers.filter(_ % 2 == 0)
    } else if (mode == WINTER) {
      semesterNumbers.filter(_ % 2 != 0)
    } else {
      semesterNumbers
    }

    courses.filter {
      c =>
        filteredNumbers.exists(fn => c.contains(fn.toString))
    }

  }

  /** returns the semestermode
    *
    * @return
    * can be summer or winter */
  def getSemesterMode() = {
    semesterModeCache.getOrElse("mode", 1 day) {
      implicit val timeout = Timeout(30 seconds)
      val mode = Await.result(databaseActor ? GiveSemesterMode, 30 seconds).asInstanceOf[SemesterMode]
      Logger.debug(mode.toString)
      semesterModeCache.set("mode", mode)
      EMode.valueOf(mode.mode)
    }
  }

  /** returns all courses */
  def uncachedCourseNames = {

    val courseNames = configuration.getStringList("courses.courseNames").get
    val semesters: Option[Seq[java.lang.Long]] = configuration.getLongSeq("courses.semesters")

    /** bai1, bai2.... */
    (courseNames.flatMap {
      cn =>
        val idx = courseNames.indexOf(cn)
        val sem: Seq[Long] = semesters.getOrElse(Seq[java.lang.Long]()).map(_.toLong)

        (1l to sem.lift(idx).getOrElse(1l)).map {
          ind =>
            cn + ind
        }
    } ++ courseNames).toList.sorted
  }

}
