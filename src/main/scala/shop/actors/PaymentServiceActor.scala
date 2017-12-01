package shop.actors

import java.util.Date

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Timers}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.remote.RemoteTransportException
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import org.jboss.netty.channel.ChannelException
import shop.PaymentMethod
import shop.actors.CheckoutActor.PaymentReceived
import shop.actors.CustomerActor.{PaymentConfirmed, PaymentUnsuccessful}
import shop.utils.TimeProvider

import scala.concurrent.Await
import scala.concurrent.duration._

class PaymentServiceActor(paymentMethod: PaymentMethod, timeout: Int = 1200) extends Actor with ActorLogging with Timers {

  import PaymentService._
  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  private val http = Http(context.system)

  override def receive: Receive = {
    case DoPayment(amount) =>
      log.info("Payment received in payment service actor")
      http.singleRequest(HttpRequest(HttpMethods.POST, uri = "http://localhost:8080/doPayment/" + amount)).pipeTo(self)
      timers.startSingleTimer(PaymentServiceTimerKey, PaymentTimeout, getTimeout(TimeProvider.current) seconds)
      context become paymentDone(sender())

    case PaymentTimeout =>
      throw TimeoutException("Request timeout")
  }

  private def paymentDone(senderRef: ActorRef): Receive = {
    case resp@HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        log.info("Got response, body: " + body.utf8String)
        senderRef ! PaymentConfirmed()
        context.parent ! PaymentReceived(Integer.parseInt(body.utf8String.toString))
        resp.discardEntityBytes()
        shutdown()
      }

    case resp@HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      senderRef ! PaymentUnsuccessful()
      resp.discardEntityBytes()
      shutdown()

  }

  def shutdown() = {
    Await.result(http.shutdownAllConnectionPools(), Duration.Inf)
    context.system.terminate()
  }

  private def getTimeout(eventDate: Date) = Math.max((timeout * 1000 + eventDate.getTime - TimeProvider.current.getTime) / 1000, 0)

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: IllegalRequestException => Resume
      case _: IllegalUriException => Resume
      case _: IllegalResponseException => Resume
      case _: NumberFormatException => Resume
      case _: java.net.ConnectException => Restart
      case _: akka.stream.StreamTcpException => Restart
      case _: TimeoutException => Restart
      case _: ChannelException => Restart
      case _: RemoteTransportException => Restart
      case _: NullPointerException ⇒ Restart
      case _: IllegalArgumentException ⇒ Stop
      case _: Exception ⇒ Escalate
    }
}

object PaymentService {

  case class DoPayment(amount: Int)

  case object PaymentTimeout

  case object PaymentServiceTimerKey

  case class TimeoutException(exception: String)  extends Exception

}