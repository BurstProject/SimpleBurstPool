package simple_burst_pool



import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.Logging
import akka.actor.Props
import spray.http._
import spray.httpx._
import spray.util._
import spray.routing._
import Directives._
import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._
import org.squeryl.PrimitiveTypeMode._
import nxt.util.Convert
import nxt.crypto.Crypto
import model._
import akka.event.LoggingAdapter

case class RegisterAddress(address: String)
case class SubmitNonce(address: String, nonce: String)

class PoolServiceActor extends Actor with PoolService {
	def actorRefFactory = context
	log = Logging(context.system, this)
	def receive = runRoute(route)
}

trait PoolService extends HttpService {
    import MineInfoJsonProtocol._
    import MineShareJsonProtocol._
    
    var log: LoggingAdapter = null
    
	lazy val route =
	  path("") {
	    get {
	      getFromFile("webroot/index.html")
	    }
	  } ~
	  path("register") {
	    post {
	      clientIP { ip =>
	        formFields('payoutAddress) { (payoutAddress) =>
	          complete {
	            registerAddress(payoutAddress, ip.toString) match {
	              case InvalidAddress() => "invalid burst address"
	              case ExistingAddress(addr) => s"generate address is $addr"
	              case NewAddress(addr) => s"new generate address is $addr"
	              case RateControl() => "registration rate limit exceeded"
	            }
	          }
	        }
	      }
	    }
	  } ~
	  pathPrefix("pool") {
	    path("getMiningInfo") {
	      get {
	        complete {
	          Global.miningInfo.get
	        }
	      }
	    } ~
	    path("submitWork") {
	      post {
	        clientIP { ip =>
	          requestInstance { req =>
	            complete {
	              val work = req.entity.asString
	              Global.workSplitter ! Work(work, ip.toString)
	              "Received share/s"
	            }
	          }
	        }
	      }
	    }
	  } ~
	  path("shares") {
	    get {
	      complete {
	        Global.shareCount.foldLeft("")((p, m) =>
	          p + Convert.toUnsignedLong(m._1) + " " + m._2 + "\n"
	        )
	      }
	    }
	  } ~
	  get {
	    getFromDirectory("webroot/")
	  }
	  
	  sealed abstract class RegistrationResult
	  case class InvalidAddress() extends RegistrationResult
	  case class ExistingAddress(address: String) extends RegistrationResult
	  case class NewAddress(address: String) extends RegistrationResult
	  case class RegistrationFailed() extends RegistrationResult
	  case class RateControl() extends RegistrationResult
	  val AddressFormat = "BURST-(.{4})-(.{4})-(.{4})-(.{5})".r
	  def registerAddress(payout: String, ip: String): RegistrationResult = payout match {
	    case AddressFormat(p1, p2, p3, p4) => {
	      val userAddress = s"BURST-$p1-$p2-$p3-$p4"
	      Global.users.find(_._2 == userAddress) match {
	        case Some((genAddress, _)) => ExistingAddress(Convert.toUnsignedLong(genAddress))
	        case None => {
	          if(System.currentTimeMillis() - Global.lastRegistered.getOrElse(ip, 0L) < 600000) {
	            return RateControl()
	          }
	          val publicKey = Crypto.getPublicKey(Config.passphrase + userAddress) 
	          val publicKeyHash = Crypto.sha256().digest(publicKey)
	          val id = Convert.fullHashToId(publicKeyHash)
	          try {
	            transaction {
	              PoolDb.users.insert(new User(id, userAddress))
	              Global.users += id.toLong -> userAddress
	            }
	            Global.lastRegistered += ip -> System.currentTimeMillis()
	          }
	          catch {
	            case e: Exception => {
	              System.out.println(e)
	              return RegistrationFailed()
	            }
	          }
	          NewAddress(Convert.toUnsignedLong(id))
	        }
	      }
	    }
	    case _ => InvalidAddress()
	  }
}