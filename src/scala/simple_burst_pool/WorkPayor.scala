package simple_burst_pool

import akka.actor._
import scala.util._
import org.squeryl.PrimitiveTypeMode._
import scala.collection.concurrent.TrieMap
import spray.client.pipelining._
import spray.http._
import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

import model._

case class AddShare(address: Long, nonce: Long, height: Long, ip: String)
case class ProcessPayout(address: Long, reward: Long, fee: Long)

class WorkPayor extends Actor with ActorLogging {
  import context.dispatcher
  val pipeline = sendReceive
  def receive = {
    case AddShare(address, nonce, height, ip) => {
      if(!Global.shares.contains((address, nonce, height))) {
        try {
          transaction {
            PoolDb.shares.insert(new Share(height, address, nonce))
          }
          Global.shares += (address, nonce, height) -> ()
          val count = Global.shareCount.getOrElseUpdate(address, 0)
          Global.shareCount(address) = count + 1
        }
        catch {
          case e: Exception => log.error(s"Failed to save share $address $nonce $height")
        }
      }
      else {
        Global.addAbuse(ip, 10)
        log.info(s"Dropped duplicate share")
      }
    }
    case ProcessPayout(address, reward, fee) => payout(address, reward, fee)
    case _ =>
  }
  
  val oneBurst = 100000000
  def payout(address: Long, reward: Long, fee: Long) {
    val rewardTotal = reward * oneBurst + fee
    val toPayOut = reward * oneBurst * (100 - Config.fee) / 100
    val payoutShares = Global.shareCount
    Global.shareCount = TrieMap[Long, Int]() // reset
    Global.shares = TrieMap[(Long, Long, Long), Unit]()
    val numShares = payoutShares.reduce((a, b) => (0, a._2 + b._2))._2
    val perShare = toPayOut / numShares
    try {
      payoutShares foreach { case (addr: Long, num: Int) =>
        sendPayment(Config.passphrase + Global.users(address),
        			Global.users(addr),
        			num * perShare)
      }
      sendPayment(Config.passphrase + Global.users(address),
    		  	  Config.feeAddress,
    		  	  rewardTotal - (numShares * perShare))
    }
    catch {
      case e: Exception => log.error("Payout error: " + e.toString())
    }
  }
  
  def sendPayment(passphrase: String, target: String, amount: Long) {
    val response = pipeline {
      val data = FormData(List(("requestType", "sendMoney"),
        		  			   ("secretPhrase", passphrase),
        		  			   ("recipient", target ),
        		  			   ("amountNQT", (amount - oneBurst).toString),
        		  			   ("feeNQT", oneBurst.toString),
        		  			   ("deadline", "1440")
        		  			))
      log.debug("Sending: " + data.toString())
      Post(Config.walletAddress + "/burst", data)
    }
    response onComplete {
      case Success(r: HttpResponse) => {
        log.info(r.entity.toString())
      }
      case Failure(error) => log.error(error.toString())
    }
  }
}