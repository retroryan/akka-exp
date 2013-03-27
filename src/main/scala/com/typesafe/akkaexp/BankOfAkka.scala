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
  val checkingBalances, savingsBalances, mmBalances: Option[List[(Long, BigDecimal)]] = None
  var originalSender: Option[ActorRef] = None

  def receive = {

    case GetCustomerAccountBalances(id) =>
      originalSender = Some(sender)
      savingsAccounts ! GetCustomerAccountBalances(id)
      checkingAccounts ! GetCustomerAccountBalances(id)
      moneyMarketAccounts ! GetCustomerAccountBalances(id)

    //The Account Proxies send back  CheckingAccountBalances, etc. and not AccountBalances, so this never gets called.
    case AccountBalances(cBalances, sBalances, mmBalances) =>
      (checkingBalances, savingsBalances, mmBalances) match {
        case (Some(c), Some(s), Some(m)) => originalSender.get ! AccountBalances(checkingBalances, savingsBalances,
          mmBalances)
        case _ => originalSender.get ! "Invalid Balance Recieved"
      }

    case _ => originalSender.get ! "Invalid Message Recieved"
  }
}