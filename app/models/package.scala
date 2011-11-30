import com.mongodb.casbah.{MongoOptions, MongoConnection, MongoDB}
import com.mongodb.ServerAddress
import com.novus.salat.Context
import play.Play
import play.Logger

package object models {

  implicit val ctx = new Context {
    val name = Some("PlaySalatContext")
  }

  ctx.registerClassLoader(Play.classloader)

  val connectionsPerHost = play.Play.configuration.getProperty("mongo.connectionsPerHost", 10)
  val mongoOptions = MongoOptions(connectionsPerHost = connectionsPerHost)

  def createConnection(connectionName: String): MongoDB  = if (Play.configuration.getProperty("mongo.test.context", "true").toBoolean || Play.mode == Play.Mode.DEV) {
    Logger.info("Starting Mongo in Test Mode connecting to localhost:27017")
    MongoConnection()(connectionName)
  }
  else if (mongoServerAddresses.isEmpty || mongoServerAddresses.size > 2) {
    Logger.info("Starting Mongo in Replicaset Mode connecting to %s".format(mongoServerAddresses.mkString(", ")))
    MongoConnection(mongoServerAddresses, mongoOptions)(connectionName)
  }
  else {
    Logger.info("Starting Mongo in Single Target Mode connecting to %s".format(mongoServerAddresses.head.toString))
    MongoConnection(mongoServerAddresses.head, mongoOptions)(connectionName)
  }

  lazy val mongoServerAddresses: List[ServerAddress] = {
    List(1, 2, 3).map {
      serverNumber =>
        val host = Play.configuration.getProperty("mongo.server%d.host".format(serverNumber)).stripMargin
        val port = Play.configuration.getProperty("mongo.server%d.port".format(serverNumber)).stripMargin
        (host, port)
    }.filter(entry => !entry._1.isEmpty && !entry._2.isEmpty).map(entry => new ServerAddress(entry._1, entry._2.toInt))
  }


}