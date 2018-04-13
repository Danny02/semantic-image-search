package example

import monix.eval.Task
import org.scalajs.dom.ext.Ajax
import shared.component.TagPrediction
import upickle.default._

object SearchApi {

  private val host = "http://localhost:8080"

  implicit def tagPredR: Reader[TagPrediction] = macroR

  def searchReq(word: String) = request[Seq[TagPrediction]](s"$host/search?q=$word")

  def tagReq(image: String) = request[Seq[String]](s"$host/tag/$image")

  def request[T: Reader](url: String) = {
    Task
      .deferFuture(Ajax.get(url))
      .map(r => read[T](r.responseText))
  }
}
