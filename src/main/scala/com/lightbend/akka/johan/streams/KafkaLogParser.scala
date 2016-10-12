/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.streams

import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.util.control.NoStackTrace
import scala.util.{Failure, Success, Try}


case class LogEntry(timestamp: LocalDateTime, host: String, process: String, text: String)

object LogEntry {

  object Unparseable extends RuntimeException with NoStackTrace

  val UnparseableFailure = Failure(Unparseable)
  private val LogEntryExp = """(\w+ \d+ \d+:\d+:\d+) (\S+).(\S+) (.*)""".r
  private val timeFormat = new DateTimeFormatterBuilder()
    .appendPattern("MMM dd HH:mm:ss")
    .parseDefaulting(ChronoField.YEAR, 2016) // year is not in there
    .toFormatter

  def parseLine(line: String): Try[LogEntry] = line match {
    case LogEntryExp(timeText, host, process, text) =>
      Try {
        LogEntry(LocalDateTime.from(timeFormat.parse(timeText)), host, process, text)
      }

    case _ => UnparseableFailure
  }

  def detectHacker(logEntries: Seq[LogEntry]): Option[LogEntry] = {
    def isSuspicious(entry: LogEntry) =
      entry.text.contains("com.openssh.sshd") && entry.text.contains("Service exited with abnormal code: 255")

    // if head is suspicious and there are more suspicious entries in there
    // it certainly is suspicious
    logEntries.headOption.filter(isSuspicious).flatMap { entry =>
      if (logEntries.tail.exists(isSuspicious)) Some(entry)
      else None
    }
  }
}

object KafkaLogParser extends App {
  val Topic = "system-logs"

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val consumerSettings =
    ConsumerSettings(
      system, new ByteArrayDeserializer,
      new StringDeserializer)
      .withGroupId("group1")
      .withBootstrapServers("127.0.0.1:9092")

  val kafkaSource = Consumer.plainSource(consumerSettings, Subscriptions.topics(Topic))

  import LogEntry._
  kafkaSource.map(record => parseLine(record.value()))
    .alsoTo(Sink.foreach(t => print(if (t.isSuccess) "." else "x")))
    // filter out unparseable
    .collect { case Success(entry) => entry }
    .sliding(10, 1)
    .mapConcat(entries => detectHacker(entries).toList)
    .runForeach(entry => println(s"\nALARM ALARM, possible hacker: $entry"))

  /*
  with KafkaLogParser started (or at least kafka/zookeeper)

  run this app
  sbt "runMain com.lightbend.akka.johan.streams.KafkaLogParser"
 */

}
