package com.algorithmic.music.adapter;

import com.algorithmic.music.domain.model.NoteEvent;
import com.algorithmic.music.domain.model.Pitch;
import com.algorithmic.music.domain.model.Velocity;
import com.algorithmic.music.port.OutputPort;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Native MIDI Output Adapter implementing {@link OutputPort} using the low-level Java Sound API.
 *
 * <p>Dispatches raw MIDI bytes via {@link ShortMessage#NOTE_ON} and {@link ShortMessage#NOTE_OFF}.
 * Designed to connect seamlessly with virtual MIDI loopback cables (IAC Driver, LoopBe, loopMIDI, ALSA)
 * for direct multi-track feeding into professional DAWs (Ableton Live, Logic Pro, Reaper, Bitwig).
 *
 * <p>Gracefully falls back to the Java Runtime internal software synthesizer (Gervill / Windows Synth)
 * if no virtual MIDI bus is active, ensuring out-of-the-box audible playback.
 */
public final class JavaSoundMidiAdapter implements OutputPort {

    private static final String[] VIRTUAL_BUS_KEYWORDS = {
        "iac", "loopmidi", "loopbe", "virtual", "alsa", "internal", "midiview"
    };

    private final Receiver midiReceiver;
    private final MidiDevice boundDevice;
    private final String deviceName;

    /**
     * Initializes adapter by searching for virtual MIDI buses, or falling back to system default synth.
     */
    public static JavaSoundMidiAdapter createAutoDetecting() throws MidiUnavailableException {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        List<MidiDevice.Info> candidates = new ArrayList<>();

        for (MidiDevice.Info info : infos) {
            String name = info.getName().toLowerCase();
            String desc = info.getDescription().toLowerCase();
            for (String kw : VIRTUAL_BUS_KEYWORDS) {
                if (name.contains(kw) || desc.contains(kw)) {
                    candidates.add(info);
                    break;
                }
            }
        }

        // Try connecting to candidate virtual bus first
        for (MidiDevice.Info candidate : candidates) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(candidate);
                if (device.getMaxReceivers() != 0) { // Device can receive MIDI messages
                    device.open();
                    Receiver receiver = device.getReceiver();
                    return new JavaSoundMidiAdapter(receiver, device, candidate.getName());
                }
            } catch (Exception ignored) {
                // Try next candidate
            }
        }

        // Fallback: Open default Java Sound Receiver (e.g. Gervill / GS Synth)
        Receiver defaultReceiver = MidiSystem.getReceiver();
        return new JavaSoundMidiAdapter(defaultReceiver, null, "Default Java Sound Synthesizer Receiver");
    }

    /**
     * Initializes adapter targeting a specific named MIDI port.
     */
    public static Optional<JavaSoundMidiAdapter> createNamed(String preferredName) {
        try {
            for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
                if (info.getName().equalsIgnoreCase(preferredName)) {
                    MidiDevice dev = MidiSystem.getMidiDevice(info);
                    if (dev.getMaxReceivers() != 0) {
                        dev.open();
                        return Optional.of(new JavaSoundMidiAdapter(dev.getReceiver(), dev, info.getName()));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[JavaSoundMidiAdapter] Failed opening " + preferredName + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    private JavaSoundMidiAdapter(Receiver receiver, MidiDevice device, String deviceName) {
        this.midiReceiver = receiver;
        this.boundDevice = device;
        this.deviceName = deviceName;
        System.out.println("[JavaSoundMidiAdapter] Bound to MIDI endpoint: " + deviceName);
    }

    private final java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean(false);

    @Override
    public void send(NoteEvent event) {
        if (closed.get() || event.isRest()) return;
        noteOn(event.channel(), event.pitch(), event.velocity());
    }

    @Override
    public void noteOn(int channel, Pitch pitch, Velocity velocity) {
        if (closed.get() || pitch.isRest()) return;
        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.NOTE_ON, channel, pitch.midiNumber(), velocity.value());
            midiReceiver.send(msg, -1);
        } catch (Exception e) {
            if (!closed.get()) {
                System.err.println("[JavaSoundMidiAdapter] Error sending NOTE_ON: " + e.getMessage());
            }
        }
    }

    @Override
    public void noteOff(int channel, Pitch pitch) {
        if (closed.get() || pitch.isRest()) return;
        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.NOTE_OFF, channel, pitch.midiNumber(), 0);
            midiReceiver.send(msg, -1);
        } catch (Exception e) {
            if (!closed.get()) {
                System.err.println("[JavaSoundMidiAdapter] Error sending NOTE_OFF: " + e.getMessage());
            }
        }
    }

    @Override
    public void allNotesOff() {
        for (int ch = 0; ch < 16; ch++) {
            try {
                ShortMessage cc123 = new ShortMessage();
                cc123.setMessage(ShortMessage.CONTROL_CHANGE, ch, 123, 0); // All Notes Off
                midiReceiver.send(cc123, -1);

                ShortMessage cc120 = new ShortMessage();
                cc120.setMessage(ShortMessage.CONTROL_CHANGE, ch, 120, 0); // All Sound Off
                midiReceiver.send(cc120, -1);
            } catch (Exception ignored) {
            }
        }
    }

    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            allNotesOff();
            if (midiReceiver != null) {
                midiReceiver.close();
            }
            if (boundDevice != null && boundDevice.isOpen()) {
                boundDevice.close();
            }
            System.out.println("[JavaSoundMidiAdapter] Closed MIDI endpoint: " + deviceName);
        }
    }
}
