/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.stream.samples.java;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import scala.concurrent.duration.FiniteDuration;
import scala.util.Success;
import scala.util.Try;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class KafkaBotDetector {

  public static void main(String[] args) {
    final String topic = "logs";

    final ActorSystem system = ActorSystem.create();
    final Materializer materializer = ActorMaterializer.create(system);

    final ConsumerSettings<byte[], String> consumerSettings = ConsumerSettings
      .create(system, new ByteArrayDeserializer(), new StringDeserializer())
      .withGroupId("group1")
      .withBootstrapServers("127.0.0.1:9092");

    final Source<ConsumerRecord<byte[], String>, Consumer.Control> kafkaSource =
      Consumer.plainSource(consumerSettings, Subscriptions.topics(topic));


    kafkaSource
      .map(record -> parseLine(record.value()))
      .filter(Try::isSuccess) // filter out unparseable
      .map(tweet -> detectBot(tweet.get()))
      .filter(result -> result.isBot)
      .groupedWithin(100, FiniteDuration.create(1, TimeUnit.SECONDS))
      .runForeach(KafkaBotDetector::writeBotReport, materializer);

  }


  public static class TweetFromFile {
    public final String message;
    public TweetFromFile(String message) {
      this.message = message;
    }

    @Override
    public String toString() {
      return "TweetFromFile{" +
        "message='" + message + '\'' +
        '}';
    }
  }

  private static class BotDetectionResult {
    public final boolean isBot;
    public final TweetFromFile entry;
    public BotDetectionResult(boolean isBot, TweetFromFile entry) {
      this.isBot = isBot;
      this.entry = entry;
    }

    @Override
    public String toString() {
      return "BotDetectionResult{" +
        "isBot=" + isBot +
        ", entry=" + entry +
        '}';
    }
  }

  private static void writeBotReport(List<BotDetectionResult> possibleBotTweets) {
    // pretend bot report, we just print a bot hit count
    System.out.println("Detected possible bots within last time-period: " + possibleBotTweets.size());
  }

  private static BotDetectionResult detectBot(TweetFromFile entry) {
    // pretend bot detection, we just randomly say that it is a bot
    return new BotDetectionResult(ThreadLocalRandom.current().nextBoolean(), entry);
  }

  private static Try<TweetFromFile> parseLine(String line) {
    // pretend parsing, we just create
    return new Success<>(new TweetFromFile(line));
  }

   /*
      Try it out:

      run kafka (macOS/homebrew):
      zookeeper-server-start /usr/local/etc/kafka/zookeeper.properties
      kafka-server-start /usr/local/etc/kafka/server.properties

      run the app streaming entries into kafka:
      sbt "runMain com.lightbend.akka.johan.stream.samples.java.KafkaLogStreamer"

      run this app, consuming entries from kafka:
      sbt "runMain com.lightbend.akka.johan.stream.samples.java.KafkaBotDetector"

      write to the logfile:
      fortune | head -n1 >> watch-me.log
      done
     */


}
