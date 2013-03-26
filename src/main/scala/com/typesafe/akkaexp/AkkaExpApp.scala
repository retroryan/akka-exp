/*
 * Copyright © 2012 Typesafe, Inc. All rights reserved.
 */

package com.typesafe.akkaexp

import akka.actor.{ ActorLogging, Actor, ActorSystem, Props }

object AkkaExpApp extends App {

  val system = ActorSystem("BankOfAkka")

  val savingsAccountProxy = system.actorOf(Props[SavingsAccountProxy], "savingsAccountProxy")
  val checkingAccountProxy = system.actorOf(Props[CheckingAccountProxy], "checkingAccountProxy")
  val moneyMarketAccountsProxy = system.actorOf(Props[MoneyMarketAccountsProxy], "moneyMarketAccountsProxy")

  val accountBalanceRetriever = system.actorOf(Props(new AccountBalanceRetriever(savingsAccountProxy, checkingAccountProxy, moneyMarketAccountsProxy)),
    "accountBalanceRetriever")

  system.actorOf(Props(new Actor with ActorLogging {

    accountBalanceRetriever ! GetCustomerAccountBalances(1)
    accountBalanceRetriever ! "test send"

    override def receive = {
      case message => {
        log.info(message.toString)
      }
    }
  }))

  Console.readLine("The enter key will get you out of here!\n")
  system.shutdown()

}
