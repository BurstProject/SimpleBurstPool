package simple_burst_pool.model

import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.DefaultJsonProtocol

object BlockInfoJsonProtocol extends DefaultJsonProtocol {
  implicit val blockInfoFormat = jsonFormat4(BlockInfo)
}

case class BlockInfo(generator: String, blockReward: String, totalFeeNQT: String, height: Long)
