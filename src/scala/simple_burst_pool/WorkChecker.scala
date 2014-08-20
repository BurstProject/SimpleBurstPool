package simple_burst_pool

import akka.actor._

import java.nio.ByteBuffer
import java.math.BigInteger

import fr.cryptohash.Shabal256

import org.squeryl.PrimitiveTypeMode._

import nxt.util.Convert
import nxt.util.MiningPlot

case class MineShare(address: Long, nonce: Long, height: Long, ip: String)

class WorkChecker extends Actor with ActorLogging {
	def receive = {
	  case MineShare(_, _, _, ip) if Global.isBanned(ip) => {
	    log.info("Dropped share from banned ip")
	  }
	  case MineShare(addr, nonce, height, ip) => {
	    val curHeight = Global.miningInfo.get.height
	    if(height == curHeight.toInt) {
	      val deadline = calculateDeadline(addr, nonce)
	      if(deadline.compareTo(BigInteger.valueOf(Config.targetDeadline )) <= 0) {
	        Global.workPayor ! AddShare(addr, nonce, height, ip)
	        Global.workSubmitter ! SubmitWork(height, addr, nonce, deadline.longValue())
	      }
	      else {
	        Global.addAbuse(ip, 20)
	        log.info(s"Rejected invalid share")
	      }
	    }
	    else {
	      Global.addAbuse(ip, 5)
	      log.info("Received stale share")
	    }
	  }
	  case _ =>
	}
	
	val md = new Shabal256
	
	var scoopNum = 0
	var scoopNumHeight = 0L
	
	def calculateDeadline(address: Long, nonce: Long) = {
	  val info = Global.miningInfo.get
	  val plot = new MiningPlot(address, nonce)
	  if(info.height.toLong != scoopNumHeight) {
	    calculateScoopNum
	  }
	  md.reset()
	  md.update(Convert.parseHexString(info.generationSignature ))
	  plot.hashScoop(md, scoopNum)
	  val hash = md.digest()
	  val hit = new BigInteger(1, Array[Byte](hash(7), hash(6), hash(5), hash(4), hash(3), hash(2), hash(1), hash(0)))
	  hit.divide(BigInteger.valueOf(info.baseTarget.toLong))
	}
	
	def calculateScoopNum {
	  val info = Global.miningInfo.get
	  val scoopBuffer = ByteBuffer.allocate(32 + 8)
	  scoopBuffer.put(Convert.parseHexString(info.generationSignature))
	  scoopBuffer.putLong(Convert.parseUnsignedLong(info.height))
	  md.reset()
	  md.update(scoopBuffer.array())
	  val scoopHash = new BigInteger(1, md.digest())
	  scoopNum = scoopHash.mod(BigInteger.valueOf(MiningPlot.SCOOPS_PER_PLOT)).intValue()
	  scoopNumHeight = info.height.toLong 
	}
}