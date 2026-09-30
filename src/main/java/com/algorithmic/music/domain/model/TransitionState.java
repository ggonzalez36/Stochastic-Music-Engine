package com.algorithmic.music.domain.model;

import java.util.Objects;

/**
 * Immutable Representation of a 2nd-Order Markov State Tuple (S_{t-2}, S_{t-1}).
 * Provides compound context for predicting the subsequent state S_t.
 *
 * @param <T> Domain type of the state (e.g., Pitch, Duration, or composite note).
 * @param stateA State at time t-2.
 * @param stateB State at time t-1.
 */
public record TransitionState<T>(T stateA, T stateB) {

    public TransitionState {
        Objects.requireNonNull(stateA, "stateA must not be null");
        Objects.requireNonNull(stateB, "stateB must not be null");
    }

    public static <T> TransitionState<T> of(T stateA, T stateB) {
        return new TransitionState<>(stateA, stateB);
    }

    /**
     * Shifts window by dropping stateA, promoting stateB to position A, and adopting nextState as position B.
     * (S_{t-2}, S_{t-1}) + S_t -> (S_{t-1}, S_t)
     */
    public TransitionState<T> shift(T nextState) {
        return new TransitionState<>(this.stateB, nextState);
    }

    @Override
    public String toString() {
        return "(" + stateA + " -> " + stateB + ")";
    }
}
