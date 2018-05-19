package com.lightbend.akka.johan.stream.samples.java;

import akka.stream.Attributes;
import akka.stream.FlowShape;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;

import java.util.function.Function;

public class Sample5 {

  public final static class Map<A, B> extends GraphStage<FlowShape<A, B>> {
    private final Function<A, B> f;
    public final Inlet<A> in = Inlet.create("Map.in");
    public final Outlet<B> out = Outlet.create("Map.out");
    private final FlowShape<A, B> shape = FlowShape.of(in, out);

    public Map(Function<A, B> f) {
      this.f = f;
    }

    public FlowShape<A, B> shape() {
      return shape;
    }

    public GraphStageLogic createLogic(Attributes inheritedAttributes) {
      return new GraphStageLogic(shape) {
        {
          setHandler(in, new AbstractInHandler() {
            public void onPush() throws Exception {
              push(out, f.apply(grab(in)));
            }
          });
          setHandler(out, new AbstractOutHandler() {
            public void onPull() throws Exception {
              pull(in);
            }
          });
        }
      };
    }
  }
}
