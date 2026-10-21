package com.uwumic;

/** Shared state between the UI and the streaming service. */
final class State {
    static final int RATE = 48000;
    static volatile boolean running;
    static volatile boolean muted;
    static volatile float gain = 1f;
    static volatile int declick = 50; // click removal strength, 0 = off
    static volatile float level;
    static volatile int clients;
    static volatile String error = "";

    private State() { }
}
