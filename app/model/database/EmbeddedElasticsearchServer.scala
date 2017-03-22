package model.database

import java.io.{File, IOException}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.node.Node
import org.elasticsearch.node.internal.InternalSettingsPreparer
import org.elasticsearch.plugins.Plugin
import org.elasticsearch.transport.Netty4Plugin

import scala.collection.JavaConversions._


/**
  * @author fabian 
  *         on 26.01.17.
  */

object EmbeddedElasticsearchServer {

  private class MyNode(preparedSetings: Settings, classpathPlugins:java.util.Collection[Class[_ <: Plugin]]) extends Node(InternalSettingsPreparer.prepareEnvironment(preparedSetings, null), classpathPlugins)

  val DEFAULT_DATA_DIRECTORY = "data"

  val node : Node = {


    val dataDir = new File(EmbeddedElasticsearchServer.DEFAULT_DATA_DIRECTORY)

    if (!dataDir.exists()) {
      dataDir.mkdirs()
    }
    new MyNode(Settings.builder()
      .put("path.data", EmbeddedElasticsearchServer.DEFAULT_DATA_DIRECTORY)
        .put("path.home",EmbeddedElasticsearchServer.DEFAULT_DATA_DIRECTORY)
      .put("transport.type", "netty4")
      .put("http.type", "netty4")
      .put("http.enabled", "true")
      .build(),
      List(classOf[Netty4Plugin])
    )


  }

  def start(): Node = {
    node.start()
  }

  def shutDown(): Unit = {
    node.close()
    deleteDataDirectory()
  }

  private def deleteDataDirectory(): Unit = {
    val directory = Paths.get(EmbeddedElasticsearchServer.DEFAULT_DATA_DIRECTORY)
    Files.walkFileTree(directory, new SimpleFileVisitor[Path] {

      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        Files.delete(dir)
        FileVisitResult.CONTINUE
      }

    })
  }

}
