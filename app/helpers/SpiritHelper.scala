package helpers

import javax.inject._

import model.database.SemesterModeDA
import model.schedule.meta.EMode._
import model.schedule.meta.{EMode, SemesterMode}
import play.api.cache._
import play.api.{Configuration, Environment, Logger}

import scala.collection.JavaConversions._
import scala.concurrent.duration._

/**
  * @author fabian 
  *         on 03.04.16.
  */
trait SpiritHelper {

  @Inject()
  protected var configuration: Configuration = null

  @Inject()
  protected var env: Environment = null

  @Inject()
  @NamedCache("semester-mode-cache")
  protected var semesterModeCache: CacheApi = null

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

  def getSemesterMode() = {
    semesterModeCache.getOrElse("mode", 1 day) {
      val mode = SemesterModeDA.findAll[SemesterMode]().headOption match {
        case Some(mode) => mode
        case None => SemesterMode(EMode.SUMMER.name())
      }
      Logger.debug(mode.toString)
      semesterModeCache.set("mode", mode)
      EMode.valueOf(mode.mode)
    }
  }

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
