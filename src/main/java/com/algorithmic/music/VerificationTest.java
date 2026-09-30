package com.algorithmic.music;

import com.algorithmic.music.domain.markov.AliasMethod;
import com.algorithmic.music.domain.markov.MarkovChainOrder2;
import com.algorithmic.music.domain.markov.ScaleCorpus;
import com.algorithmic.music.domain.model.Pitch;
import com.algorithmic.music.domain.model.TransitionState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verification test suite for mathematical core (Order-2 Markov Chain and Vose's Alias Method).
 */
public final class VerificationTest {

    public static void main(String[] args) {
        System.out.println(">>> RUNNING MATHEMATICAL CORE VERIFICATION TESTS...\n");

        testAliasMethodEquiprobable();
        testAliasMethodSkewedDistribution();
        testOrder2MarkovDeterminismAndBackoff();
        testCorpusTrainingAndStability();

        System.out.println("\n>>> ALL MATHEMATICAL VERIFICATION TESTS PASSED SUCCESSFULLY!");
    }

    private static void testAliasMethodEquiprobable() {
        System.out.print("[TEST] Vose's Alias Method (Equiprobable)... ");
        Map<String, Double> weights = Map.of("A", 1.0, "B", 1.0, "C", 1.0, "D", 1.0);
        AliasMethod<String> sampler = AliasMethod.fromWeights(weights);

        int iterations = 100_000;
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < iterations; i++) {
            counts.merge(sampler.sample(), 1, Integer::sum);
        }

        // Each bucket should have approx 25,000 hits (+/- 3%)
        for (String key : weights.keySet()) {
            double ratio = (double) counts.get(key) / iterations;
            if (ratio < 0.23 || ratio > 0.27) {
                throw new AssertionError("Equiprobable distribution violated for " + key + ": " + ratio);
            }
        }
        System.out.println("PASSED (uniform distribution within 3% tolerance).");
    }

    private static void testAliasMethodSkewedDistribution() {
        System.out.print("[TEST] Vose's Alias Method (Heavily Skewed 90/10)... ");
        Map<String, Double> weights = Map.of("COMMON", 9.0, "RARE", 1.0);
        AliasMethod<String> sampler = AliasMethod.fromWeights(weights);

        int iterations = 100_000;
        int commonCount = 0;
        for (int i = 0; i < iterations; i++) {
            if ("COMMON".equals(sampler.sample())) {
                commonCount++;
            }
        }

        double commonRatio = (double) commonCount / iterations;
        if (commonRatio < 0.88 || commonRatio > 0.92) {
            throw new AssertionError("Skewed distribution violated: " + commonRatio);
        }
        System.out.println("PASSED (empirical ratio " + String.format("%.4f", commonRatio) + " ~= 0.9000).");
    }

    private static void testOrder2MarkovDeterminismAndBackoff() {
        System.out.print("[TEST] Order-2 Markov Chain (Transitions & Katz Backoff)... ");
        MarkovChainOrder2<Pitch> chain = new MarkovChainOrder2<>();

        Pitch c4 = Pitch.of("C", 4);
        Pitch d4 = Pitch.of("D", 4);
        Pitch e4 = Pitch.of("E", 4);
        Pitch f4 = Pitch.of("F", 4);

        // Sequence: C4 -> D4 -> E4 -> F4 -> C4
        chain.train(List.of(c4, d4, e4, f4, c4));
        chain.compile();

        // Conditioned on (C4, D4), only E4 was observed -> must deterministically yield E4
        Pitch outcome = chain.next(c4, d4);
        if (!outcome.equals(e4)) {
            throw new AssertionError("Expected E4 from (C4, D4), got: " + outcome);
        }

        // Conditioned on (D4, E4), only F4 was observed -> must deterministically yield F4
        outcome = chain.next(d4, e4);
        if (!outcome.equals(f4)) {
            throw new AssertionError("Expected F4 from (D4, E4), got: " + outcome);
        }

        // Novel unobserved Order-2 pair (F4, D4) -> Backoff Level 1 must examine D4 -> E4
        outcome = chain.next(f4, d4);
        if (!outcome.equals(e4)) {
            throw new AssertionError("Expected backoff to yield E4 from D4, got: " + outcome);
        }

        System.out.println("PASSED (Order-2 transitions and Level-1 backoff verified).");
    }

    private static void testCorpusTrainingAndStability() {
        System.out.print("[TEST] Melodic Techno Corpus Stability (100,000 step continuous walk)... ");
        MarkovChainOrder2<Pitch> chain = new MarkovChainOrder2<>();
        chain.train(ScaleCorpus.melodicTechnoLeadCorpus());
        chain.compile();

        TransitionState<Pitch> state = TransitionState.of(Pitch.of("D", 4), Pitch.of("F", 4));
        for (int i = 0; i < 100_000; i++) {
            Pitch next = chain.next(state);
            if (next == null || next.isRest()) {
                throw new AssertionError("Null or invalid pitch produced at step " + i);
            }
            state = state.shift(next);
        }
        System.out.println("PASSED (100,000 steps executed without deadlocks or exceptions).");
    }
}
