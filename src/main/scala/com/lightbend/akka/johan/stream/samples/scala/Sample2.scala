/*
 * Copyright (C) 2009-2018 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.stream.samples.scala

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

/**
  * Stages can be expressed with a "fluent" API:
  */
object Sample2 extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  Source(0 to 20000000)
    .map(_.toString)
    .runForeach(println)

}
