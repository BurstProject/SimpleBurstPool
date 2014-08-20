package simple_burst_pool

import akka.actor._
import scala.util._
import scala.concurrent.duration._
import spray.client.pipelining._
import spray.http._
import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

import model._

case class StateTick()

class StateChecker extends Actor with ActorLogging{
	import context.dispatcher
	
	import MineInfoJsonProtocol._
	val pipeline = sendReceive
	
	def receive = {
	  case StateTick() => {
	    try {
	      val response = pipeline {
	        Get(Config.walletAddress + "/burst?requestType=getMiningInfo")
	      }
	      response onComplete {
	        case Success(r: HttpResponse) => {
	          var netMineInfo = r.entity.asString.parseJson.convertTo[MineInfo]
	          var lastMineInfo = Global.miningInfo.get
	          if(netMineInfo.height != lastMineInfo.height ||
	             netMineInfo.generationSignature != lastMineInfo.generationSignature ||
	             netMineInfo.baseTarget != lastMineInfo.baseTarget) {
	            Global.workSubmitter ! NewHeight(netMineInfo.height.toLong)
	            Global.miningInfo.send(MineInfo(netMineInfo.height,
	        		  				 		    netMineInfo.generationSignature,
	        		  				 		    netMineInfo.baseTarget,
	        		  				 		    Some(Config.targetDeadline.toString)))
	        	log.debug("Detected new block " + netMineInfo.height)
	        	Global.lastBlockChecker ! CheckGenerator()
	          }
	        }
	        case Failure(error) =>
	          log.error(error.toString())
	      }
	    }
	    catch {
	      case e: Exception => log.error(e.toString())
	    }
	  }
	  case _ =>
	}
	
	override def preStart {
	  context.system.scheduler.schedule(0 seconds, 3 seconds, self, StateTick())
	}
}