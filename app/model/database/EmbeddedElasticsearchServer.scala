package model.database

import java.io.{File, IOException}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.node
import org.elasticsearch.node.Node



/**
  * @author fabian 
  *         on 26.01.17.
  */

object EmbeddedElasticsearchServer {


  val DEFAULT_DATA_DIRECTORY = "data"

  val node : Node = {
    import org.elasticsearch.node.NodeBuilder._

    val dataDir = new File(EmbeddedElasticsearchServer.DEFAULT_DATA_DIRECTORY)

    if (!dataDir.exists()) {
      dataDir.mkdirs()
    }
    val elasticSearchSettings = org.elasticsearch.common.settings.Settings.builder()
      .put("path.data", EmbeddedElasticsearchServer.DEFAULT_DATA_DIRECTORY)
        .put("path.home",EmbeddedElasticsearchServer.DEFAULT_DATA_DIRECTORY)

      .build()
    nodeBuilder().local(true).settings(elasticSearchSettings).build()
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
