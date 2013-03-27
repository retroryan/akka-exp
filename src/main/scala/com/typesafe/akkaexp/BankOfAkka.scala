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
    case GetCustomerAccountBalances(id) => {
      context.actorOf(Props(new Actor() {
        var checkingBalances, savingsBalances, mmBalances: Option[List[(Long, BigDecimal)]] = None

        //sender in this context is dead letters, so this doesn't work
        val originalSender = sender
        log.info("originalSender =  " + originalSender)

        def receive = {
          case CheckingAccountBalances(balances) =>
            checkingBalances = balances
            isDone
          case SavingsAccountBalances(balances) =>
            savingsBalances = balances
            isDone
          case MoneyMarketAccountBalances(balances) =>
            mmBalances = balances
            isDone
        }

        //isDone doesn't get sent AccountBalances, but the individual Balances,
        // so the case statement doesn't work
        def isDone() = {
          (checkingBalances, savingsBalances, mmBalances) match {
            case (Some(c), Some(s), Some(m)) =>
              originalSender ! AccountBalances(checkingBalances, savingsBalances, mmBalances)
            //context.system.stop(self)
            case _ => log.info("invalid message in isDone")
          }
        }

        savingsAccounts ! GetCustomerAccountBalances(id)
        checkingAccounts ! GetCustomerAccountBalances(id)
        moneyMarketAccounts ! GetCustomerAccountBalances(id)
      }))
    }
  }
}