package example

import monix.eval.Task
import monix.execution.{Ack, Cancelable}
import monix.execution.Scheduler.Implicits.global
import monix.execution.cancelables.SingleAssignCancelable
import monix.reactive.Observable
import monix.reactive.OverflowStrategy.Unbounded
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.raw.{Event, EventTarget, KeyboardEvent}
import scalatags.JsDom.all.{SeqFrag => SFrag, _}
import scalatags.JsDom.tags2._
import shared.component.Components
import shared.component.PageIds._
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration.DurationDouble

object JsDomComponents extends Components(scalatags.JsDom)
import JsDomComponents._

object ScalaJSExample {

  def main(args: Array[String]): Unit = {
    val searchInput = input(cls := "form-control", placeholder := "e.g. dog, pizza or beach").render

    val searchSection = section(
      div(cls := "form-group")(searchInput)
    )

    val entrypoint = dom.document.getElementById(predSearchId)
    entrypoint.appendChild(searchSection.render)

    eventListener(searchInput, "keypress")
      .debounce(1 second)
      .switchMap(e => Observable.fromTask(listSearchResults(searchInput.value)))
      .subscribe()
  }

  def listSearchResults(word: String): Task[_] = {
    val predList = dom.document.getElementById(predListId)

    SearchApi.searchReq(word).map { preds =>
      predList.innerHTML = ""
      val predNodes = preds match {
        case Nil => p("no similar images!")
        case _   => SFrag(preds)
      }
      predList.appendChild(predNodes.render)
    }
  }

  def eventListener(target: EventTarget, event: String): Observable[Event] =
    Observable.create(Unbounded) { subscriber =>
      val c = SingleAssignCancelable()
      // Forced conversion, otherwise canceling will not work!
      val f: scala.scalajs.js.Function1[Event, Ack] =
        (e: Event) => {
          dom.console.log(e)
          subscriber.onNext(e).syncOnStopOrFailure(_ => c.cancel())
        }

      target.addEventListener(event, f)
      c := Cancelable(() => target.removeEventListener(event, f))
    }
}
