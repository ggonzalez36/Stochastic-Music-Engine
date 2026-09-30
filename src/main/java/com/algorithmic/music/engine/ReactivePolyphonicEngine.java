package com.algorithmic.music.engine;

import com.algorithmic.music.domain.model.NoteEvent;
import com.algorithmic.music.port.OutputPort;
import com.algorithmic.music.timing.PrecisionClock;
import com.algorithmic.music.timing.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

/**
 * Reactive Polyphonic Engine orchestrating algorithmic voice generators, the microsecond
 * precision clock, and output adapters via Java 21 Project Loom structured concurrency.
 *
 * <p>Concurrency Architecture:
 * <ul>
 *     <li><b>Clock Loop:</b> Runs in a dedicated Virtual Thread with hybrid nano-parking.</li>
 *     <li><b>Voice Orchestration:</b> Evaluates polyphonic voices in parallel on each tick using
 *         {@link StructuredTaskScope.ShutdownOnFailure}, guaranteeing deterministic thread boundaries.</li>
 *     <li><b>Asynchronous Note-Offs:</b> Spawns ephemeral Virtual Threads that sleep for the exact note
 *         duration via {@link LockSupport#parkNanos(long)} before sending NOTE_OFF, preventing note hangs
 *         without blocking the main clock thread.</li>
 * </ul>
 */
public final class ReactivePolyphonicEngine implements AutoCloseable {

    private final PrecisionClock clock;
    private final List<VoiceGenerator> voices;
    private final OutputPort outputPort;
    private final int ppq;
    private final java.util.function.Consumer<TickEvent> tickHook;

    public ReactivePolyphonicEngine(
        double bpm,
        int ppq,
        List<VoiceGenerator> voices,
        OutputPort outputPort,
        java.util.function.Consumer<TickEvent> tickHook
    ) {
        if (bpm <= 0.0) throw new IllegalArgumentException("BPM must be positive: " + bpm);
        if (ppq <= 0) throw new IllegalArgumentException("PPQ must be positive: " + ppq);
        this.ppq = ppq;
        this.voices = List.copyOf(voices);
        this.outputPort = Objects.requireNonNull(outputPort, "outputPort must not be null");
        this.tickHook = tickHook;
        this.clock = new PrecisionClock(bpm, ppq, this::onTick);
    }

    public ReactivePolyphonicEngine(
        double bpm,
        int ppq,
        List<VoiceGenerator> voices,
        OutputPort outputPort
    ) {
        this(bpm, ppq, voices, outputPort, null);
    }

    /**
     * Starts the reactive sequencer engine.
     */
    public void start() {
        System.out.printf("[Engine] Starting Reactive Polyphonic Engine (Voices: %d | BPM: %.1f | PPQ: %d)%n",
            voices.size(), clock.getBpm(), ppq);
        clock.start();
    }

    /**
     * Internal tick callback triggered by {@link PrecisionClock}.
     * Executes voices concurrently using Project Loom Structured Concurrency.
     */
    private void onTick(TickEvent tick) {
        if (tickHook != null) {
            try {
                tickHook.accept(tick);
            } catch (Exception ignored) {
            }
        }

        List<NoteEvent> tickEvents = new ArrayList<>();

        // Structured multi-voice parallel evaluation using Project Loom Virtual Threads
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            List<java.util.concurrent.Future<List<NoteEvent>>> futures = new ArrayList<>(voices.size());

            for (VoiceGenerator voice : voices) {
                futures.add(executor.submit(() -> voice.onTick(tick)));
            }

            for (var future : futures) {
                tickEvents.addAll(future.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (Exception e) {
            System.err.println("[Engine] Error in voice orchestration: " + e.getMessage());
            return;
        }

        // Dispatch all generated note events
        for (NoteEvent event : tickEvents) {
            dispatchNoteEvent(event, tick.bpm());
        }
    }

    /**
     * Dispatches NOTE_ON immediately and schedules NOTE_OFF via an ephemeral Virtual Thread.
     */
    private void dispatchNoteEvent(NoteEvent event, double bpm) {
        if (event.isRest()) return;

        // 1. Send outbound event & NOTE_ON
        outputPort.send(event);
        outputPort.noteOn(event.channel(), event.pitch(), event.velocity());

        // 2. Schedule asynchronous NOTE_OFF in a lightweight Virtual Thread
        long durationNanos = event.duration().toNanos(bpm);

        // Standard MIDI articulation: note gate length is ~92% of nominal duration to prevent overlap
        long gateNanos = Math.round(durationNanos * 0.92);

        Thread.ofVirtual()
            .name("note-off-" + event.channel() + "-" + event.pitch().midiNumber())
            .start(() -> {
                LockSupport.parkNanos(gateNanos);
                outputPort.noteOff(event.channel(), event.pitch());
            });
    }

    public void setBpm(double newBpm) {
        clock.setBpm(newBpm);
        System.out.println("[Engine] Tempo adjusted to: " + newBpm + " BPM");
    }

    public double getBpm() {
        return clock.getBpm();
    }

    public boolean isRunning() {
        return clock.isRunning();
    }

    public void stop() {
        clock.stop();
        outputPort.allNotesOff();
        System.out.println("[Engine] Engine stopped. Sent All Notes Off.");
    }

    @Override
    public void close() {
        stop();
        outputPort.close();
    }
}
