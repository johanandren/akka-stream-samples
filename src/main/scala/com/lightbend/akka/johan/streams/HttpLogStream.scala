/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.streams

import java.nio.file.{FileSystems, Files, Path}
import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.stream.ActorMaterializer
import akka.stream.contrib.FileTailSource
import akka.stream.scaladsl.{Flow, Framing, Source}
import akka.util.ByteString
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat}

import scala.concurrent.duration._
import scala.util.Success

object HttpLogStream extends App {

  // a bunch of constants
  val MaxChunkSize = 1000
  val PollingInterval = 1.second
  val StartOffset = 0L

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val logfile = FileSystems.getDefault.getPath("/var/log/system.log")

  import LogEntry._
  val logEntries: Source[LogEntry, NotUsed] =
    FileTailSource.apply(logfile, MaxChunkSize, StartOffset, PollingInterval)
      .via(Framing.delimiter(ByteString.fromString("\n"), MaxChunkSize))
      .map(_.utf8String)
      .map { line =>
        system.log.info("Line consumed")
        line
      }.map(parseLine)
       .collect { case Success(entry) => entry }


  // streaming rendering of LogEntries as json-stream
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()
    .withFramingRenderer(Flow[ByteString].intersperse(ByteString(",\n")))
  implicit val dateTimeFormat = new RootJsonFormat[LocalDateTime] {
    override def read(json: JsValue): LocalDateTime = ??? // not actually supported here
    override def write(obj: LocalDateTime): JsValue = JsString(obj.toString)
  }
  implicit val logEntryFormat = jsonFormat4(LogEntry.apply)

  val routes =
    path("log") {
      complete(logEntries)
    }

  Http().bindAndHandle(routes, "localhost", 8080)

  /*
   * run server:
   * sbt "runMain com.lightbend.akka.johan.streams.HttpLogStream"
   *
   * curl localhost:8080/log
   */
}
