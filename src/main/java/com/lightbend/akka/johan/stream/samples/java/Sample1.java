/*
 * Copyright (C) 2009-2018 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.stream.samples.java;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.*;
import akka.stream.javadsl.*;

import java.util.concurrent.CompletionStage;

/**
 * Minimal sample of Akka Streams
 */
public class Sample1 {
    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(system);

        final Source<Integer, NotUsed> source =
                Source.range(0, 20_000_000);

        final Flow<Integer, String, NotUsed> flow =
                Flow.fromFunction((Integer n) -> n.toString());

        final Sink<String, CompletionStage<Done>> sink =
                Sink.foreach(str -> System.out.println(str));

        final RunnableGraph<NotUsed> runnable = source.via(flow).to(sink);

        runnable.run(materializer);
    }
}
