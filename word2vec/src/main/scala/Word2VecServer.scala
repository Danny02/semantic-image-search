import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

object Word2VecServer extends App {
  implicit val system = ActorSystem("server-system")
  implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  val service = new Word2VecService()

  Http().bindAndHandle(service.route, interface, port)

  println(s"Server online at http://$interface:$port")
}
