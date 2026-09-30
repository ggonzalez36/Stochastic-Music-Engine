package com.algorithmic.music.adapter;

import com.algorithmic.music.domain.model.NoteEvent;
import com.algorithmic.music.domain.model.Pitch;
import com.algorithmic.music.domain.model.Velocity;
import com.algorithmic.music.port.OutputPort;

import java.util.List;
import java.util.Objects;

/**
 * Composite Adapter that broadcasts outbound musical events to multiple {@link OutputPort} destinations
 * (e.g. simultaneously sending to a DAW MIDI bus and high-resolution telemetry logger).
 */
public final class CompositeOutputAdapter implements OutputPort {

    private final List<OutputPort> ports;

    public CompositeOutputAdapter(List<OutputPort> ports) {
        Objects.requireNonNull(ports, "ports must not be null");
        this.ports = List.copyOf(ports);
    }

    public static CompositeOutputAdapter of(OutputPort... ports) {
        return new CompositeOutputAdapter(List.of(ports));
    }

    @Override
    public void send(NoteEvent event) {
        for (OutputPort p : ports) {
            try {
                p.send(event);
            } catch (Exception e) {
                System.err.println("[CompositeOutputAdapter] Error in port.send: " + e.getMessage());
            }
        }
    }

    @Override
    public void noteOn(int channel, Pitch pitch, Velocity velocity) {
        for (OutputPort p : ports) {
            try {
                p.noteOn(channel, pitch, velocity);
            } catch (Exception e) {
                System.err.println("[CompositeOutputAdapter] Error in port.noteOn: " + e.getMessage());
            }
        }
    }

    @Override
    public void noteOff(int channel, Pitch pitch) {
        for (OutputPort p : ports) {
            try {
                p.noteOff(channel, pitch);
            } catch (Exception e) {
                System.err.println("[CompositeOutputAdapter] Error in port.noteOff: " + e.getMessage());
            }
        }
    }

    @Override
    public void allNotesOff() {
        for (OutputPort p : ports) {
            try {
                p.allNotesOff();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void close() {
        for (OutputPort p : ports) {
            try {
                p.close();
            } catch (Exception ignored) {
            }
        }
    }
}
