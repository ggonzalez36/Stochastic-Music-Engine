package com.algorithmic.music.domain.markov;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encapsulates the stochastic transition distribution from a given context state.
 * Supports dynamic frequency accumulation during training and compiles into an
 * immutable, zero-jitter {@link AliasMethod} for production audio streaming.
 *
 * @param <T> Domain type of the outcome state.
 */
public final class TransitionDistribution<T> {

    private final Map<T, Double> counts = new HashMap<>();
    private final AtomicBoolean compiled = new AtomicBoolean(false);
    private volatile AliasMethod<T> aliasSampler;

    public synchronized void record(T outcome, double weight) {
        if (weight <= 0.0) {
            throw new IllegalArgumentException("Weight must be positive: " + weight);
        }
        counts.merge(outcome, weight, Double::sum);
        compiled.set(false);
    }

    public synchronized void record(T outcome) {
        record(outcome, 1.0);
    }

    /**
     * Compiles accumulated transition counts into an O(1) AliasMethod sampler.
     */
    public synchronized void compile() {
        if (!compiled.get() && !counts.isEmpty()) {
            this.aliasSampler = AliasMethod.fromWeights(counts);
            this.compiled.set(true);
        }
    }

    /**
     * Samples the next state according to the learned empirical probabilities in O(1) time.
     */
    public T sample() {
        if (!compiled.get() || aliasSampler == null) {
            compile();
        }
        if (aliasSampler == null) {
            throw new IllegalStateException("TransitionDistribution is empty and cannot be sampled");
        }
        return aliasSampler.sample();
    }

    public boolean isEmpty() {
        return counts.isEmpty();
    }

    public Map<T, Double> getRawWeights() {
        return Collections.unmodifiableMap(counts);
    }

    @Override
    public String toString() {
        return counts.toString();
    }
}
