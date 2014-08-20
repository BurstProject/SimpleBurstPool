package simple_burst_pool.model

import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.DefaultJsonProtocol

object SubmitResultJsonProtocol extends DefaultJsonProtocol {
  implicit val submitResultFormat = jsonFormat2(SubmitResult)
}

case class SubmitResult(result: String, deadline: Option[Long])
