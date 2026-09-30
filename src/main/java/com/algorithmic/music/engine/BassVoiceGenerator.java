package com.algorithmic.music.engine;

import com.algorithmic.music.domain.markov.MarkovChainOrder2;
import com.algorithmic.music.domain.model.Duration;
import com.algorithmic.music.domain.model.NoteEvent;
import com.algorithmic.music.domain.model.Pitch;
import com.algorithmic.music.domain.model.TransitionState;
import com.algorithmic.music.domain.model.Velocity;
import com.algorithmic.music.domain.model.VoiceType;
import com.algorithmic.music.song.TechnoSongConductor;
import com.algorithmic.music.timing.TickEvent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Heavy Peak-Time Techno Rolling Bassline Generator.
 *
 * <p>Produces relentless, driving low-end rumble and saw bass motor:
 * <ul>
 *     <li><b>16th-note Rolling Train:</b> Relentless momentum locking into the 909 Kick.</li>
 *     <li><b>Offbeat 8th Drives (Steps 2, 6, 10, 14):</b> Maximum velocity impact.</li>
 *     <li><b>D Phrygian Sub Roots:</b> D1, Eb1, F1, C1 with octave leaps to D2.</li>
 * </ul>
 */
public final class BassVoiceGenerator implements VoiceGenerator {

    // Heavy Techno 16-step Rolling Bass Pattern
    private static final boolean[] PEAK_TECHNO_GRID = {
        false, true, true, true,   // Beat 1 (Kick transient hits on 0, Bass rolls on 1, 2, 3)
        false, true, true, true,   // Beat 2 (Kick transient hits on 4, Bass rolls on 5, 6, 7)
        false, true, true, true,   // Beat 3 (Kick transient hits on 8, Bass rolls on 9, 10, 11)
        false, true, true, true    // Beat 4 (Kick transient hits on 12, Bass rolls on 13, 14, 15)
    };

    private final MarkovChainOrder2<Pitch> markovChain;
    private final TransitionState<Pitch> initialContext;
    private final TechnoSongConductor conductor;
    private TransitionState<Pitch> currentContext;

    public BassVoiceGenerator(
        MarkovChainOrder2<Pitch> markovChain,
        Pitch seed1,
        Pitch seed2,
        TechnoSongConductor conductor
    ) {
        this.markovChain = Objects.requireNonNull(markovChain, "markovChain must not be null");
        this.initialContext = TransitionState.of(seed1, seed2);
        this.currentContext = this.initialContext;
        this.conductor = conductor;
    }

    public BassVoiceGenerator(MarkovChainOrder2<Pitch> markovChain, Pitch seed1, Pitch seed2) {
        this(markovChain, seed1, seed2, null);
    }

    @Override
    public VoiceType voiceType() {
        return VoiceType.BASS;
    }

    @Override
    public List<NoteEvent> onTick(TickEvent tick) {
        if (conductor != null && !conductor.isBassActive(tick.tickIndex(), tick.ppq())) {
            return List.of();
        }

        int barStep = (int) (tick.tickIndex() % PEAK_TECHNO_GRID.length);

        if (!PEAK_TECHNO_GRID[barStep]) {
            // Kick step: sub-bass glue pulse
            return List.of();
        }

        Pitch nextPitch = markovChain.next(currentContext);
        currentContext = currentContext.shift(nextPitch);

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Driving Rolling Techno Velocity
        int baseVel;
        if (barStep == 2 || barStep == 6 || barStep == 10 || barStep == 14) {
            baseVel = 120; // Massive offbeat drive!
        } else if (barStep == 3 || barStep == 7 || barStep == 11 || barStep == 15) {
            baseVel = 105; // 16th forward push
        } else {
            baseVel = 94;  // 16th gallop roll
        }

        Velocity velocity = Velocity.of(baseVel + random.nextInt(-3, 4));
        Duration duration = Duration.SIXTEENTH;

        NoteEvent event = new NoteEvent(
            voiceType(),
            channel(),
            nextPitch,
            velocity,
            duration,
            tick.tickIndex()
        );

        return List.of(event);
    }

    @Override
    public void reset() {
        this.currentContext = this.initialContext;
    }
}
