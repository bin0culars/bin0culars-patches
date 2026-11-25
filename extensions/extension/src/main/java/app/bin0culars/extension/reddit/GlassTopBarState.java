package app.bin0culars.extension.reddit;

import java.lang.reflect.Method;

final class GlassTopBarState {

    private static final Object LOCK = new Object();
    private static volatile Object feedAtTopState;
    private static volatile Method getValueMethod;
    private static volatile Method setValueMethod;
    private static volatile boolean initialized;

    private GlassTopBarState() {}

    static void onFeedAtTop(boolean atTop) {
        try {
            Object state = ensureState();
            Method setter = setValueMethod;
            if (state != null && setter != null) {
                setter.invoke(state, Boolean.valueOf(atTop));
            }
        } catch (Throwable ignored) {
        }
    }

    static boolean isFeedAtTop() {
        try {
            Object state = ensureState();
            Method getter = getValueMethod;
            if (state == null || getter == null) {
                return true;
            }
            Object value = getter.invoke(state);
            return !(value instanceof Boolean) || (Boolean) value;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static Object ensureState() {
        if (initialized) {
            return feedAtTopState;
        }
        synchronized (LOCK) {
            if (!initialized) {
                try {
                    Object state = createState(Boolean.TRUE);
                    if (state != null) {
                        Class<?> cls = state.getClass();
                        Method getter = cls.getMethod("getValue");
                        Method setter = cls.getMethod("setValue", Object.class);
                        getter.setAccessible(true);
                        setter.setAccessible(true);
                        getValueMethod = getter;
                        setValueMethod = setter;
                        feedAtTopState = state;
                    }
                } catch (Throwable ignored) {
                    feedAtTopState = null;
                    getValueMethod = null;
                    setValueMethod = null;
                } finally {
                    initialized = true;
                }
            }
            return feedAtTopState;
        }
    }

    private static Object createState(Object initial) {
        return null;
    }
}
