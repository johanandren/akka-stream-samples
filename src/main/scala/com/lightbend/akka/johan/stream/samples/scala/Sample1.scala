/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.stream.samples.scala

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

/**
  * Minimal sample of Akka Streams
  */
object Sample1 extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  val source = Source(0 to 20000000)

  val flow = Flow[Int].map(_.toString())

  val sink = Sink.foreach[String](println(_))

  val runnable = source.via(flow).to(sink)

  runnable.run()

}
