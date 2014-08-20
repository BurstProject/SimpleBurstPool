package simple_burst_pool

import akka.actor._
import scala.util._
import org.squeryl.PrimitiveTypeMode._
import spray.client.pipelining._
import spray.http._
import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

import nxt.util.Convert

import model._

case class CheckGenerator()

class LastBlockChecker extends Actor with ActorLogging {
  import context.dispatcher
  import BlockchainStatusJsonProtocol._
  import BlockInfoJsonProtocol._
  
  val pipeline = sendReceive
  
  def receive = {
    case CheckGenerator() => {
      try {
        val response = pipeline {
          Get(Config.walletAddress + "/burst?requestType=getBlockchainStatus")
        }
        response onComplete {
          case Success(r: HttpResponse) => {
            val block = r.entity.asString.parseJson.convertTo[BlockchainStatus].lastBlock
            log.debug(s"last block: $block")
            val response2 = pipeline {
              Get(Config.walletAddress + "/burst?requestType=getBlock&block=" + block)
            }
            response2 onComplete {
              case Success(r: HttpResponse) => {
                val blockInfo = r.entity.asString.parseJson.convertTo[BlockInfo]
                if(Global.users.contains(Convert.parseUnsignedLong(blockInfo.generator))) {
                  Global.workPayor ! ProcessPayout(Convert.parseUnsignedLong(blockInfo.generator), blockInfo.blockReward.toLong, blockInfo.totalFeeNQT.toLong)
                  log.info(blockInfo.generator + " found block")
                  try {
                    transaction {
                      PoolDb.blocks.insert(new Block(blockInfo.height))
                    }
                    log.debug("Inserted block " + blockInfo.height + " into db")
                  }
                  catch {
                    case e: Exception => log.error(e.toString)
                  }
                }
              }
              case Failure(error) => log.error(error.toString())
            }
          }
          case Failure(error) => log.error(error.toString())
        }
      }
      catch {
        case e: Exception => log.error(e.toString())
      }
    }
    case _ =>
  }

}