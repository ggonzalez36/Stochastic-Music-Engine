package com.algorithmic.music.timing;

/**
 * High-resolution sequencer tick notification event.
 *
 * @param tickIndex      Monotonically increasing tick counter (0, 1, 2, ...).
 * @param scheduledNanos Scheduled epoch timestamp according to absolute mathematical grid.
 * @param actualNanos    Actual dispatch timestamp sampled with System.nanoTime().
 * @param bpm            Current tempo in beats per minute.
 * @param ppq            Pulses (ticks) per quarter note (e.g., 4 for 16th-note grid).
 */
public record TickEvent(
    long tickIndex,
    long scheduledNanos,
    long actualNanos,
    double bpm,
    int ppq
) {

    /**
     * Latency jitter: discrepancy between scheduled tick time and actual execution time in nanoseconds.
     */
    public long jitterNanos() {
        return actualNanos - scheduledNanos;
    }

    /**
     * Latency jitter in microseconds.
     */
    public double jitterMicros() {
        return jitterNanos() / 1_000.0;
    }

    /**
     * Beat index within the current measure (e.g., 0.0, 0.25, 0.5, 0.75 in 4/4 meter with 16th subdivisions).
     */
    public double beatPosition() {
        return (double) (tickIndex % (ppq * 4)) / ppq;
    }

    /**
     * Checks if this tick falls on a quarter-note downbeat (quarter beat boundary).
     */
    public boolean isDownbeat() {
        return (tickIndex % ppq) == 0;
    }

    /**
     * Checks if this tick falls on the first beat of a 4/4 measure (bar start).
     */
    public boolean isBarStart() {
        return (tickIndex % (ppq * 4)) == 0;
    }
}
