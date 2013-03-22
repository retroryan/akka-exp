/*
 * Copyright © 2012 Typesafe, Inc. All rights reserved.
 */

package com.typesafe.akkaexp

import akka.actor.{ Actor, ActorLogging }

class AkkaExp extends Actor with ActorLogging {

  log.debug("The Akka Experiment has started!")

  override def receive = {
    case _ => log.info("Welcome to the Akka Experiment!")
  }
}
