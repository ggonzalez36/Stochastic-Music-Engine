package com.algorithmic.music.domain.model;

import java.util.Objects;

/**
 * Value Object representing a musical pitch in the Western 12-TET tuning system (A4 = 440Hz).
 * Backed by an immutable MIDI note number [0..127].
 *
 * @param midiNumber MIDI note number between 0 and 127 inclusive, or -1 representing a REST.
 */
public record Pitch(int midiNumber) implements Comparable<Pitch> {

    public static final int REST_MIDI = -1;
    public static final Pitch REST = new Pitch(REST_MIDI);

    private static final String[] NOTE_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    public Pitch {
        if (midiNumber != REST_MIDI && (midiNumber < 0 || midiNumber > 127)) {
            throw new IllegalArgumentException("MIDI note number must be between 0 and 127, or -1 for REST. Got: " + midiNumber);
        }
    }

    public static Pitch of(int midiNumber) {
        return midiNumber == REST_MIDI ? REST : new Pitch(midiNumber);
    }

    public static Pitch of(String noteName, int octave) {
        Objects.requireNonNull(noteName, "noteName must not be null");
        String cleanName = noteName.trim().toUpperCase();
        int noteIndex = -1;
        for (int i = 0; i < NOTE_NAMES.length; i++) {
            if (NOTE_NAMES[i].equals(cleanName) || matchEnharmonic(cleanName, i)) {
                noteIndex = i;
                break;
            }
        }
        if (noteIndex == -1) {
            throw new IllegalArgumentException("Unknown note name: " + noteName);
        }
        int midi = (octave + 1) * 12 + noteIndex;
        return of(midi);
    }

    private static boolean matchEnharmonic(String name, int noteIndex) {
        return switch (name) {
            case "DB" -> noteIndex == 1;
            case "EB" -> noteIndex == 3;
            case "FB" -> noteIndex == 4;
            case "E#" -> noteIndex == 5;
            case "GB" -> noteIndex == 6;
            case "AB" -> noteIndex == 8;
            case "BB" -> noteIndex == 10;
            case "CB" -> noteIndex == 11;
            case "B#" -> noteIndex == 0;
            default -> false;
        };
    }

    public boolean isRest() {
        return midiNumber == REST_MIDI;
    }

    /**
     * Calculates the fundamental frequency in Hertz using equal temperament (A4 = 440 Hz).
     */
    public double frequencyHz() {
        if (isRest()) return 0.0;
        return 440.0 * Math.pow(2.0, (midiNumber - 69) / 12.0);
    }

    /**
     * Pitch class: 0 = C, 1 = C#, ..., 11 = B.
     */
    public int pitchClass() {
        if (isRest()) return -1;
        return midiNumber % 12;
    }

    /**
     * Octave index in standard scientific pitch notation (C4 = middle C, MIDI 60).
     */
    public int octave() {
        if (isRest()) return -1;
        return (midiNumber / 12) - 1;
    }

    /**
     * Formats note name (e.g., "C4", "G#2", "REST").
     */
    public String noteName() {
        if (isRest()) return "REST";
        return NOTE_NAMES[pitchClass()] + octave();
    }

    /**
     * Transposes pitch by semitones, clamping to valid MIDI bounds [0..127].
     */
    public Pitch transpose(int semitones) {
        if (isRest()) return this;
        int target = Math.clamp(midiNumber + semitones, 0, 127);
        return Pitch.of(target);
    }

    @Override
    public int compareTo(Pitch o) {
        return Integer.compare(this.midiNumber, o.midiNumber);
    }

    @Override
    public String toString() {
        return noteName();
    }
}
