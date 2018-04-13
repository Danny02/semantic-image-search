package example

import scalatags.Text._
import scalatags.Text.short._
import shared.component.PageIds._

object Pages {

  def index() = {
    val con = *.cls := "container"

    template("Akka HTTP with Scala.js")(
      header(con)(h1("Semantic Image Search")),
      tags2.main(*.id := mainId, con)(
        div(*.id := predSearchId),
        h2("Predictions"),
        tags2.section(*.id := predListId, *.cls := "d-flex", *.style := "flex-flow: row wrap")
      )
    )
  }

  def template(titleText: String)(content: Frag*) = {
    "<!DOCTYPE html>" + html(
      head(
        tags2.title(titleText),
        meta(attr("charset") := "utf-8"),
        meta(attr("name") := "viewport",
             attr("content") := "width=device-width, initial-scale=1, shrink-to-fit=no"), {
          link(
            *.rel := "stylesheet",
            *.href := "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta/css/bootstrap.min.css",
            attr("integrity") := "sha384-/Y6pD6FV/Vv2HJnA6t+vslU6fwYXjCFtcEpHbNJ0lyAFsXTsjBbfaDjzALeQsN6M",
            attr("crossorigin") := "anonymous"
          )
        }, {
          link(
            *.rel := "stylesheet",
            *.href := "/assets/css/main.css"
          )
        }
      ),
      body(
        content,
        raw(
          scalajs.html
            .scripts("client",
                     name => s"/assets/$name",
                     name => getClass.getResource(s"/public/$name") != null)
            .body
        )
      )
    )
  }
}
