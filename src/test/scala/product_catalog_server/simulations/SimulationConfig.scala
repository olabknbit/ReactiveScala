package product_catalog_server.simulations

import akka.event.slf4j.SLF4JLogging
import com.typesafe.config.{ConfigException, ConfigFactory}

import scala.util.Try

trait SimulationConfig extends SLF4JLogging {

  /**
    * Application config object.
    */
  private[this] val config = ConfigFactory.load()

  /**
    * Gets the required string from the config file or throws
    * an exception if the string is not found.
    *
    * @param path path to string
    * @return string fetched by path
    */
  def getRequiredString(path: String) = {
    Try(config.getString(path)).getOrElse {
      handleError(path)
    }
  }

  /**
    * Gets the required int from the config file or throws
    * an exception if the int is not found.
    *
    * @param path path to int
    * @return int fetched by path
    */
  def getRequiredInt(path: String) = {
    Try(config.getInt(path)).getOrElse {
      handleError(path)
    }
  }

  /**
    * Gets the required string list from the config file or throws
    * an exception if the string list is not found.
    *
    * @param path path to string list
    * @return string list fetched by path
    */
  def getRequiredStringList(path: String) = {
    Try(config.getStringList(path)).getOrElse {
      handleError(path)
    }
  }

  private[this] def handleError(path: String) = {
    val errMsg = s"Missing required configuration entry: $path"
    log.error(errMsg)
    throw new ConfigException.Missing(errMsg)
  }

  /**
    * URL for test.
    */
  val baseURL = getRequiredString("service.host")

  /**
    * Endpoint link.
    */
  val searchItemsLink = getRequiredString("service.api_link")

  /**
    * Scenario repeat count.
    */
  val repeatCount = getRequiredInt("scenario.repeat_count")

  /**
    * Count of users for simulation.
    */
  val threads = getRequiredInt("scenario.repeat_count")

  /**
    * Percent of successful service responses when
    * the simulation is considered to be successful.
    */
  val percentSuccess = Try(config.getInt("scenario.percent_success")).getOrElse(100)
}