package com.algorithmic.music.domain.markov;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementation of Vose's Alias Method for O(1) time sampling from a discrete probability distribution.
 *
 * <p>Precomputes probability and alias tables in O(n) time. Once constructed, samples can be generated
 * in strict O(1) time with exactly two random number generations and a single comparison, guaranteeing
 * zero timing jitter in high-precision audio threads.</p>
 *
 * @param <T> Domain type to be sampled.
 */
public final class AliasMethod<T> {

    private final List<T> items;
    private final double[] probabilities;
    private final int[] aliases;
    private final int size;

    private AliasMethod(List<T> items, double[] probabilities, int[] aliases) {
        this.items = List.copyOf(items);
        this.probabilities = probabilities;
        this.aliases = aliases;
        this.size = items.size();
    }

    /**
     * Builds an AliasMethod sampler from a map of items to positive weights.
     *
     * @param weights Map of items to non-negative relative weights.
     * @param <T> Type of sampled item.
     * @return Initialized AliasMethod sampler.
     */
    public static <T> AliasMethod<T> fromWeights(Map<T, Double> weights) {
        Objects.requireNonNull(weights, "weights map must not be null");
        if (weights.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct AliasMethod with empty weights");
        }

        List<T> items = new ArrayList<>(weights.keySet());
        int n = items.size();
        double[] rawWeights = new double[n];
        double sum = 0.0;

        for (int i = 0; i < n; i++) {
            double w = weights.get(items.get(i));
            if (w < 0.0 || Double.isNaN(w)) {
                throw new IllegalArgumentException("Weight must be non-negative: " + w);
            }
            rawWeights[i] = w;
            sum += w;
        }

        if (sum <= 0.0) {
            throw new IllegalArgumentException("Sum of weights must be strictly positive: " + sum);
        }

        // Scale probabilities so that the average weight is 1.0 (prob = p_i * n / sum)
        double[] scaledProb = new double[n];
        Deque<Integer> small = new ArrayDeque<>(n);
        Deque<Integer> large = new ArrayDeque<>(n);

        for (int i = 0; i < n; i++) {
            scaledProb[i] = (rawWeights[i] * n) / sum;
            if (scaledProb[i] < 1.0) {
                small.push(i);
            } else {
                large.push(i);
            }
        }

        double[] prob = new double[n];
        int[] alias = new int[n];

        while (!small.isEmpty() && !large.isEmpty()) {
            int l = small.pop();
            int g = large.pop();

            prob[l] = scaledProb[l];
            alias[l] = g;

            scaledProb[g] = (scaledProb[g] + scaledProb[l]) - 1.0;
            if (scaledProb[g] < 1.0) {
                small.push(g);
            } else {
                large.push(g);
            }
        }

        // Numerical stability: remaining entries get probability 1.0
        while (!large.isEmpty()) {
            prob[large.pop()] = 1.0;
        }
        while (!small.isEmpty()) {
            prob[small.pop()] = 1.0;
        }

        return new AliasMethod<>(items, prob, alias);
    }

    /**
     * Samples an item in strict O(1) time.
     * Uses {@link ThreadLocalRandom} for zero lock-contention across Loom virtual threads.
     *
     * @return Stochastically selected item.
     */
    public T sample() {
        if (size == 1) {
            return items.getFirst();
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int column = random.nextInt(size);
        boolean coinToss = random.nextDouble() < probabilities[column];

        return coinToss ? items.get(column) : items.get(aliases[column]);
    }

    public int size() {
        return size;
    }
}
