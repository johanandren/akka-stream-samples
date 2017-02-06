/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.stream.samples.java;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.*;
import akka.stream.javadsl.*;

import java.util.concurrent.CompletionStage;

public class Sample1 {
    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(system);

        Source<Integer, NotUsed> source = Source.range(0, 20000000);

        Flow<Integer, String, NotUsed> flow =
                Flow.fromFunction((Integer n) -> n.toString());

        Sink<String, CompletionStage<Done>> sink =
                Sink.foreach(str -> System.out.println(str));

        RunnableGraph<NotUsed> runnable = source.via(flow).to(sink);

        runnable.run(materializer);
    }
}
