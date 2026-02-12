package com.obsdl.common.util;

import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {

    private static final AtomicLong COUNTER = new AtomicLong(1000);

    private IdGenerator() {
    }

    public static long nextId() {
        return COUNTER.incrementAndGet();
    }
}
