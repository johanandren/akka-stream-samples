package com.lightbend.akka.johan.stream.samples.java;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.ActorAttributes;
import akka.stream.ActorMaterializer;
import akka.stream.Attributes;
import akka.stream.Materializer;
import akka.stream.contrib.FileTailSource;
import akka.stream.javadsl.Framing;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import scala.concurrent.duration.FiniteDuration;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class KafkaLogStreamer {

  private final static int MAX_CHUNK_SIZE = 1000;
  private final static FiniteDuration POLLING_INTERVAL = FiniteDuration.apply(1L, TimeUnit.MILLISECONDS);
  private final static long START_OFFSET = 0;
  private final static Attributes logLevels = ActorAttributes.createLogLevels(
    Logging.InfoLevel(), Logging.InfoLevel(), Logging.InfoLevel());


  public static void main(String[] args) {

    final FileSystem fs = FileSystems.getDefault();
    final Path logfile = fs.getPath("watch-me.log");
    System.out.println("Watching: " + logfile);
    final String topic = "logs";

    final ActorSystem system = ActorSystem.create();
    final Materializer materializer = ActorMaterializer.create(system);

    // settings for connecting to akka
    final ProducerSettings<byte[], String> producerSettings = ProducerSettings
      .create(system, new ByteArraySerializer(), new StringSerializer())
      .withBootstrapServers("127.0.0.1:9092");

    final Source<String, NotUsed> logLines =
      FileTailSource.create(logfile, MAX_CHUNK_SIZE, START_OFFSET, POLLING_INTERVAL)
        .via(Framing.delimiter(ByteString.fromString("\n"), MAX_CHUNK_SIZE))
        .map(ByteString::utf8String)
        .log("file-watcher").addAttributes(logLevels);

    final Sink<ProducerRecord<byte[], String>, CompletionStage<Done>> kafkaSink =
      Producer.plainSink(producerSettings);

    logLines
      .map(line -> new ProducerRecord<byte[], String>(topic, line))
      .to(kafkaSink)
      .run(materializer);

  }

   /*
      run kafka (macOS/homebrew):
      zookeeper-server-start /usr/local/etc/kafka/zookeeper.properties
      kafka-server-start /usr/local/etc/kafka/server.properties

      run this app
      sbt "runMain com.lightbend.akka.johan.stream.samples.java.KafkaLogStreamer"

      put something into logfile:
      fortune | head -n1 >> watch-me.log

      see that stuff got into kafka:
      kafka-console-consumer --zookeeper localhost:2181 --topic logs --from-beginning
     */


}