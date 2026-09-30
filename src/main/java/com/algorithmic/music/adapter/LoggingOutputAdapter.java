package com.algorithmic.music.adapter;

import com.algorithmic.music.domain.model.NoteEvent;
import com.algorithmic.music.domain.model.Pitch;
import com.algorithmic.music.domain.model.Velocity;
import com.algorithmic.music.port.OutputPort;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Tracing & Diagnostic Output Adapter implementing {@link OutputPort}.
 *
 * <p>Designed for headless environments, unit testing, and latency benchmarking.
 * Logs high-resolution event dispatches with real-time jitter statistics without
 * requiring physical or virtual audio hardware.
 */
public final class LoggingOutputAdapter implements OutputPort {

    private final boolean verbose;
    private final LongAdder dispatchedCount = new LongAdder();
    private final DoubleAdder totalJitterMicros = new DoubleAdder();
    private final AtomicLong maxJitterNanos = new AtomicLong(0);

    public LoggingOutputAdapter(boolean verbose) {
        this.verbose = verbose;
    }

    public LoggingOutputAdapter() {
        this(true);
    }

    @Override
    public void send(NoteEvent event) {
        dispatchedCount.increment();
        if (verbose) {
            System.out.printf("[TRACE | OUT] %s%n", event);
        }
    }

    @Override
    public void noteOn(int channel, Pitch pitch, Velocity velocity) {
        dispatchedCount.increment();
        if (verbose) {
            System.out.printf("[TRACE | NOTE_ON ] Ch: %2d | Pitch: %-4s (MIDI %3d, %6.1fHz) | Vel: %3d%n",
                channel + 1, pitch.noteName(), pitch.midiNumber(), pitch.frequencyHz(), velocity.value());
        }
    }

    @Override
    public void noteOff(int channel, Pitch pitch) {
        if (verbose) {
            System.out.printf("[TRACE | NOTE_OFF] Ch: %2d | Pitch: %-4s (MIDI %3d)%n",
                channel + 1, pitch.noteName(), pitch.midiNumber());
        }
    }

    @Override
    public void allNotesOff() {
        System.out.println("[TRACE | PANIC   ] Sent All Notes Off across all channels");
    }

    public void recordJitter(long jitterNanos) {
        double micros = jitterNanos / 1_000.0;
        totalJitterMicros.add(micros);
        maxJitterNanos.accumulateAndGet(Math.abs(jitterNanos), Math::max);
    }

    public long getDispatchedCount() {
        return dispatchedCount.sum();
    }

    public double getAverageJitterMicros() {
        long count = dispatchedCount.sum();
        return count == 0 ? 0.0 : totalJitterMicros.sum() / count;
    }

    public double getMaxJitterMicros() {
        return maxJitterNanos.get() / 1_000.0;
    }

    @Override
    public void close() {
        System.out.printf("[LoggingOutputAdapter] Summary: %d events dispatched | Avg Jitter: %.2f µs | Max Jitter: %.2f µs%n",
            dispatchedCount.sum(), getAverageJitterMicros(), getMaxJitterMicros());
    }
}
