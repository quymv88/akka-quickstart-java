package com.example;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
public class Server {
    public static void main(String[] args) {

        ActorSystem.create(Behaviors.setup(context -> {
            context.spawn(RemoteActor.create(), "helloakka");
            return Behaviors.empty();
        }), "ROOT");
    }

}
