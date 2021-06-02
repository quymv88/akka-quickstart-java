package com.example.ask;

import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.example.RemoteActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;

// demo how to use AskPattern to call remote actor
public class Client2 {

    public static void main(String[] args) {

        Map<String, Object> overrides = new HashMap<>();
        overrides.put("akka.remote.artery.canonical.port", 0);
        Config config = ConfigFactory.parseMap(overrides).withFallback(ConfigFactory.load());

        ActorSystem.create(Behaviors.<String>setup(context -> {

            // Need only one instance
            ActorRef<RemoteActor.SayHello> localProxy = context.spawn(Proxy.create(), "proxy");

            IntStream.range(0, 10).forEach(index -> ask(localProxy, context).whenComplete((s, throwable) -> System.out.println(">>>>>>>>>>>>" + s)));

            return Behaviors.empty();
        }), "ROOT", config);
    }

    private static CompletionStage<String> ask(ActorRef<RemoteActor.SayHello> localProxy, ActorContext<?> context) {

        return AskPattern.ask(
                localProxy,
                ref -> new RemoteActor.SayHello("QuyMV", ref),
                Duration.ofSeconds(5),
                context.getSystem().scheduler());
    }
}
