/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.stream.samples.java;

import akka.NotUsed;
import akka.actor.ActorSystem;
import static akka.pattern.PatternsCS.after;

import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.model.ws.Message;
import static akka.http.javadsl.server.Directives.*;
import static java.util.concurrent.TimeUnit.SECONDS;

import akka.http.javadsl.Http;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import scala.concurrent.duration.FiniteDuration;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Accepts strings over websocket at ws://127.0.0.1/measurements
 * Protects the "database" by batching element in groups of 1000 but makes sure to at least
 * write every 1 second to not write too stale data or loose too much on failure.
 *
 * Based on this (great) blog article by Colin Breck:
 * http://blog.colinbreck.com/akka-streams-a-motivating-example/
 */
public class Sample4 {

    static class Database {

        private final ActorSystem system;

        public Database(ActorSystem system) {
            this.system = system;
        }

        public CompletionStage<List<String>> asyncBulkInsert(final List<String> entries) {
            // simulate that writing to a database takes ~30 millis
            return after(FiniteDuration.create(30, TimeUnit.MILLISECONDS),
                    system.scheduler(),
                    system.dispatcher(),
                    () -> {
                        System.out.println("write completed, last element: " + entries.get(entries.size() - 1));
                        return CompletableFuture.completedFuture(entries);
                    }
            );
        }
    }


    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create();
        final Materializer materializer = ActorMaterializer.create(system);
        final Database database = new Database(system);
        final ConnectHttp host = ConnectHttp.toHost("127.0.0.1", 8080);
        final Http http = Http.get(system);

        final Flow<Message, Message, NotUsed> measurementsFlow =
                Flow.of(Message.class)
                        .flatMapConcat((Message message) ->
                                // handles both strict and streamed ws messages by folding
                                // the later into a single string (in memory)
                                message.asTextMessage()
                                        .getStreamedText()
                                        .fold("", (acc, elem) -> acc + elem)
                        )
                        .groupedWithin(1000, FiniteDuration.create(1, SECONDS))
                        .mapAsync(5, database::asyncBulkInsert)
                        .map(written ->
                                TextMessage.create(
                                        "wrote up to: " + written.get(written.size() - 1)
                                )
                        );

        final Route route = path("measurements", () ->
                get(() ->
                        handleWebSocketMessages(measurementsFlow)
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
