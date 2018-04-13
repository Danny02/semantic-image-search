import java.io.{File, IOException}
import java.nio.file.{Files, Path, Paths}
import java.util
import java.util.Collection

import akka.http.scaladsl.server.Directives
import com.drew.imaging.{ImageMetadataReader, ImageProcessingException}
import com.drew.metadata.{Directory, Metadata, Tag}
import com.drew.metadata.iptc.IptcDirectory
import com.typesafe.config.ConfigFactory
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer

import scala.collection.JavaConverters._

class Word2VecService() extends Directives {

  private val config = ConfigFactory.load().getConfig("word2vec")


  val images = loadImageTags(Paths.get(config.getString("images")))
  val reverseIndex: Map[String, List[Path]] = {
    images.toList
      .flatMap {
        case (path, tags) => tags.map(tag => (tag, path))
      }
      .groupBy(_._1)
      .mapValues(_.map(_._2))
  }

  val vec = {
    val model = new File(config.getString("model"))
    println(s"loading model: $model")
    val v = WordVectorSerializer.readWord2VecModel(model)
    println("model loaded ...")
    v
  }

  val route = {
    pathPrefix("nearest") {
      parameter('word, 'count.as[Int]) { (word, count) =>
        val nearest = vec.wordsNearest(word, count).asScala.mkString(", ")
        complete(s"the $count nearest for '$word' are: $nearest")
      }
    } ~
      pathPrefix("diff" / Segment / Segment) { (a, b) =>
        val sim = vec.similarity(a, b)
        complete(s"'$a' is $sim similary to '$b'")
      } ~
      pathPrefix("search" / Segment)(w => complete(findImages(w)))
  }

  def findImages(tag: String): Option[String] = {
    reverseIndex.get(tag).map(_.mkString("[\n", ",\n", "]"))
  }

  def loadImageTags(imgDir: Path): Map[Path, List[String]] = {
    val jpgs   = Files.newDirectoryStream(imgDir, "*.jpg")

    println(s"loading metadata of images in $imgDir")
    val data = try {
      val iter = jpgs.iterator().asScala
      iter.map { image =>
        var metadata = ImageMetadataReader.readMetadata(image.toFile)
        val dir      = metadata.getDirectoriesOfType(classOf[IptcDirectory]).asScala
        val tags = dir
          .flatMap(_.getTags.asScala)
          .filter(_.getTagName == "Keywords")
          .flatMap(tag => tag.getDescription.split(','))
          .map(_.trim)

        (image, tags.toList)
      }.toMap
    } finally {
      jpgs.close();
    }
    println(s"loaded data for ${data.size} images")
    data
  }
}
