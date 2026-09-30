package com.algorithmic.music.port;

import com.algorithmic.music.domain.model.NoteEvent;
import com.algorithmic.music.domain.model.Pitch;
import com.algorithmic.music.domain.model.Velocity;

/**
 * Primary Outbound Port (Hexagonal Architecture) for rendering musical events.
 * Decouples algorithmic generation completely from physical output protocols (MIDI, OSC, Audio Synthesizer, Mock).
 */
public interface OutputPort extends AutoCloseable {

    /**
     * Dispatches a scheduled musical note event.
     */
    void send(NoteEvent event);

    /**
     * Sends an immediate raw NOTE_ON message to the target channel.
     *
     * @param channel  Zero-based channel index [0..15].
     * @param pitch    Target pitch value object.
     * @param velocity Attack dynamic velocity [0..127].
     */
    void noteOn(int channel, Pitch pitch, Velocity velocity);

    /**
     * Sends an immediate raw NOTE_OFF message to terminate sound on the target channel.
     *
     * @param channel Zero-based channel index [0..15].
     * @param pitch   Target pitch value object.
     */
    void noteOff(int channel, Pitch pitch);

    /**
     * Sends panic / All Notes Off (CC 123) control change across all 16 MIDI channels.
     */
    void allNotesOff();

    @Override
    void close();
}
