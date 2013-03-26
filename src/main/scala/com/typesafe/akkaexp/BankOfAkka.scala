package com.typesafe.akkaexp

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.Some

case class GetCustomerAccountBalances(id: Long)

case class AccountBalances(checking: Option[List[(Long, BigDecimal)]],
  savings: Option[List[(Long, BigDecimal)]],
  moneyMarket: Option[List[(Long, BigDecimal)]])

case class CheckingAccountBalances(balances: Option[List[(Long, BigDecimal)]])

case class SavingsAccountBalances(balances: Option[List[(Long, BigDecimal)]])

case class MoneyMarketAccountBalances(balances: Option[List[(Long, BigDecimal)]])

class SavingsAccountProxy extends Actor with ActorLogging {
  def receive = {
    case GetCustomerAccountBalances(id: Long) =>
      sender ! SavingsAccountBalances(Some(List((1, 150000), (2, 29000))))
  }
}

class CheckingAccountProxy extends Actor {
  def receive = {
    case GetCustomerAccountBalances(id: Long) =>
      sender ! CheckingAccountBalances(Some(List((3, 15000))))
  }
}

class MoneyMarketAccountsProxy extends Actor {
  def receive = {
    case GetCustomerAccountBalances(id: Long) =>
      sender ! MoneyMarketAccountBalances(None)
  }
}

class AccountBalanceRetriever(savingsAccounts: ActorRef, checkingAccounts: ActorRef, moneyMarketAccounts: ActorRef) extends Actor with ActorLogging {
  implicit val timeout: Timeout = 100 milliseconds
  implicit val ec: ExecutionContext = context.dispatcher

  def receive = {

    case GetCustomerAccountBalances(id) =>
      val futSavings = savingsAccounts ? GetCustomerAccountBalances(id)
      val futChecking = checkingAccounts ? GetCustomerAccountBalances(id)
      val futMM = moneyMarketAccounts ? GetCustomerAccountBalances(id)
      val futBalances = for {
        savings <- futSavings.mapTo[SavingsAccountBalances]
        checking <- futChecking.mapTo[CheckingAccountBalances]
        mm <- futMM.mapTo[MoneyMarketAccountBalances]
      } yield AccountBalances(savings.balances, checking.balances, mm.balances)

      futBalances map (fb => {
        sender ! "test"
        sender ! fb
        log.info("sent: " + fb)
      })

    case _ => sender ! "verified"
  }
}