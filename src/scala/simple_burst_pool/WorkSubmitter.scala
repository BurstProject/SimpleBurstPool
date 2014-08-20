package simple_burst_pool

import akka.actor._
import akka.agent.Agent
import scala.util._
import scala.util.control.NonFatal
import spray.client.pipelining._
import spray.http._
import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

import model._

import nxt.util.Convert

case class NewHeight(height: Long)
case class SubmitWork(height: Long, address: Long, nonce: Long, deadline: Long)

case class SubmitState(height: Long, lastDeadline: Long)

class WorkSubmitter extends Actor with ActorLogging{
	import context.dispatcher
	import SubmitResultJsonProtocol._
	val pipeline = sendReceive
	
	val state = Agent(SubmitState(0L, 999999999L))
	
	def receive = {
	  case SubmitWork(height, address, nonce, deadline) => {
	    val curState = state.get
	    if(height == curState.height  && deadline < curState.lastDeadline) {
	      try {
	        val secret = Config.passphrase + Global.users.get(address).get 
	        val response = pipeline {
	          val data = FormData(List(("requestType", "submitNonce"),
	        		  					("secretPhrase", secret),
	        		  					("nonce", Convert.toUnsignedLong(nonce))))
	          Post(Config.walletAddress + "/burst", data)
	        }
	        response onComplete {
	          case Success(r: HttpResponse) => {
	              val result = r.entity.asString.parseJson.convertTo[SubmitResult]
	              result.deadline match {
	        	    case Some(receivedDeadline) => {
	        	      if(deadline == receivedDeadline) {
	        	        log.debug("Sucessfully submitted nonce with deadline " + deadline)
	        	        state.send(s => {
	        	          if(height == s.height && deadline < s.lastDeadline) {
	        	            SubmitState(height, deadline)
	        	          }
	        	          else {
	        	            s
	        	          }
	        	        })
	        	      }
	        	      else {
	        	        log.error("Submitted " + Convert.toUnsignedLong(address) + ":" +
	        	        		  Convert.toUnsignedLong(nonce) + " and received deadline " +
	        	        		  receivedDeadline + " expected " + deadline)
	        	      }
	        	    }
	        	    case None => {
	        	      log.error("Failed to submit. Message: " + result.result)
	        	    }
	        	  }
	          }
	          case Failure(error) => log.error(error.toString())
	        }
	      }
	      catch {
	        case e: Exception => log.error(e.toString)
	      }
	    }
	  }
	  case NewHeight(height) => {
	    state.send(SubmitState(height, 999999999L))
	  }
	  case _ =>
	}
}