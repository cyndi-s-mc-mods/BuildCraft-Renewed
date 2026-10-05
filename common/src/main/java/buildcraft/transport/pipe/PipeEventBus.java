/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import buildcraft.api.transport.pipe.PipeEvent;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventPriority;

/** Sends pipe events to the methods of behaviours and flows annotated with {@link PipeEventHandler}. */
public class PipeEventBus {
    private static final Map<Class<?>, List<Handler>> ALL_HANDLERS = new ConcurrentHashMap<>();

    private final List<LocalHandler> currentHandlers = new ArrayList<>();

    private static List<Handler> getHandlers(Class<?> cls) {
        List<Handler> cached = ALL_HANDLERS.get(cls);
        if (cached != null) {
            return cached;
        }
        List<Handler> list = new ArrayList<>();
        Class<?> superCls = cls.getSuperclass();
        if (superCls != null) {
            list.addAll(getHandlers(superCls));
        }
        for (Method m : cls.getDeclaredMethods()) {
            PipeEventHandler annot = m.getAnnotation(PipeEventHandler.class);
            if (annot == null) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length != 1 || !PipeEvent.class.isAssignableFrom(params[0])) {
                throw new IllegalStateException("Cannot annotate " + m + " with @PipeEventHandler: it must take a single pipe event");
            }
            m.trySetAccessible();
            list.add(new Handler(annot.priority(), annot.receiveCancelled(), Modifier.isStatic(m.getModifiers()), m, params[0]));
        }
        ALL_HANDLERS.put(cls, list);
        return list;
    }

    public void registerHandler(Object obj) {
        for (Handler handler : getHandlers(obj.getClass())) {
            currentHandlers.add(new LocalHandler(handler, obj));
        }
        Collections.sort(currentHandlers);
    }

    public void unregisterHandler(Object obj) {
        currentHandlers.removeIf(next -> next.target == obj);
    }

    /** @return True if any handler received the event. */
    public boolean fireEvent(PipeEvent event) {
        boolean handled = false;
        for (LocalHandler handler : currentHandlers) {
            handled |= handler.handleEvent(event);
        }
        return handled;
    }

    private record Handler(PipeEventPriority priority, boolean receiveCanceled, boolean isStatic, Method method,
        Class<?> eventClass) {}

    private record LocalHandler(Handler handler, Object target) implements Comparable<LocalHandler> {
        boolean handleEvent(PipeEvent event) {
            if (!handler.receiveCanceled() && event.isCanceled()) {
                return false;
            }
            if (!handler.eventClass().isInstance(event)) {
                return false;
            }
            try {
                handler.method().invoke(handler.isStatic() ? null : target, event);
                return true;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException re) throw re;
                if (cause instanceof Error err) throw err;
                throw new IllegalStateException(cause);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public int compareTo(LocalHandler o) {
            return handler.priority().compareTo(o.handler.priority());
        }
    }
}
