package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class RemoteActor extends AbstractBehavior<RemoteActor.SayHello> {

    public static class SayHello implements Protocol {

        public final ActorRef<String> ref;
        public final String name;

        public SayHello() {
            name = null;
            ref = null;
        }

        public SayHello(String name, ActorRef<String> ref) {
            this.name = name;
            this.ref = ref;
        }
    }

    public static Behavior<SayHello> create() {
        return Behaviors.setup(RemoteActor::new);
    }

    private RemoteActor(ActorContext<SayHello> context) {
        super(context);
    }

    @Override
    public Receive<SayHello> createReceive() {
        return newReceiveBuilder().onMessage(SayHello.class, this::onSayHello).build();
    }

    private Behavior<SayHello> onSayHello(SayHello command) {
        System.out.println("Remote Actor received a message");
        command.ref.tell(command.ref.path().toString());
        return this;
    }
}
