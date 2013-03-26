/*
 * Copyright © 2012 Typesafe, Inc. All rights reserved.
 */

package com.typesafe.akkaexp

import akka.actor.{ ActorSystem, Props }

object AkkaExpApp extends App {

  val system = ActorSystem("akka-exp-system")
  val akkaExp = system.actorOf(Props[AkkaExp], "akka-exp")
  akkaExp ! "Start the experiment!"

  Console.readLine("The enter key will get you out of here!")
  system.shutdown()

}
