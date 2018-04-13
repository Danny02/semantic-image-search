package example

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import shared.SharedMessages

class WebIndexService() extends Directives {

  val route = {
    pathSingleSlash {
      get {
        complete {
          HttpEntity(ContentTypes.`text/html(UTF-8)`, Pages.index())
        }
      }
    } ~
      pathPrefix("assets" / Remaining) { file =>
        // optionally compresses the response with Gzip or Deflate
        // if the client accepts compressed responses
        encodeResponse {
          getFromResource("public/" + file)
        }
      }
  }
}
