package product_catalog_server.simulations

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object SimulationRunner {

  def main(args: Array[String]) {


    // This sets the class for the simulation we want to run.
    val simClass = classOf[SearchSimulation].getName

    val props = new GatlingPropertiesBuilder
    props.sourcesDirectory("./src/main/scala")
    props.binariesDirectory("./target/scala-2.11/classes")
    props.simulationClass(simClass)
    Gatling.fromMap(props.build)

  }
}