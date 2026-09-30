package com.algorithmic.music.domain.markov;

import com.algorithmic.music.domain.model.TransitionState;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance Order-2 Markov Chain (Cadena de Markov de Orden 2).
 *
 * <p>Predicts subsequent state S_t conditioned on compound history (S_{t-2}, S_{t-1}),
 * avoiding the trivial harmonic wandering and memoryless loops of Order-1 chains.
 *
 * <p>Implements a hierarchical backoff strategy (Order 2 -> Order 1 -> Prior Vocabulary)
 * to guarantee continuous infinite generation without absorbing dead-ends or runtime exceptions.
 * Precomputes transition tables into {@link AliasMethod} instances for O(1) sampling time.
 *
 * @param <T> Domain type representing musical states (e.g. Pitch, Duration, or combined motifs).
 */
public final class MarkovChainOrder2<T> {

    private final Map<TransitionState<T>, TransitionDistribution<T>> order2Transitions = new ConcurrentHashMap<>();
    private final Map<T, TransitionDistribution<T>> order1Transitions = new ConcurrentHashMap<>();
    private final TransitionDistribution<T> priorVocabularyDistribution = new TransitionDistribution<>();

    /**
     * Trains the model using an observed sequential melody or chord progression.
     * Extracts Order-2 tuples (S_{t-2}, S_{t-1}) -> S_t, Order-1 pairs S_{t-1} -> S_t,
     * and accumulates prior vocabulary frequencies.
     *
     * @param sequence Sequence of states in temporal order.
     */
    public void train(List<T> sequence) {
        Objects.requireNonNull(sequence, "Sequence must not be null");
        if (sequence.size() < 3) {
            throw new IllegalArgumentException("Sequence must contain at least 3 elements for Order-2 training");
        }

        // Register prior frequencies for all elements
        for (T item : sequence) {
            priorVocabularyDistribution.record(item, 1.0);
        }

        // Register Order-1 transitions
        for (int i = 0; i < sequence.size() - 1; i++) {
            T current = sequence.get(i);
            T next = sequence.get(i + 1);
            order1Transitions
                .computeIfAbsent(current, _ -> new TransitionDistribution<>())
                .record(next, 1.0);
        }

        // Register Order-2 transitions
        for (int i = 0; i < sequence.size() - 2; i++) {
            T stPrev2 = sequence.get(i);
            T stPrev1 = sequence.get(i + 1);
            T stNext  = sequence.get(i + 2);

            TransitionState<T> stateKey = TransitionState.of(stPrev2, stPrev1);
            order2Transitions
                .computeIfAbsent(stateKey, _ -> new TransitionDistribution<>())
                .record(stNext, 1.0);
        }
    }

    /**
     * Injects a weighted transition manually, allowing fine-tuning of harmonic probabilities.
     */
    public void recordTransition(T stateA, T stateB, T outcome, double weight) {
        TransitionState<T> stateKey = TransitionState.of(stateA, stateB);
        order2Transitions
            .computeIfAbsent(stateKey, _ -> new TransitionDistribution<>())
            .record(outcome, weight);

        order1Transitions
            .computeIfAbsent(stateB, _ -> new TransitionDistribution<>())
            .record(outcome, weight);

        priorVocabularyDistribution.record(outcome, weight);
    }

    /**
     * Compiles all transition distributions into O(1) AliasMethod lookup structures.
     * Should be called after training and prior to real-time playback.
     */
    public void compile() {
        priorVocabularyDistribution.compile();
        order1Transitions.values().forEach(TransitionDistribution::compile);
        order2Transitions.values().forEach(TransitionDistribution::compile);
    }

    /**
     * Computes the next state S_t given compound context (S_{t-2}, S_{t-1}) using
     * Katz-style hierarchical backoff.
     *
     * <ol>
     *     <li>Level 2: Checks P(S_t | S_{t-2}, S_{t-1}). If match exists, samples in O(1).</li>
     *     <li>Level 1: Backs off to P(S_t | S_{t-1}). If match exists, samples in O(1).</li>
     *     <li>Level 0: Backs off to Prior distribution across all observed elements.</li>
     * </ol>
     *
     * @param context Compound history tuple (S_{t-2}, S_{t-1}).
     * @return Stochastically sampled next state S_t.
     */
    public T next(TransitionState<T> context) {
        Objects.requireNonNull(context, "context must not be null");

        // Level 2: Order-2 context lookup
        TransitionDistribution<T> dist2 = order2Transitions.get(context);
        if (dist2 != null && !dist2.isEmpty()) {
            return dist2.sample();
        }

        // Level 1: Order-1 backoff using stateB (S_{t-1})
        TransitionDistribution<T> dist1 = order1Transitions.get(context.stateB());
        if (dist1 != null && !dist1.isEmpty()) {
            return dist1.sample();
        }

        // Level 0: Global prior vocabulary backoff
        if (!priorVocabularyDistribution.isEmpty()) {
            return priorVocabularyDistribution.sample();
        }

        throw new IllegalStateException("Markov Chain has not been trained on any vocabulary");
    }

    /**
     * Convenience method to query next state with individual previous states.
     */
    public T next(T statePrev2, T statePrev1) {
        return next(TransitionState.of(statePrev2, statePrev1));
    }

    public int order2StateCount() {
        return order2Transitions.size();
    }

    public int vocabularySize() {
        return priorVocabularyDistribution.getRawWeights().size();
    }
}
