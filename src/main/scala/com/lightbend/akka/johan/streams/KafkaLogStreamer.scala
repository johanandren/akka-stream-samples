/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.streams

import java.nio.file.FileSystems

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.contrib.FileTailSource
import akka.stream.scaladsl.Framing
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.duration._

object KafkaLogStreamer extends App {

  // a bunch of constants
  val MaxChunkSize = 1000
  val PollingInterval = 1.second
  val StartOffset = 0L
  val Topic = "system-logs"

  // materializer to materialize the stream and system to run it on
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val fs = FileSystems.getDefault
  val logfile = fs.getPath("/var/log/system.log")

  // settings for connecting to kafka
  val producerSettings =
    ProducerSettings(
      system,
      new ByteArraySerializer,
      new StringSerializer
    ).withBootstrapServers("127.0.0.1:9092")


  // tail
  val logLines: Source[String, NotUsed] =
    FileTailSource.apply(logfile, MaxChunkSize, StartOffset, PollingInterval)
      .via(Framing.delimiter(ByteString.fromString("\n"), MaxChunkSize))
      .map(_.utf8String)
      .map { line =>
        print(".") // make it visual that something happens
        line
      }

  val kafkaSink = Producer.plainSink(producerSettings)

  logLines.map(line => new ProducerRecord[Array[Byte], String](Topic, line))
    .to(kafkaSink)
    .run()

  /*
     run kafka (macOS/homebrew):
     zookeeper-server-start /usr/local/etc/kafka/zookeeper.properties
     kafka-server-start /usr/local/etc/kafka/server.properties

     run this app
     sbt "runMain com.lightbend.akka.johan.streams.KafkaLogStreamer"

     see that stuff got into kafka:
     kafka-console-consumer --zookeeper localhost:2181 --topic system-logs --from-beginning
    */

}
