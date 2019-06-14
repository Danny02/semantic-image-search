package example

import java.nio.file.{Files, Path, Paths}

import org.datavec.image.loader.NativeImageLoader
import org.deeplearning4j.nn.graph.ComputationGraph
import org.deeplearning4j.zoo.model.VGG19
import org.deeplearning4j.zoo.util.imagenet.ImageNetLabels
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor

import scala.collection.JavaConverters._

object MyYolo extends App {

  yolo()

  def yolo(): Unit = {
    val model = new VGG19(1, 123, 1)
    //num labels doesn't matter since we're getting pretrained imagenet
    val initializedModel = model.initPretrained.asInstanceOf[ComputationGraph]

    files("/Users/dheinrich/Projects/24sprint/images") { (img: Path) =>
      // set up input and feedforward
      val loader = new NativeImageLoader(224, 224, 3)
      val image  = loader.asMatrix(Files.newInputStream(img))
      val scaler = new VGG16ImagePreProcessor
      scaler.transform(image)
      val output = initializedModel.output(false, image)

      // check output labels of result
      val decodedLabels = new ImageNetLabels().decodePredictions(output(0))
      println(img)
      println(decodedLabels)
    }
  }

  def files(path: String)(f: Path => _) = {
    val imgDir = Paths.get(path)
    val jpgs   = Files.newDirectoryStream(imgDir, "*.jpg")

    println(s"loading metadata of images in $imgDir")
    val data = try {
      val iter = jpgs.iterator().asScala
      iter.foreach(f)
    } finally {
      jpgs.close()
    }
  }
}
