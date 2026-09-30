package com.algorithmic.music.engine;

import com.algorithmic.music.domain.model.NoteEvent;
import com.algorithmic.music.domain.model.VoiceType;
import com.algorithmic.music.timing.TickEvent;

import java.util.List;

/**
 * Strategy interface defining an algorithmic musical voice generator.
 * Encapsulates the stochastic state machines, Markov chains, and rhythmic deciders for a single musical role.
 */
public interface VoiceGenerator {

    /**
     * Identifies the musical role and default routing of this voice.
     */
    VoiceType voiceType();

    /**
     * Target MIDI channel [0..15].
     */
    default int channel() {
        return voiceType().defaultChannel();
    }

    /**
     * Evaluates notes produced by this voice on the given sequencer tick.
     * Returns an empty list if the voice rests on this tick, or multiple notes in case of polyphonic chords/drum layers.
     *
     * @param tick Current tick event with timing and metric information.
     * @return List of generated NoteEvents for this tick.
     */
    List<NoteEvent> onTick(TickEvent tick);

    /**
     * Resets internal state machine or seeds to initial training state.
     */
    void reset();
}
