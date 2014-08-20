package simple_burst_pool

import akka.actor._
import scala.util._
import scala.concurrent.duration._

case class UnbanCycle()

class Unbanner extends Actor with ActorLogging {
    import context.dispatcher
	def receive = {
	  case UnbanCycle() => {
	    Global.abuse.keys foreach (Global.abuse(_) send 0)
	    log.debug("Ran unban cycle")
	  }
	  case _ =>
	}
	
	override def preStart {
	  context.system.scheduler.schedule(0 seconds, 10 minutes, self, UnbanCycle())
	}
}