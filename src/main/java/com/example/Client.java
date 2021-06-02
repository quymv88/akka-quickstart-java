package com.example;

import akka.actor.ActorSelection;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class Client {

    public static void main(String[] args) {

        Map<String, Object> overrides = new HashMap<>();
        overrides.put("akka.remote.artery.canonical.port", 0);
        Config config = ConfigFactory.parseMap(overrides).withFallback(ConfigFactory.load());

        ActorSystem.create(Behaviors.setup(context -> {
            ActorSelection selection = context.classicActorContext().actorSelection("akka://ROOT@localhost:25520/user/helloakka");

            IntStream.range(0, 10).forEach(index -> selection.tell(

                    new RemoteActor.SayHello("Charles", context.spawn(Adaptor.create(), uidGenerate(index))),
                    context.classicActorContext().self()));

            return Behaviors.empty();
        }), "ROOT", config);
    }

    private static String uidGenerate(Object seed) {
        return  "tmp-adaptor-" + seed;
    }

    public static class Adaptor extends AbstractBehavior<String> {

        public Adaptor(ActorContext<String> context) {
            super(context);
        }

        public static Behavior<String> create() {
            return Behaviors.setup(Adaptor::new);
        }

        @Override
        public Receive<String> createReceive() {
            return newReceiveBuilder()
                    .onMessage(String.class, param -> {
                        System.out.println("Pong > " + param);
                        return this;
                    })
                    .build();
        }
    }
}
