package com.algorithmic.music.domain.model;

/**
 * Domain enumeration defining polyphonic voices, their musical roles, and target MIDI channels.
 * Note: MIDI channels are 0-indexed in Java Sound API [0..15] (Channel 0 = DAW Track 1, Channel 9 = General MIDI Drums).
 */
public enum VoiceType {
    LEAD(0, "Lead / Arpeggiator"),
    BASS(1, "Bassline"),
    DRUMS(9, "Drums / Percussion"),
    HARMONY(2, "Chords / Pad");

    private final int defaultChannel;
    private final String description;

    VoiceType(int defaultChannel, String description) {
        this.defaultChannel = defaultChannel;
        this.description = description;
    }

    public int defaultChannel() {
        return defaultChannel;
    }

    public String description() {
        return description;
    }
}
