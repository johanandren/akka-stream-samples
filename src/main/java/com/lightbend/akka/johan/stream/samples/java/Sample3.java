/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.stream.samples.java;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.*;

/**
 * Natural numbers (up to maximum long, then it wraps around) as a service http://127.0.0.1/numbers
 */
public class Sample3 {

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(system);
        final ConnectHttp host = ConnectHttp.toHost("127.0.0.1", 9000);
        final Http http = Http.get(system);

        // Note that a more realistic solution would use the EntityStreaming API to stream elements
        // as for example JSON Streaming (see the docs for more details)
        final Source<ByteString, NotUsed> numbers = Source.unfold(0L, n -> {
            long next = n + 1;
            return Optional.of(Pair.create(next, next));
        }).map(n -> ByteString.fromString(n.toString() + "\n"));


        final Route route =
                path("numbers", () ->
                        get(() ->
                                complete(HttpResponse.create()
                                        .withStatus(StatusCodes.OK)
                                        .withEntity(HttpEntities.create(
                                                ContentTypes.TEXT_PLAIN_UTF8,
                                                numbers
                                        )))
                        )
                );

        final CompletionStage<ServerBinding> bindingCompletionStage =
                http.bindAndHandle(route.flow(system, materializer), host, materializer);

        bindingCompletionStage.thenAccept((binding) -> {
            final InetSocketAddress address = binding.localAddress();
            System.out.println("Akka HTTP server running at " + address.getHostString() + ":" + address.getPort());
        }).exceptionally((ex) -> {
            System.out.print("Failed to bind HTTP server: " + ex.getMessage());
            ex.fillInStackTrace();
            return null;
        });

    }

}
