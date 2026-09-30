package com.algorithmic.music.domain.model;

/**
 * Value Object representing MIDI note velocity (dynamic intensity / volume),
 * bounded to [0..127].
 *
 * @param value MIDI velocity integer [0..127].
 */
public record Velocity(int value) implements Comparable<Velocity> {

    public static final Velocity SILENT = new Velocity(0);
    public static final Velocity PPP = new Velocity(16);
    public static final Velocity PP = new Velocity(32);
    public static final Velocity P = new Velocity(48);
    public static final Velocity MP = new Velocity(64);
    public static final Velocity MF = new Velocity(80);
    public static final Velocity F = new Velocity(96);
    public static final Velocity FF = new Velocity(112);
    public static final Velocity FFF = new Velocity(127);

    public Velocity {
        if (value < 0 || value > 127) {
            throw new IllegalArgumentException("Velocity must be between 0 and 127 inclusive, got: " + value);
        }
    }

    public static Velocity of(int val) {
        return new Velocity(Math.clamp(val, 0, 127));
    }

    /**
     * Normalized linear amplitude in range [0.0..1.0].
     */
    public double normalized() {
        return value / 127.0;
    }

    /**
     * Applies scaling factor (e.g. 0.8 for accent attenuation or 1.2 for accentuation).
     */
    public Velocity scale(double factor) {
        int scaled = (int) Math.round(this.value * factor);
        return Velocity.of(scaled);
    }

    @Override
    public int compareTo(Velocity o) {
        return Integer.compare(this.value, o.value);
    }

    @Override
    public String toString() {
        return "Vel(" + value + ")";
    }
}
