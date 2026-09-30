package com.algorithmic.music.domain.model;

import java.util.Objects;

/**
 * Immutable Domain Record representing an atomic musical note event.
 * Encapsulates pitch, dynamic velocity, rhythmic duration, channel assignment,
 * and scheduled temporal position.
 *
 * @param voice         The polyphonic voice role generating this event.
 * @param channel       Zero-based MIDI channel [0..15].
 * @param pitch         Pitch value object (or Pitch.REST).
 * @param velocity      Dynamic intensity.
 * @param duration      Rhythmic duration.
 * @param scheduledTick Global sequencer tick on which this note starts.
 */
public record NoteEvent(
    VoiceType voice,
    int channel,
    Pitch pitch,
    Velocity velocity,
    Duration duration,
    long scheduledTick
) {

    public NoteEvent {
        Objects.requireNonNull(voice, "voice must not be null");
        Objects.requireNonNull(pitch, "pitch must not be null");
        Objects.requireNonNull(velocity, "velocity must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        if (channel < 0 || channel > 15) {
            throw new IllegalArgumentException("MIDI channel must be [0..15], got: " + channel);
        }
    }

    public static NoteEvent of(VoiceType voice, Pitch pitch, Velocity velocity, Duration duration, long scheduledTick) {
        return new NoteEvent(voice, voice.defaultChannel(), pitch, velocity, duration, scheduledTick);
    }

    public static NoteEvent rest(VoiceType voice, Duration duration, long scheduledTick) {
        return new NoteEvent(voice, voice.defaultChannel(), Pitch.REST, Velocity.SILENT, duration, scheduledTick);
    }

    public boolean isRest() {
        return pitch.isRest() || velocity.value() == 0;
    }

    /**
     * Creates a copy with an adjusted velocity (e.g. for groove accentuation).
     */
    public NoteEvent withVelocity(Velocity newVelocity) {
        return new NoteEvent(voice, channel, pitch, newVelocity, duration, scheduledTick);
    }

    @Override
    public String toString() {
        if (isRest()) {
            return String.format("[%s|CH%d] REST (%s) @ tick %d", voice, channel + 1, duration, scheduledTick);
        }
        return String.format("[%s|CH%d] %s (%dHz) %s %s @ tick %d",
            voice, channel + 1, pitch, Math.round(pitch.frequencyHz()), velocity, duration, scheduledTick);
    }
}
