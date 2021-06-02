package com.example.ask;

import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.RemoteActor;

import java.time.Duration;

// Proxy actor and Remote actor must have the same protocol
public class Proxy extends AbstractBehavior<RemoteActor.SayHello> {

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

                    // Get remote actor
                    ActorSelection selection = getContext().classicActorContext().actorSelection("akka://ROOT@localhost:25520/user/helloakka");

                    selection.tell(
                            new RemoteActor.SayHello(
                                    msg.name,
                                    // this line is important!
                                    getContext().spawn(Proxy.Adaptor.create(msg.ref), uidGenerate(counter++))
                            ),
                            getContext().classicActorContext().self());

                    return this;
                })
                .build();
    }

    // generate unique name for temporary actor
    // should return a value has one-to-one relation with original request data (ex: requestId)
    private String uidGenerate(Object seed) {
        return  "tmp-adaptor-" + seed;
    }

    // temporary actor: a new actor will be spawned for each income request
    private static class Adaptor extends AbstractBehavior<String> {

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
                        System.out.println(getContext().getSelf().path().toString() + " received response " + msg);
                        originalRequester.tell(msg);

                        // destroy
                        cancellable.cancel();
                        return Behaviors.stopped();
                    })
                    .build();
        }
    }
}
