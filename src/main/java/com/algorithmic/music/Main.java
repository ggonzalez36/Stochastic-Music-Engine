package com.algorithmic.music;

import com.algorithmic.music.adapter.CompositeOutputAdapter;
import com.algorithmic.music.adapter.JavaSoundMidiAdapter;
import com.algorithmic.music.adapter.LoggingOutputAdapter;
import com.algorithmic.music.domain.markov.MarkovChainOrder2;
import com.algorithmic.music.domain.markov.ScaleCorpus;
import com.algorithmic.music.domain.model.Pitch;
import com.algorithmic.music.engine.BassVoiceGenerator;
import com.algorithmic.music.engine.DrumVoiceGenerator;
import com.algorithmic.music.engine.LeadVoiceGenerator;
import com.algorithmic.music.engine.PadVoiceGenerator;
import com.algorithmic.music.engine.ReactivePolyphonicEngine;
import com.algorithmic.music.engine.VoiceGenerator;
import com.algorithmic.music.port.OutputPort;
import com.algorithmic.music.song.TechnoSongConductor;

import java.util.List;

/**
 * Main application entry point for the Peak-Time Acid & Industrial Techno Engine.
 *
 * <p>Genre: Peak-Time / Warehouse Driving Techno (Drumcode / Charlotte de Witte / Bodzin / Alignment aesthetic).
 * Key: D Phrygian / Industrial Dark Minor (133.0 BPM | 4/4 meter).
 * Concurrency: Java 25 LTS Project Loom Virtual Threads.
 */
public final class Main {

    private static final double DEFAULT_BPM = 133.0; // Definitive Peak-Time Driving Techno tempo
    private static final int PPQ = 4; // 4 ticks per beat = 16th-note sequencer grid

    public static void main(String[] args) {
        printBanner();

        System.out.println(">>> [1/4] INITIALIZING HEXAGONAL OUTPUT ADAPTERS...");
        LoggingOutputAdapter loggingAdapter = new LoggingOutputAdapter(true);
        OutputPort midiAdapter;

        try {
            midiAdapter = JavaSoundMidiAdapter.createAutoDetecting();
        } catch (Exception e) {
            System.err.println("[Main] Could not bind MIDI interface: " + e.getMessage() + ". Defaulting to Trace Logger only.");
            midiAdapter = loggingAdapter;
        }

        // Composite port broadcasting to both MIDI interface and real-time latency logger
        OutputPort compositePort = (midiAdapter == loggingAdapter)
            ? loggingAdapter
            : CompositeOutputAdapter.of(midiAdapter, loggingAdapter);

        System.out.println(">>> [2/4] TRAINING ORDER-2 MARKOV CHAINS (D PHRYGIAN 303 ACID & RUMBLE BASS)...");

        // 1. Train 303 Acid Lead Chain (Order-2 in D Phrygian / Minor)
        MarkovChainOrder2<Pitch> leadChain = new MarkovChainOrder2<>();
        List<Pitch> leadCorpus = ScaleCorpus.peakTimeAcidCorpus();
        leadChain.train(leadCorpus);
        leadChain.compile();
        System.out.printf("    - 303 Acid Lead: %d Order-2 states compiled | Vocabulary: %d pitches%n",
            leadChain.order2StateCount(), leadChain.vocabularySize());

        // 2. Train Heavy Rumble Bass Harmonic Chain (Order-2 in D Phrygian Sub Roots)
        MarkovChainOrder2<Pitch> bassChain = new MarkovChainOrder2<>();
        List<Pitch> bassCorpus = ScaleCorpus.peakTimeRumbleBassCorpus();
        bassChain.train(bassCorpus);
        bassChain.compile();
        System.out.printf("    - Rumble Bass:   %d Order-2 states compiled | Vocabulary: %d pitches%n",
            bassChain.order2StateCount(), bassChain.vocabularySize());

        System.out.println(">>> [3/4] ASSEMBLING 4-VOICE PEAK-TIME TECHNO ORCHESTRA...");

        TechnoSongConductor conductor = new TechnoSongConductor();

        LeadVoiceGenerator leadVoice = new LeadVoiceGenerator(
            leadChain,
            Pitch.of("D", 3),
            Pitch.of("D", 4),
            conductor
        );

        BassVoiceGenerator bassVoice = new BassVoiceGenerator(
            bassChain,
            Pitch.of("D", 1),
            Pitch.of("D", 2),
            conductor
        );

        PadVoiceGenerator stabVoice = new PadVoiceGenerator(conductor);
        DrumVoiceGenerator drumVoice = new DrumVoiceGenerator(conductor);

        List<VoiceGenerator> polyphonicVoices = List.of(leadVoice, bassVoice, stabVoice, drumVoice);

        // 4. Assemble Reactive Engine with live Song Conductor HUD hook
        ReactivePolyphonicEngine engine = new ReactivePolyphonicEngine(
            DEFAULT_BPM,
            PPQ,
            polyphonicVoices,
            compositePort,
            conductor::onTick
        );

        // Clean shutdown hook to prevent hung MIDI notes and close audio resources on SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n>>> [SHUTDOWN] Intercepted termination signal. Releasing resources...");
            engine.close();
            System.out.println(">>> [SHUTDOWN] Complete. All notes extinguished.");
        }));

        System.out.println(">>> [4/4] LAUNCHING PEAK-TIME TECHNO AUDIO STREAM (Press Ctrl+C to terminate)...");
        System.out.printf("    Tempo: %.1f BPM | Grid: 16th notes (4/4 meter) | Subdivisions: %d ticks/quarter%n", DEFAULT_BPM, PPQ);
        System.out.println("    Voices Active:");
        System.out.println("      * Channel 1: TB-303 Acid Synth (D Phrygian Squelch, Resonant Accents & Slides)");
        System.out.println("      * Channel 2: Heavy Rolling Bassline & Sub-Rumble (Lock-stepped 16th Gallop)");
        System.out.println("      * Channel 3: Industrial Rave Stabs & Dark Drones (Syncopated Offbeat Stabs)");
        System.out.println("      * Channel 10: Crushing 909 Techno Drums (Max Kick, Searing Offbeat Hat, Claps, Rides)");
        System.out.println("-------------------------------------------------------------------------------------");

        engine.start();

        // Optional timed demo mode or infinite stream
        long durationSeconds = -1;
        if (args.length > 0) {
            try {
                durationSeconds = Long.parseLong(args[0].replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {
            }
        }

        if (durationSeconds > 0) {
            System.out.printf(">>> [DEMO MODE] Running live Peak-Time Techno stream for %d seconds...%n", durationSeconds);
            java.util.concurrent.locks.LockSupport.parkNanos(durationSeconds * 1_000_000_000L);
            System.out.println("\n>>> [DEMO MODE] Duration elapsed. Gracefully closing engine...");
            engine.close();
            System.out.println(">>> [DEMO MODE] Execution finished successfully.");
        } else {
            // Keep main thread alive while virtual threads drive the sequencer
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void printBanner() {
        System.out.println("""
            =====================================================================================
             PEAK-TIME ACID TECHNO GENERATIVE ENGINE - JAVA 25 LTS & PROJECT LOOM
             TRACK: "Aethelgard - Warehouse Protocol" (Peak-Time Acid Techno Mix)
             Key: D Phrygian | 133.0 BPM | Heavy 909 Kick & Sub-Rumble | 303 Acid Sequences
             Domain-Driven Design (DDD) & Hexagonal Architecture
            =====================================================================================
            """);
    }
}
