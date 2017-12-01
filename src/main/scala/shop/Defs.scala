package shop

sealed trait PaymentMethod

object PaymentMethods {
  case object CashPayment extends PaymentMethod

  case object TransferPayment extends PaymentMethod

  case object CreditCardPayment extends PaymentMethod

}

sealed trait DeliveryMethod

object DeliveryMethods {
  case object PostalDelivery extends DeliveryMethod

  case object CollectionInPerson extends DeliveryMethod

  case object CourierDelivery extends DeliveryMethod
}

final case class CartId(value: Int)

final case class CheckoutId(value: Int)