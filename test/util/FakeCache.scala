package util

import play.api.cache.CacheApi
import play.cache.{CacheApi => JavaCacheApi, DefaultCacheApi => DefaultJavaCacheApi}

import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
  * @author fabian 
  *         on 16.04.16.
  */
class FakeCache extends CacheApi {
  override def set(key: String, value: Any, expiration: Duration): Unit = {}

  override def get[T](key: String)(implicit evidence$2: ClassTag[T]): Option[T] = None

  override def getOrElse[A](key: String, expiration: Duration)(orElse: => A)(implicit evidence$1: ClassTag[A]): A = orElse

  override def remove(key: String): Unit = {}
}
