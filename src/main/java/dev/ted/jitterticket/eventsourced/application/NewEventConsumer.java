package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public abstract class NewEventConsumer {

    private final Map<Class<? extends Event>, MethodHandle> eventToHandleMethod = new HashMap<>();

    public NewEventConsumer() {
        Method[] declaredMethods = this.getClass().getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            if (declaredMethod.getName().equals("handle")) {
                Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
                if (parameterTypes.length == 1) {
                    Class<?> handleParameterType = parameterTypes[0];
                    if (Event.class.isAssignableFrom(handleParameterType)) {
                        try {
                            MethodHandle methodHandle = MethodHandles.lookup().unreflect(declaredMethod);
                            eventToHandleMethod.put((Class<? extends Event>) handleParameterType, methodHandle);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    public void handle(Stream<? extends Event> stream) {
        stream.forEach(this::invokeForEvent);
    }

    private void invokeForEvent(Event event) {
        if (eventToHandleMethod.containsKey(event.getClass())) {
            MethodHandle methodHandle = eventToHandleMethod.get(event.getClass());
            try {
                methodHandle.invoke(this, event);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new UnwantedEventException(event, eventToHandleMethod.keySet());
        }
    }

    public Set<Class<? extends Event>> handledEventTypes() {
        return eventToHandleMethod.keySet();
    }
}
