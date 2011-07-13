// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.jumi.core.actors;

import java.util.*;
import java.util.concurrent.*;

public class Actors {
    private final ListenerFactory<?>[] factories;
    private final Set<Thread> actorThreads = Collections.synchronizedSet(new HashSet<Thread>());
    private final ExecutorService unattendedWorkers = Executors.newCachedThreadPool();
    private final ThreadLocal<MessageQueue<Event<?>>> queueOfCurrentActor = new ThreadLocal<MessageQueue<Event<?>>>();

    public Actors(ListenerFactory<?>... factories) {
        this.factories = factories;
    }

    public <T> T startEventPoller(Class<T> type, T target, String name) {
        ListenerFactory<T> factory = getFactoryForType(type);

        MessageQueue<Event<T>> queue = new MessageQueue<Event<T>>();
        MessageSender<Event<T>> receiver = factory.newBackend(target);
        T handle = factory.newFrontend(queue);

        startActorThread(name, queue, new EventPoller<T>(queue, receiver));
        return type.cast(handle);
    }

    private <T> void startActorThread(String name, MessageQueue<Event<T>> queue, EventPoller<T> actor) {
        Thread t = new Thread(new ActorContext<T>(queue, actor), name);
        t.start();
        actorThreads.add(t);
    }

    public void startUnattendedWorker(Runnable worker, Runnable onFinished) {
        Runnable onFinishedHandle = bindSecondaryInterface(Runnable.class, onFinished);
        unattendedWorkers.execute(new UnattendedWorker(worker, onFinishedHandle));
    }

    public <T> T bindSecondaryInterface(Class<T> type, final T target) {
        ListenerFactory<T> factory = getFactoryForType(type);
        final MessageQueue<Event<?>> queue = getQueueOfCurrentActor();

        T handle = factory.newFrontend(new MessageSender<Event<T>>() {
            public void send(Event<T> message) {
                queue.send(new CustomTargetEvent<T>(message, target));
            }
        });
        return type.cast(handle);
    }

    private MessageQueue<Event<?>> getQueueOfCurrentActor() {
        MessageQueue<Event<?>> queue = queueOfCurrentActor.get();
        if (queue == null) {
            throw new IllegalStateException("queue not set up; maybe we are not inside an actor?");
        }
        return queue;
    }

    @SuppressWarnings({"unchecked"})
    private <T> ListenerFactory<T> getFactoryForType(Class<T> type) {
        for (ListenerFactory<?> factory : factories) {
            if (factory.getType().equals(type)) {
                return (ListenerFactory<T>) factory;
            }
        }
        throw new IllegalArgumentException("unsupported listener type: " + type);
    }

    public void shutdown(long timeout) throws InterruptedException {
        for (Thread t : actorThreads) {
            t.interrupt();
        }
        unattendedWorkers.shutdown();
        for (Thread t : actorThreads) {
            t.join(timeout);
        }
        unattendedWorkers.awaitTermination(timeout, TimeUnit.MILLISECONDS);
    }


    private class ActorContext<T> implements Runnable {
        private final MessageQueue<Event<T>> queue;
        private final Runnable actor;

        public ActorContext(MessageQueue<Event<T>> queue, Runnable actor) {
            this.actor = actor;
            this.queue = queue;
        }

        @SuppressWarnings({"unchecked"})
        public void run() {
            queueOfCurrentActor.set((MessageQueue) queue);
            try {
                actor.run();
            } finally {
                queueOfCurrentActor.remove();
            }
        }
    }

    private class EventPoller<T> implements Runnable {
        private final MessageReceiver<Event<T>> events;
        private final MessageSender<Event<T>> target;

        public EventPoller(MessageReceiver<Event<T>> events, MessageSender<Event<T>> target) {
            this.events = events;
            this.target = target;
        }

        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Event<T> message = events.take();
                    target.send(message);
                }
            } catch (InterruptedException e) {
                // actor was told to exit
            }
        }
    }

    private static class UnattendedWorker implements Runnable {
        private final Runnable worker;
        private final Runnable onFinished;

        public UnattendedWorker(Runnable worker, Runnable onFinished) {
            this.worker = worker;
            this.onFinished = onFinished;
        }

        public void run() {
            try {
                worker.run();
            } finally {
                onFinished.run();
            }
        }
    }

    private static class CustomTargetEvent<T> implements Event<Object> {
        private final Event<T> message;
        private final T target;

        public CustomTargetEvent(Event<T> message, T target) {
            this.message = message;
            this.target = target;
        }

        public void fireOn(Object ignored) {
            // TODO: double-check that we are on the right thread?
            message.fireOn(target);
        }
    }
}
