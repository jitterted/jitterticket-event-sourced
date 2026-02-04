package dev.ted.jitterticket.eventsourced.domain;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public abstract class Event {
    protected Long eventSequence;

    protected Event(Long eventSequence) {
        this.eventSequence = eventSequence;
    }

    public static Set<Class<?>> allConcreteImplementationsOf(Class<?> sealedClass) {
        Set<Class<?>> result = new HashSet<>();

        if (!sealedClass.isInterface() && !Modifier.isAbstract(sealedClass.getModifiers())) {
            // This is a concrete class, add it
            result.add(sealedClass);
            return result;
        }

        // Get direct permitted subclasses/interfaces
        Class<?>[] permittedSubclasses = sealedClass.getPermittedSubclasses();
        if (permittedSubclasses == null) {
            return result;
        }

        // Recursively process each permitted subclass/interface
        for (Class<?> subclass : permittedSubclasses) {
            if (!subclass.isInterface() && !Modifier.isAbstract(subclass.getModifiers())) {
                // This is a concrete class, add it
                result.add(subclass);
            } else {
                // This is an interface or abstract class, recurse into it
                result.addAll(allConcreteImplementationsOf(subclass));
            }
        }

        return result;
    }

    public Long eventSequence() {
        return eventSequence;
    }

    public void setEventSequence(Long eventSequence) {
        this.eventSequence = eventSequence;
    }
}
