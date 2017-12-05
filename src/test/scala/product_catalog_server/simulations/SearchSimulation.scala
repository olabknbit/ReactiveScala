package product_catalog_server.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import net.liftweb.json.Serialization
import product_catalog_server.simulations.TestData._

import scala.util.Random

/**
  *
  * Load test for the rest service.
  */
class SearchSimulation extends io.gatling.core.scenario.Simulation with SimulationConfig {

  val httpProtocol = http.baseURL(baseURL)

  implicit val formats = net.liftweb.json.DefaultFormats

  def randSearchWords = {
    StringBody(Serialization.write(searchWordsLists(Random.nextInt(searchWordsLists.size))))
  }

  /**
    * Scenario for simulation.
    */
  val scn = scenario("Simulation for the product catalog").repeat(repeatCount) {
    exec(
      http(session => "PUT: search")
        .put(searchItemsLink + "search")
        .header("Content-Type", "application/json")
        .body(randSearchWords)
        .check(status is 200)
    ).exec(
      http(session => "GET: stats")
        .get(searchItemsLink + "stats")
        .check(status is 200)
    )
  }

  /**
    * Sets the scenario.
    */
  setUp(scn.inject(atOnceUsers(threads)))
    .protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.is(percentSuccess)) //Check test result
}