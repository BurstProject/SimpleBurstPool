package simple_burst_pool.model

import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.DefaultJsonProtocol

object BlockchainStatusJsonProtocol extends DefaultJsonProtocol {
  implicit val blockchainStatusFormat = jsonFormat1(BlockchainStatus)
}

case class BlockchainStatus(lastBlock: String)