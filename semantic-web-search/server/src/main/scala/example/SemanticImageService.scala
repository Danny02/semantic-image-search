package example

import java.io.File
import java.nio.file.{Files, Path, Paths}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.server.Directives
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.iptc.IptcDirectory
import com.typesafe.config.ConfigFactory
import monix.eval.Task
import monix.execution.Scheduler
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import shared.component.TagPrediction
import spray.json.DefaultJsonProtocol

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val predFormat = jsonFormat3(TagPrediction)
}

case class ImagesIndex(imgTags: Map[Path, List[String]],
                       reverse: Map[String, List[Path]],
                       tagFreq: Map[String, Int])

class SemanticImageService() extends Directives with JsonSupport {

  private val ioScheduler = Scheduler.io()

  private val config = ConfigFactory.load().getConfig("word2vec")

  val imagesDir: Path = Paths.get(config.getString("images"))

  // build processing Tasks and trigger them directly
  val index: Task[ImagesIndex] = Task.evalOnce(loadImagesIndex(imagesDir)).executeOn(ioScheduler)
  index.runAsync(Scheduler.global)

  val word2vec = Task
    .evalOnce {
      val model = new File(config.getString("model"))
      println(s"loading model: $model")
      val v = WordVectorSerializer.readWord2VecModel(model)
      println("model loaded ...")
      v
    }
    .executeOn(ioScheduler)
  word2vec.runAsync(Scheduler.global)

  // support for akka-http
  private implicit def taskMarshaller[A, B](implicit m: Marshaller[A, B]): Marshaller[Task[A], B] =
    Marshaller(implicit ec ⇒ _.runAsync(Scheduler(ec)).flatMap(m(_)))

  val route = {
    pathPrefix("nearest") {
      parameter('word, 'count.as[Int]) { (word, count) =>
        val nearest = word2vec.map(_.wordsNearest(word, count).asScala)
        complete(nearest)
      }
    } ~
      pathPrefix("search") {
        parameter('q) { word =>
          complete(findRelativeImages(word))
        }
      } ~
      pathPrefix("images" / Remaining) { file =>
        // optionally compresses the response with Gzip or Deflate
        // if the client accepts compressed responses
        encodeResponse {
          getFromFile(imagesDir.resolve(file).toFile)
        }
      } ~
      pathPrefix("tags") {
        get {
          complete(index.map(_.tagFreq))
        } ~
          path(Segment) { image =>
            val pathsOrEmpty = index.map { ind =>
              val maybeList = ind.imgTags.get(Paths.get(image))
              maybeList.getOrElse(List.empty[String])
            }
            complete(pathsOrEmpty)
          }
      }

  }

  val predCache = word2vec.map { v =>
    collection.mutable.Map.empty[(String, String), Double].withDefault {
      case (a, b) => v.similarity(a, b)
    }
  }.memoize

  def findRelativeImages(search: String): Task[List[TagPrediction]] = {
    for {
      ind    <- index
      pCache <- predCache
    } yield {
      ind.reverse
        .map {
          case (tag, paths) => {
            val images = paths.map(p => s"/images/${p.getFileName}")
            val pred   = tag.split(' ').map(tp => pCache(search, tp)).reduce(_ max _)
            TagPrediction(pred, tag, images)
          }
        }
        .toList
        .sortBy(1 - _.pred)
        .take(5)
        .filter(_.pred > 0.3)
    }
  }

  def loadImagesIndex(imgDir: Path): ImagesIndex = {
    val imgs = loadImageTags(imgDir)

    val reverse = imgs.toList
      .flatMap {
        case (path, tags) => tags.map((_, path))
      }
      .groupBy(_._1)
      .mapValues(_.map(_._2))

    val sortedTags = reverse.mapValues(_.size).toList.sortBy(-_._2)
    val freq       = ListMap(sortedTags: _*)

    ImagesIndex(imgs, reverse, freq)
  }

  def loadImageTags(imgDir: Path): Map[Path, List[String]] = {
    val jpgs = Files.newDirectoryStream(imgDir, "*.jpg")

    println(s"loading metadata of images in $imgDir")
    val data = try {
      val iter = jpgs.iterator().asScala
      iter.map { image =>
        try {
          var metadata = ImageMetadataReader.readMetadata(image.toFile)
          val dir      = metadata.getDirectoriesOfType(classOf[IptcDirectory]).asScala
          val tags = dir
            .flatMap(_.getTags.asScala)
            .filter(_.getTagName == "Keywords")
            .flatMap(tag => tag.getDescription.split(",|;"))
            .map(_.trim)
            .filter(!_.isEmpty)

          (image, tags.toList.distinct)
        } catch {
          case ex: Throwable => {
            println(s"failed loading metadata for $image")
            throw ex
          }
        }
      }.toMap
    } finally {
      jpgs.close();
    }

    println(s"loaded data for ${data.size} images")
    data
  }
}
