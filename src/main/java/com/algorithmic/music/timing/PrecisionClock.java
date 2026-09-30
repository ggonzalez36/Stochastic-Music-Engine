package com.algorithmic.music.timing;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * Microsecond-precision sequencer clock powered by Java 21 Project Loom.
 *
 * <p>Employs an absolute mathematical timeline reference (drift-free accumulator)
 * coupled with a hybrid sleep strategy:
 * <ol>
 *     <li>{@link LockSupport#parkNanos(long)} for the bulk coarse wait, freeing CPU cores.</li>
 *     <li>Short {@link Thread#onSpinWait()} spin-loop during the final 100 microseconds to eliminate
 *         OS scheduler quantum jitter and thread unparking latency.</li>
 * </ol>
 * Strictly avoids {@link Thread#sleep(long)}, ensuring zero timing drift across indefinite playback.
 */
public final class PrecisionClock implements AutoCloseable {

    private static final long PARK_THRESHOLD_NANOS = 150_000L; // 150 microseconds spin margin

    private final AtomicReference<Double> bpmReference;
    private final int ppq;
    private final Consumer<TickEvent> tickListener;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread clockThread;
    private long startNanos;
    private long tickCounter = 0;

    /**
     * @param initialBpm   Initial tempo in beats per minute (e.g. 130.0).
     * @param ppq          Pulses per quarter note (e.g. 4 for 16th-note ticks, or 24/96 for higher resolution).
     * @param tickListener Reactive listener invoked on every clock tick.
     */
    public PrecisionClock(double initialBpm, int ppq, Consumer<TickEvent> tickListener) {
        if (initialBpm <= 0.0) throw new IllegalArgumentException("BPM must be positive: " + initialBpm);
        if (ppq <= 0) throw new IllegalArgumentException("PPQ must be positive: " + ppq);
        this.bpmReference = new AtomicReference<>(initialBpm);
        this.ppq = ppq;
        this.tickListener = Objects.requireNonNull(tickListener, "tickListener must not be null");
    }

    /**
     * Spawns the dedicated precision clock loop inside a Project Loom Virtual Thread.
     */
    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            this.tickCounter = 0;
            this.startNanos = System.nanoTime();

            this.clockThread = Thread.ofVirtual()
                .name("loom-precision-clock")
                .start(this::runClockLoop);
        }
    }

    private void runClockLoop() {
        long currentNanosPerTick = computeNanosPerTick(bpmReference.get(), ppq);
        long targetNanos = startNanos;

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            double currentBpm = bpmReference.get();
            currentNanosPerTick = computeNanosPerTick(currentBpm, ppq);
            targetNanos += currentNanosPerTick;

            // Wait until targetNanos with hybrid coarse-park / fine-spin
            waitAccurate(targetNanos);

            long actualNanos = System.nanoTime();
            TickEvent event = new TickEvent(tickCounter++, targetNanos, actualNanos, currentBpm, ppq);

            try {
                tickListener.accept(event);
            } catch (Throwable t) {
                System.err.println("[PrecisionClock] Error in tick listener: " + t.getMessage());
            }
        }
    }

    /**
     * Hybrid wait: parks virtual thread until within safety window, then spins with CPU hints.
     */
    private void waitAccurate(long targetNanos) {
        while (running.get()) {
            long remainingNanos = targetNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                break;
            }

            if (remainingNanos > PARK_THRESHOLD_NANOS) {
                // Park virtual thread for the bulk of remaining time
                LockSupport.parkNanos(remainingNanos - PARK_THRESHOLD_NANOS);
            } else {
                // Microsecond fine alignment using CPU hint
                Thread.onSpinWait();
            }
        }
    }

    private static long computeNanosPerTick(double bpm, int ppq) {
        return Math.round(60_000_000_000.0 / (bpm * ppq));
    }

    /**
     * Dynamically updates the tempo in real time without stopping the sequencer.
     */
    public void setBpm(double newBpm) {
        if (newBpm <= 0.0) throw new IllegalArgumentException("BPM must be positive: " + newBpm);
        this.bpmReference.set(newBpm);
    }

    public double getBpm() {
        return bpmReference.get();
    }

    public boolean isRunning() {
        return running.get();
    }

    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            if (clockThread != null) {
                clockThread.interrupt();
            }
        }
    }

    @Override
    public void close() {
        stop();
    }
}
