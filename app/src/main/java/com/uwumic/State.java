package com.uwumic;

/** Shared state between the UI and the streaming service. */
final class State {
    static final int RATE = 48000;
    static volatile boolean running;
    static volatile boolean muted;
    static volatile float gain = 1f;
    static volatile float level;
    static volatile int clients;
    static volatile long startedAt;
    static volatile String error = "";

    private State() { }
}
