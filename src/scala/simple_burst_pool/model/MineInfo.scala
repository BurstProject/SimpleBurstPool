package simple_burst_pool.model

import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.DefaultJsonProtocol

object MineInfoJsonProtocol extends DefaultJsonProtocol {
  implicit val mineInfoFormat = jsonFormat4(MineInfo)
}

case class MineInfo(height: String, generationSignature: String, baseTarget: String, targetDeadline: Option[String])
