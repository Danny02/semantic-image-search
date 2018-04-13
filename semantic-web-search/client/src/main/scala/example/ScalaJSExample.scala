package example

import monix.eval.Task
import monix.execution.{Ack, Cancelable}
import monix.execution.Scheduler.Implicits.global
import monix.execution.cancelables.SingleAssignmentCancelable
import monix.reactive.Observable
import monix.reactive.OverflowStrategy.Unbounded
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.raw.{Event, EventTarget, KeyboardEvent}
import scalatags.JsDom.all._
import scalatags.JsDom.tags2._
import shared.component.Components
import shared.component.PageIds._

import scala.concurrent.duration.DurationInt

object ScalaJSExample {

  def eventListener(target: EventTarget, event: String): Observable[Event] =
    Observable.create(Unbounded) { subscriber =>
      val c = SingleAssignmentCancelable()
      // Forced conversion, otherwise canceling will not work!
      val f: Function1[Event, Ack] =
        (e: Event) => subscriber.onNext(e).syncOnStopOrFailure(_ => c.cancel())

      target.addEventListener(event, f)
      c := Cancelable(() => target.removeEventListener(event, f))
    }

  val enterPress = (ev: KeyboardEvent) => {
    if (ev.keyCode == KeyCode.Enter) {
      ev.target match {
        case input: dom.html.Input => {
          for {
            _ <- listSearchResults(input.value)
          } {}
        }
        case _ => {}
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val searchInput = input(cls := "form-control", onkeypress := enterPress).render

    val searchButton = button(
      cls := "btn btn-primary",
      onclick := { () =>
        for {
          _ <- listSearchResults(searchInput.value)
        } {}
      }
    )("Search")

    val searchSection = section(
      div(cls := "form-group")(searchInput),
      searchButton
    )

    val entrypoint = dom.document.getElementById(predSearchId)
    entrypoint.appendChild(searchSection.render)

    eventListener(searchInput, "onkeypress")
      .debounce(1 second)
      .switchMap(e => Observable.fromTask(listSearchResults(searchInput.value)))
  }

  def listSearchResults(word: String): Task[_] = {

    object JsDomComponents extends Components(scalatags.JsDom)
    import JsDomComponents._

    val predList = dom.document.getElementById(predListId)

    SearchApi.searchReq(word).map { preds =>
      predList.innerHTML = ""
      val predNodes = preds.map(_.render)
      predList.appendChild(scalatags.JsDom.all.SeqFrag(predNodes).render)
    }
  }

}
