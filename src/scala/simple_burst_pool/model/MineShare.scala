package simple_burst_pool.model

import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.DefaultJsonProtocol

object MineShareJsonProtocol extends DefaultJsonProtocol {
  implicit val mineShareFormat = jsonFormat3(MineShare)
}

case class MineShare(address: String, nonce: String, height: Int)

/*object MineShareContainerJsonProtocol extends DefaultJsonProtocol {
  implicit def containerWriter = jsonFormat1(MineShareContainer)
}*/

case class MineShareContainer[A](shares: List[MineShare])