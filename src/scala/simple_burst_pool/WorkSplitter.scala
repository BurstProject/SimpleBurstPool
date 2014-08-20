package simple_burst_pool

import akka.actor._

import nxt.util.Convert

case class Work(work: String, ip: String)

class WorkSplitter extends Actor with ActorLogging {
  val workPattern = "(\\d+):(\\d+):(\\d+)".r
  def receive = {
    case Work(_, ip) if Global.isBanned(ip) => {
      log.info("Dropped work from banned ip")
    }
    case Work(work, ip) => {
      work.lines foreach ( _ match {
        case workPattern(address, nonce, height) => {
          try {
            Global.users.get(Convert.parseUnsignedLong(address)) match {
              case Some(_) => Global.workChecker ! MineShare(Convert.parseUnsignedLong(address),
        		  						   					 Convert.parseUnsignedLong(nonce),
        		  						   					 height.toInt,
        		  						   					 ip)
              case _ => {
                log.info("Address " + address + " not registered")
                Global.addAbuse(ip, 5)
              }
            }
            
          }
          catch {
            case e: Exception => log.error(e.toString())
          }
        }
        case a: String => log.info("Failed match " + a)// add to abuse counter
      })
    }
    case _ =>
  }

}