/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.stream.samples.java;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.common.EntityStreamingSupport;
import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

import static akka.http.javadsl.server.Directives.completeWithSource;
import static akka.http.javadsl.server.Directives.path;

public class HttpStreamTweet {

  public static class Tweet {
    private final String text;
    public Tweet() { this.text = ""; }
    public Tweet(String text) {
      this.text = text;
    }
    public String getText() { return text; }
  }

  public static void main(String[] args) {
    final ActorSystem system = ActorSystem.create();
    final Materializer materializer = ActorMaterializer.create(system);
    final Http http = Http.get(system);
    final Marshaller<Tweet, ByteString> marshaller = JacksonMarshalling.marshaller();

    final Source<Tweet, NotUsed> tweets =
      Source.repeat(new Tweet("Hello world"))
        .map(tweet -> {
          system.log().info("Source generated tweet");
          return tweet;
        });

    final Route tweetsRoute =
      path("tweets", () ->
        completeWithSource(tweets, marshaller, EntityStreamingSupport.json())
      );


    final Flow<HttpRequest, HttpResponse, NotUsed> handler =
      tweetsRoute.flow(system, materializer);

    http.bindAndHandle(handler,
      ConnectHttp.toHost("localhost", 8080),
      materializer
    );
    System.out.println("Running at http://localhost:8080");

  }

  /*
  Try it out:
  sbt "runMain com.lightbend.akka.johan.stream.samples.java.HttpStreamTweet"

  access the http route:
  curl http://localhost:8080/tweets

  look at the os buffers (macOS):
  ./print-os-buffers.sh

  pause the client Ctrl+Z - start it again with 'fg' (unixy os:es)
  
   */
}

