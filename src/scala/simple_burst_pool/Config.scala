package simple_burst_pool

object Config {
    var dbCreate = 1
    var listenPort: Int = 80
    var walletAddress: String = "http://127.0.0.1:8125"
    var targetDeadline: Long = 0
    var passphrase: String = ""
    var workCheckingThreads: Int = 1
    var banThreshold = 100
    var fee = 1
    var feeAddress = "BURST-QHCJ-9HB5-PTGC-5Q8J9"

	val ConfigFormat = "(\\S+?)\\s*=\\s*(\\S+)".r
	def init: Boolean = {
	  try {
	    val lines = io.Source.fromFile("simplepool.conf").mkString
	    lines.lines foreach (_ match {
	      case ConfigFormat(key, value) => {
	        key match {
	          case "dbCreate" => dbCreate = value.toInt
	          case "listenPort" => listenPort = value.toInt
	          case "walletAddress" => walletAddress = value
	          case "targetDeadline" => targetDeadline = value.toLong
	          case "passphrase" => passphrase = value
	          case "workCheckingThreads" => workCheckingThreads = value.toInt
	          case "banThreshold" => banThreshold = value.toInt
	          case "fee" => fee = value.toInt
	          case "feeAddress" => feeAddress = value
	          case k => System.out.println("unknown config option: " + k)
	        }
	      }
	      case "" =>
	      case l => System.out.println("failed to parse: " + l)
	    })
	  }
	  catch {
	    case e: Exception => {
	      System.out.println(e)
	      return false
	    }
	  }
	  if(targetDeadline == 0 ||
	     passphrase == "") {
	    false
	  }
	  else {
	    true
	  }
	}
}