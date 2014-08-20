import akka.actor.ActorSystem
import akka.actor.Props
import akka.io.IO
import spray.can.Http
import simple_burst_pool.Global
import simple_burst_pool.Config
import simple_burst_pool.Db
import simple_burst_pool.PoolServiceActor
import simple_burst_pool.StateChecker
import simple_burst_pool.WorkChecker
import simple_burst_pool.WorkSplitter
import simple_burst_pool.WorkSubmitter
import simple_burst_pool.WorkPayor
import simple_burst_pool.LastBlockChecker
import simple_burst_pool.Unbanner
import akka.routing.RoundRobinPool

object SimplePool extends App {
	if(!Config.init) {
	  System.out.println("Failed to load config")
	  System.exit(0)
	}
    if(!Db.init) {
	  System.out.println("Failed to init Db")
	  System.exit(0)
	}
	implicit val system = ActorSystem("system")
	val stateChecker = system.actorOf(Props[StateChecker])
	val unbanner = system.actorOf(Props[Unbanner])
	Global.workPayor = system.actorOf(Props[WorkPayor])
	//Global.workChecker = system.actorOf(Props[WorkChecker])
	Global.workChecker = system.actorOf(RoundRobinPool(Config.workCheckingThreads).props(Props[WorkChecker]))
	Global.workSplitter = system.actorOf(Props[WorkSplitter])
	Global.workSubmitter = system.actorOf(Props[WorkSubmitter])
	Global.lastBlockChecker = system.actorOf(Props[LastBlockChecker])
	Global.lastBlockChecker ! simple_burst_pool.CheckGenerator()
	val poolService = system.actorOf(Props[PoolServiceActor])
	IO(Http) ! Http.Bind(poolService, interface = "0.0.0.0", port = Config.listenPort)
}