package com.algorithmic.music.domain.markov;

import com.algorithmic.music.domain.model.Pitch;

import java.util.List;

/**
 * Curated modal training corpora for Peak-Time, Driving, and Industrial Techno.
 * Features D Phrygian (D, Eb, F, G, A, Bb, C), 303 Acid riffs, warehouse stabs,
 * and relentless sub-rumble bass patterns inspired by Drumcode, Charlotte de Witte,
 * Enrico Sangiuliano, and Klangkuenstler.
 */
public final class ScaleCorpus {

    private ScaleCorpus() {}

    /**
     * Aggressive 303 Acid & Peak-Time Lead Corpus (D Phrygian / Industrial Minor, Octaves 3-5).
     * Relentless 16th-note acid sequences, dissonant half-steps (D <-> Eb), tritones (Ab),
     * and sudden octave leaps.
     */
    public static List<Pitch> peakTimeAcidCorpus() {
        return List.of(
            // Riff 1: Classic Acid 303 Phrygian Ostinato (D <-> Eb tension)
            Pitch.of("D", 3), Pitch.of("D", 4), Pitch.of("Eb", 4), Pitch.of("D", 4),
            Pitch.of("F", 4), Pitch.of("D", 4), Pitch.of("Eb", 4), Pitch.of("D", 3),
            Pitch.of("D", 4), Pitch.of("C", 4), Pitch.of("D", 4), Pitch.of("Eb", 4),
            Pitch.of("F", 4), Pitch.of("G", 4), Pitch.of("F", 4), Pitch.of("Eb", 4),

            // Riff 2: Warehouse Screamer (Tritone Ab & High Octave D5)
            Pitch.of("D", 4), Pitch.of("Ab", 4), Pitch.of("G", 4), Pitch.of("F", 4),
            Pitch.of("Eb", 4), Pitch.of("D", 4), Pitch.of("D", 5), Pitch.of("C", 5),
            Pitch.of("A", 4), Pitch.of("F", 4), Pitch.of("Eb", 4), Pitch.of("D", 4),
            Pitch.of("Eb", 4), Pitch.of("D", 4), Pitch.of("C", 4), Pitch.of("D", 3),

            // Riff 3: High Energy Gallop & Pitch Slides
            Pitch.of("D", 4), Pitch.of("F", 4), Pitch.of("D", 4), Pitch.of("A", 4),
            Pitch.of("D", 5), Pitch.of("Eb", 5), Pitch.of("D", 5), Pitch.of("C", 5),
            Pitch.of("Bb", 4), Pitch.of("A", 4), Pitch.of("G", 4), Pitch.of("F", 4),
            Pitch.of("Eb", 4), Pitch.of("D", 4), Pitch.of("Eb", 4), Pitch.of("D", 3)
        );
    }

    /**
     * Driving Heavy Techno Rumble & Rolling Bass Corpus (D Phrygian, Octaves 1-2).
     * Relentless sub-bass momentum with kick-glue rumble on 35Hz - 65Hz.
     */
    public static List<Pitch> peakTimeRumbleBassCorpus() {
        return List.of(
            // Motive 1: Driving Root D with Octave Gallop & Phrygian Eb snap
            Pitch.of("D", 1), Pitch.of("D", 1), Pitch.of("D", 2), Pitch.of("D", 1),
            Pitch.of("D", 1), Pitch.of("Eb", 1), Pitch.of("D", 1), Pitch.of("D", 2),
            Pitch.of("D", 1), Pitch.of("D", 1), Pitch.of("F", 1), Pitch.of("D", 1),
            Pitch.of("Eb", 1), Pitch.of("D", 1), Pitch.of("C", 1), Pitch.of("D", 1),

            // Motive 2: Sub-rumble locomotive
            Pitch.of("D", 1), Pitch.of("D", 2), Pitch.of("D", 1), Pitch.of("D", 2),
            Pitch.of("Eb", 1), Pitch.of("Eb", 2), Pitch.of("D", 1), Pitch.of("D", 2),
            Pitch.of("F", 1), Pitch.of("D", 1), Pitch.of("Eb", 1), Pitch.of("D", 1),
            Pitch.of("C", 1), Pitch.of("D", 1), Pitch.of("D", 1), Pitch.of("D", 2)
        );
    }

    /**
     * Dark Industrial Rave Stabs & Dissonant Techno Chords.
     * Short, syncopated, menacing stabs designed to punch through the sub-bass.
     */
    public static List<List<Pitch>> industrialRaveStabs() {
        return List.of(
            // Dm(b5) / Tritone Industrial Stab (Dark, menancing)
            List.of(Pitch.of("D", 3), Pitch.of("F", 3), Pitch.of("Ab", 3), Pitch.of("C", 4)),
            // D Phrygian Dark Stab
            List.of(Pitch.of("D", 3), Pitch.of("Eb", 3), Pitch.of("A", 3), Pitch.of("D", 4)),
            // D Minor Power Stab
            List.of(Pitch.of("D", 3), Pitch.of("A", 3), Pitch.of("D", 4), Pitch.of("F", 4)),
            // Bb Industrial Lift
            List.of(Pitch.of("Bb", 2), Pitch.of("F", 3), Pitch.of("Bb", 3), Pitch.of("D", 4))
        );
    }

    // Retain Melodic Techno references for compatibility
    public static List<Pitch> melodicTechnoLeadCorpus() {
        return peakTimeAcidCorpus();
    }

    public static List<Pitch> melodicTechnoBassCorpus() {
        return peakTimeRumbleBassCorpus();
    }

    public static List<List<Pitch>> melodicTechnoPadChords() {
        return industrialRaveStabs();
    }
}
