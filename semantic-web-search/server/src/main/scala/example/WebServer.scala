package example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

object WebServer extends RouteConcatenation {

  def main(args: Array[String]) {
    implicit val system       = ActorSystem("server-system")
    implicit val materializer = ActorMaterializer()

    val config    = ConfigFactory.load()
    val interface = config.getString("http.interface")
    val port      = config.getInt("http.port")

    val indexSservice   = new WebIndexService()
    val word2vecservice = new SemanticImageService()

    val routes = indexSservice.route ~ word2vecservice.route

    Http().bindAndHandle(routes, interface, port)

    println(s"Server online at http://$interface:$port")
  }
}
