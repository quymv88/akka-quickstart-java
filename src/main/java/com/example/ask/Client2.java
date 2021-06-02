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

            ActorRef<RemoteActor.SayHello> localProxy = context.spawn(Proxy.create(), "proxy");

            IntStream.range(0, 10).forEach(index -> ask(localProxy, context).whenComplete((s, throwable) -> System.out.println(">>>>>>>>>>>>" + s)));

            return Behaviors.empty();
        }), "ROOT", config);
    }

    private static CompletionStage<String> ask(ActorRef<RemoteActor.SayHello> localProxy, ActorContext<String> context) {

        return AskPattern.ask(
                localProxy,
                ref -> new RemoteActor.SayHello("Q", ref),
                Duration.ofSeconds(5),
                context.getSystem().scheduler());
    }

    private static String uidGenerate(Object seed) {
        return  "tmp-adaptor-" + seed;
    }

    public static class Proxy extends AbstractBehavior<RemoteActor.SayHello> {

        long counter = 0;

        public Proxy(ActorContext<RemoteActor.SayHello> context) {
            super(context);
        }

        public static Behavior<RemoteActor.SayHello> create() {
            return Behaviors.setup(Proxy::new);
        }

        @Override
        public Receive<RemoteActor.SayHello> createReceive() {

            return newReceiveBuilder()
                    .onMessage(RemoteActor.SayHello.class, msg -> {

                        ActorSelection selection = getContext().classicActorContext().actorSelection("akka://ROOT@localhost:25520/user/helloakka");

                        selection.tell(
                                new RemoteActor.SayHello(msg.name, getContext().spawn(Client2.Adaptor.create(msg.ref), uidGenerate(counter++))),
                                getContext().classicActorContext().self());

                        return this;
                    })
                    .build();
        }
    }

    public static class Adaptor extends AbstractBehavior<String> {

        private final ActorRef<String> originalRequester;

        private final Cancellable cancellable;
        private final ActorRef<String> self;

        public Adaptor(ActorContext<String> context, ActorRef<String> requester) {

            super(context);
            self = context.getSelf();

            cancellable = context.getSystem().scheduler().scheduleOnce(
                    Duration.ofSeconds(5),
                    () -> self.tell("Timeout!"),
                    context.getExecutionContext()
            );

            this.originalRequester = requester;
        }

        public static Behavior<String> create(ActorRef<String> requester) {
            return Behaviors.setup(param -> new Adaptor(param, requester));
        }

        @Override
        public Receive<String> createReceive() {
            return newReceiveBuilder()
                    .onMessage(String.class, msg -> {

                        // forward to original requester
                        originalRequester.tell(msg);

                        // destroy
                        cancellable.cancel();
                        return Behaviors.stopped();
                    })
                    .build();
        }
    }
}
