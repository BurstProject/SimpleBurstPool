package simple_burst_pool

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import org.squeryl.annotations.Column
import org.squeryl.SessionFactory
import java.util.Date
import java.sql.Timestamp
import org.squeryl.adapters.H2Adapter
import org.squeryl.Session
import com.jolbox.bonecp.BoneCP
import com.jolbox.bonecp.BoneCPConfig

class User(val generateAddress: Long,
		   val payoutAddress: String) {
}

class Share(val blockHeight: Long,
			val generateAddress: Long,
			val nonce: Long)

class Block(val height: Long)

object PoolDb extends Schema {
  val users = table[User]
  val shares = table[Share]
  val blocks = table[Block]
  
  on(users) (u => declare(
      u.generateAddress is(unique),
      columns(u.generateAddress, u.payoutAddress) are(indexed)
  ))
  
  on(shares) (s => declare(
      columns(s.blockHeight, s.generateAddress) are(indexed)
  ))
  
  on(blocks) (b => declare(
      b.height is(indexed)
  ))
}

object Db {
  def init: Boolean = {
    Class.forName("org.h2.Driver")
    val bcpConfig = new BoneCPConfig
    bcpConfig.setJdbcUrl("jdbc:h2:db/pooldb")
    val pool = new BoneCP(bcpConfig)
    SessionFactory.concreteFactory = Some(
    	() => Session.create(pool.getConnection(), new H2Adapter)
    )
    
    if(Config.dbCreate != 0) {
      try {
        transaction {
          PoolDb.create
        }
      }
      catch {
        case _: Throwable =>// ignore. thrown when db exists
      }
    }
    
    
    try {
      transaction {
	    val users = from(PoolDb.users)(u => select(u))
	    users foreach { u =>
	      Global.users += u.generateAddress -> u.payoutAddress 
        }
      }
    }
    catch {
      case e: Exception => {
        System.out.println("Failed to load users")
        return false
      }
    }
    
    var lastBlock = 0L
    try {
      transaction {
        val block = from(PoolDb.blocks)(b =>
          select(b)
          orderBy(b.height desc)
        ).single
        lastBlock = block.height 
      }
    }
    catch {
      case e: Exception =>// assume never found block
    }
    
    try {
      transaction {
        val shares = from(PoolDb.shares)(s =>
          where(s.blockHeight gt lastBlock)
          select(s)
        )
        shares foreach { s =>
          Global.shares += (s.generateAddress, s.nonce, s.blockHeight) -> ()
          val numShares = Global.shareCount.getOrElseUpdate(s.generateAddress, 0)
          Global.shareCount.put(s.generateAddress , numShares + 1)
        }
      }
    }
    catch {
      case e: Exception => System.out.println("Failed to load shares: " + e.toString())
    }
    true
  }
}
