/*
 * Copyright (C) 2009-2018 Lightbend Inc. <http://www.lightbend.com>
 */
package com.lightbend.akka.johan.stream.samples.scala

import akka.stream.stage.{AbstractOutHandler, GraphStage, GraphStageLogic, InHandler};
import akka.stream.{Attributes, FlowShape, Inlet, Outlet};

final class Map[A, B](f: A => B) extends GraphStage[FlowShape[A, B]] {
  val in = Inlet[A]("Map.in")
  val out = Outlet[B]("Map.out")
  val shape = FlowShape.of(in, out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler() {
        def onPush(): Unit = {
          push(out, f(grab(in)))
        }
      })
      setHandler(out, new AbstractOutHandler() {
        def onPull(): Unit = {
          pull(in)
        }
      })
    }
}

