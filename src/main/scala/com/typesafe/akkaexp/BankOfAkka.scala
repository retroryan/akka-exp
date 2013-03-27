package com.typesafe.akkaexp

import concurrent.{ Promise, ExecutionContext }
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.Some
import java.util.concurrent.TimeoutException

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

  def receive = {
    case GetCustomerAccountBalances(id) => {
      val originalSender = sender
      log.info("originalSender: " + originalSender)
      implicit val ec: ExecutionContext = context.dispatcher

      context.actorOf(Props(new Actor() with ActorLogging {
        val promisedResult = Promise[AccountBalances]()
        var checkingBalances, savingsBalances, mmBalances: Option[List[(Long, BigDecimal)]] = None

        def receive = {
          case CheckingAccountBalances(balances) =>
            checkingBalances = balances
            collectBalances
          case SavingsAccountBalances(balances) =>
            savingsBalances = balances
            collectBalances
          case MoneyMarketAccountBalances(balances) =>
            mmBalances = balances
            collectBalances
        }

        def collectBalances() = (checkingBalances, savingsBalances, mmBalances) match {
          case (Some(c), Some(s), Some(m)) =>
            if (promisedResult.trySuccess(AccountBalances(checkingBalances, savingsBalances, mmBalances)))
              sendResults
          case _ => log.info("invalid message")
        }

        def sendResults() = {
          originalSender ! ((promisedResult.future.map(x => x)) recover {
            case t: TimeoutException => t
          })
          //context.system.stop(self)
        }

        savingsAccounts ! GetCustomerAccountBalances(id)
        checkingAccounts ! GetCustomerAccountBalances(id)
        moneyMarketAccounts ! GetCustomerAccountBalances(id)

        context.system.scheduler.scheduleOnce(250 milliseconds) {
          if (promisedResult.tryFailure(new TimeoutException)) {
            originalSender ! promisedResult.future
            //context.system.stop(self) // ADD THIS LINE
          }
        }
      }))
    }
  }
}