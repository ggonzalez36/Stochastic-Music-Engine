package com.algorithmic.music.domain.model;

/**
 * Value Object representing a musical duration in rhythmic ticks and beat subdivisions.
 * Provides conversions between musical meter, ticks, beats, and physical nanoseconds.
 *
 * @param ticks Number of sequencer ticks (at sequencer PPQ resolution).
 * @param beats Floating-point beat length (1.0 = Quarter note in 4/4 meter).
 */
public record Duration(int ticks, double beats) {

    public static final Duration SIXTEENTH = new Duration(1, 0.25);
    public static final Duration EIGHTH = new Duration(2, 0.50);
    public static final Duration DOTTED_EIGHTH = new Duration(3, 0.75);
    public static final Duration QUARTER = new Duration(4, 1.00);
    public static final Duration DOTTED_QUARTER = new Duration(6, 1.50);
    public static final Duration HALF = new Duration(8, 2.00);
    public static final Duration WHOLE = new Duration(16, 4.00);

    public Duration {
        if (ticks < 1) {
            throw new IllegalArgumentException("Duration ticks must be >= 1, got: " + ticks);
        }
        if (beats <= 0.0) {
            throw new IllegalArgumentException("Duration beats must be > 0.0, got: " + beats);
        }
    }

    public static Duration ofTicks(int ticks, int ppq) {
        double beats = (double) ticks / ppq;
        return new Duration(ticks, beats);
    }

    public static Duration ofBeats(double beats, int ppq) {
        int ticks = Math.max(1, (int) Math.round(beats * ppq));
        return new Duration(ticks, beats);
    }

    /**
     * Converts musical duration to physical nanoseconds given the current tempo in BPM.
     *
     * @param bpm Beats per minute (e.g. 130.0).
     * @return Duration in nanoseconds.
     */
    public long toNanos(double bpm) {
        if (bpm <= 0.0) throw new IllegalArgumentException("BPM must be positive: " + bpm);
        // Nanoseconds per beat = (60 * 1_000_000_000) / bpm
        double nanosPerBeat = 60_000_000_000.0 / bpm;
        return Math.round(beats * nanosPerBeat);
    }

    /**
     * Converts musical duration to physical milliseconds.
     */
    public double toMillis(double bpm) {
        return toNanos(bpm) / 1_000_000.0;
    }

    @Override
    public String toString() {
        return "Duration[" + ticks + " ticks, " + beats + " beats]";
    }
}
