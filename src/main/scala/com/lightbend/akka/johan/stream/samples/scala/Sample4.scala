/*
 * Copyright (C) 2009-2018 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.stream.samples.scala

import akka.pattern.Patterns.after
import akka.actor.ActorSystem
import akka.http.javadsl.model.ws.BinaryMessage
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Accepts strings over websocket at ws://127.0.0.1/measurements
  * Protects the "database" by batching element in groups of 1000 but makes sure to at least
  * write every 1 second to not write too stale data or loose too much on failure.
  *
  * Based on this (great) blog article by Colin Breck:
  * http://blog.colinbreck.com/akka-streams-a-motivating-example/
  */
object Sample4 extends App {

  object Database {
    def asyncBulkInsert(entries: Seq[String])(implicit system: ActorSystem): Future[Seq[String]] =
      // simulate that writing to a database takes ~30 millis
      after(30.millis, system.scheduler, system.dispatcher, Future.successful(entries))

  }

  implicit val system = ActorSystem()
  import system.dispatcher
  implicit val mat = ActorMaterializer()

  val measurementsFlow =
    Flow[Message].flatMapConcat{
        // handles both strict and streamed ws messages by folding
        // the later into a single string (in memory)
        case t: TextMessage => t.textStream.fold("")(_ + _)
        case b: BinaryMessage => throw new RuntimeException("Binary not supported")
      }
      .groupedWithin(1000, 1.second)
      .mapAsync(5)(Database.asyncBulkInsert)
      .map(written => TextMessage("wrote up to: " + written.last))

  val route =
    path("measurements") {
      get {
        handleWebSocketMessages(measurementsFlow)
      }
    }

  val futureBinding = Http().bindAndHandle(route, "127.0.0.1", 8080)

  futureBinding.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      println(s"Akka HTTP server running at ${address.getHostString}:${address.getPort}")

    case Failure(ex) =>
      println(s"Failed to bind HTTP server: ${ex.getMessage}")
      ex.fillInStackTrace()

  }

}
