package helpers

import javax.inject.{Inject, Singleton}

import play.api.cache.{CacheApi, _}
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
  * @author fabian 
  *         on 23.04.16.
  */
trait CacheHelper {

  protected var cache: CacheApi = null
  private val keys = scala.collection.mutable.HashSet[String]()

  /**
    * Set a value into the cache.
    *
    * @param key        Item key.
    * @param value      Item value.
    * @param expiration Expiration time.
    */
  def set(key: String, value: Any, expiration: Duration = Duration.Inf) = {
    keys += key
    cache.set(key, value, expiration)
  }

  /**
    * Retrieve a value from the cache for the given type
    *
    * @param key Item key.
    * @return result as Option[T]
    */
  def get[T: ClassTag](key: String): Option[T] = {
    cache.get(key)
  }

  /**
    * Remove a value from the cache
    */
  def remove(key: String): Unit = {
    cache.remove(key)
    keys -= key
  }

  /**
    * Retrieve a value from the cache, or set it from a default function.
    *
    * @param key        Item key.
    * @param expiration expiration period in seconds.
    * @param orElse     The default function to invoke if the value was not found in cache.
    */
  def getOrElse[A: ClassTag](key: String, expiration: Duration = Duration.Inf)(orElse: => A): A = {
    cache.getOrElse(key, expiration)(orElse)
  }

  /** this will clear the whole session cache */
  def clear() = {
    keys.foreach(cache.remove)
    keys.clear()
  }

  /** cache http responses */
  def cached(key: String, expiration: Duration = 4 hours)(response: => Action[AnyContent]) = {
    getOrElse(key) {
      val result = response()
      set(key, result, expiration)
      result
    }
  }

}

@Singleton
class SemesterModeCache @Inject()(@NamedCache("semester-mode-cache") semesterModeCache: CacheApi) extends CacheHelper {
  cache = semesterModeCache
}

@Singleton
class SessionCache @Inject()(@NamedCache("session-cache") sessionCache: CacheApi) extends CacheHelper {
  cache = sessionCache
}