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
  implicit def obsFrag[T <% Frag](obs: Observable[T]): Frag = {
    val first = span().render
    obs
      .scan(first) { (last, frag) =>
        val next = span(frag).render
        last.parentNode.replaceChild(next, last)
        next
      }
      .subscribe()
    first
  }

  def main(args: Array[String]): Unit = {
    val searchInput = input(cls := "form-control", placeholder := "e.g. dog, pizza or beach").render

    val predsObs = eventListener(searchInput, "keypress")
      .debounce(1 second)
      .map(_ => searchInput.value)
      .map(SearchApi.searchReq)
      .switchMap(Observable.fromTask)

    val searchSection = section(
      div(cls := "form-group")(
        searchInput,
        predsObs.map { preds =>
          div(
            h2("Predictions"),
            preds match {
              case Nil => p("no similar images!")
              case _   => SFrag(preds)
            }
          )
        }
      )
    )

    val entrypoint = dom.document.getElementById(predSearchId)
    entrypoint.appendChild(searchSection.render)
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
