package com.github.aqiu202.cron.core;

public class ValueResult {
    public ValueResult(Integer value) {
        this(value, false);
    }

    public ValueResult(Integer value, boolean outOfBounds) {
        this.value = value;
        this.outOfBounds = outOfBounds;
    }

    private final Integer value;
    private final boolean outOfBounds;

    public Integer getValue() {
        return value;
    }

    public boolean isOutOfBounds() {
        return outOfBounds;
    }

}
