import java.io.File

import akka.http.scaladsl.server.Directives
import com.typesafe.config.ConfigFactory
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import scala.collection.JavaConverters._

class Word2VecService() extends Directives {

  private val config = ConfigFactory.load()

  private val model = new File(config.getString("model"))
  println(s"loading model: $model")
  val vec = WordVectorSerializer.readWord2VecModel(model)

  println("model loaded ...")

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
      }
  }
}
