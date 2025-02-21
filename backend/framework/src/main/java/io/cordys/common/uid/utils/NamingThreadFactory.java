
package io.cordys.common.uid.utils;

import io.cordys.common.util.LogUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Named thread in ThreadFactory. If there is no specified name for thread, it
 * will auto detect using the invoker classname instead.
 */
public class NamingThreadFactory implements ThreadFactory {
    /**
     * Thread name pre
     * -- GETTER --
     * Getters & Setters
     */
    @Setter
    @Getter
    private String name;
    /**
     * Is daemon thread
     */
    private final boolean daemon;
    /**
     * UncaughtExceptionHandler
     */
    private final UncaughtExceptionHandler uncaughtExceptionHandler;
    /**
     * Sequences for multi thread name prefix
     */
    private final ConcurrentHashMap<String, AtomicLong> sequences;

    /**
     * Constructors
     */
    public NamingThreadFactory() {
        this(null, false, null);
    }

    public NamingThreadFactory(String name) {
        this(name, false, null);
    }

    public NamingThreadFactory(String name, boolean daemon) {
        this(name, daemon, null);
    }

    public NamingThreadFactory(String name, boolean daemon, UncaughtExceptionHandler handler) {
        this.name = name;
        this.daemon = daemon;
        this.uncaughtExceptionHandler = handler;
        this.sequences = new ConcurrentHashMap<String, AtomicLong>();
    }

    @Override
    public Thread newThread(@NotNull Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(this.daemon);

        // If there is no specified name for thread, it will auto detect using the invoker classname instead.
        // Notice that auto detect may cause some performance overhead
        String prefix = this.name;
        if (StringUtils.isBlank(prefix)) {
            prefix = getInvoker();
        }
        thread.setName(prefix + "-" + getSequence(prefix));

        // no specified uncaughtExceptionHandler, just do logging.
        thread.setUncaughtExceptionHandler(Objects.requireNonNullElseGet(this.uncaughtExceptionHandler, () -> (t, e) -> LogUtils.error("unhandled exception in thread: " + t.getName(), e)));

        return thread;
    }

    /**
     * Get the method invoker's class name
     *
     * @return
     */
    private String getInvoker() {
        Exception e = new Exception();
        StackTraceElement[] sites = e.getStackTrace();
        if (sites.length > 2) {
            return ClassUtils.getShortClassName(sites[2].getClassName());
        }
        return getClass().getSimpleName();
    }

    /**
     * Get sequence for different naming prefix
     *
     * @param invoker
     * @return
     */
    private long getSequence(String invoker) {
        AtomicLong r = this.sequences.get(invoker);
        if (r == null) {
            r = new AtomicLong(0);
            AtomicLong previous = this.sequences.putIfAbsent(invoker, r);
            if (previous != null) {
                r = previous;
            }
        }

        return r.incrementAndGet();
    }

}
