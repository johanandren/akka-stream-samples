/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.stream.samples.java;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;

/**
 * Stages can be expressed with a "fluent" API:
 */
public class Sample2 {
    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(system);

        Source.range(0, 20000000)
                .map(Object::toString)
                .runForeach(str -> System.out.println(str), materializer);

    }
}
