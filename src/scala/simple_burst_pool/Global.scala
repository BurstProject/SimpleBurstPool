package simple_burst_pool

import scala.concurrent.ExecutionContext.Implicits.global
import akka.agent.Agent
import model._
import akka.actor.ActorRef
import scala.collection.concurrent.TrieMap

object Global {
	var workChecker: ActorRef = null
	var workSplitter: ActorRef = null
	var workSubmitter: ActorRef = null
	var workPayor: ActorRef = null
	var lastBlockChecker: ActorRef = null
	var miningInfo = Agent(MineInfo("0", "0000000000000000000000000000000000000000000000000000000000000000", "10000000000", Some("1")))
	var users = TrieMap[Long, String]()
	var shares = new TrieMap[(Long, Long, Long), Unit]() // address, nonce, height
	var shareCount = TrieMap[Long, Int]()
	var abuse = TrieMap[String, Agent[Int]]()
	var lastRegistered = TrieMap[String, Long]()
	
	def addAbuse(ip: String, amount: Int) {
	  abuse.getOrElseUpdate(ip, Agent(0))
	  abuse(ip).send(_ + amount)
	}
	
	def isBanned(ip: String) = {
	  abuse.get(ip) match {
	    case Some(a) => a.get > Config.banThreshold 
	    case None => false
	  }
	}
}