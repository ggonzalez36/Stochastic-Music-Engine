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
 * Aggressive Acid 303 / Peak-Time Techno Synth Generator.
 *
 * <p>Implements authentic Roland TB-303 / Moog style acid sequences:
 * <ul>
 *     <li><b>D Phrygian / Industrial Dark Scale:</b> Menacing half-step tension (D <-> Eb).</li>
 *     <li><b>Resonant Squelch Accents:</b> Sudden velocity spikes (120-127) simulating 303 resonance.</li>
 *     <li><b>Relentless 16th Drive:</b> Constant hypnotic momentum that sweeps open through buildups.</li>
 * </ul>
 */
public final class LeadVoiceGenerator implements VoiceGenerator {

    private final MarkovChainOrder2<Pitch> markovChain;
    private final TransitionState<Pitch> initialContext;
    private final TechnoSongConductor conductor;
    private TransitionState<Pitch> currentContext;

    public LeadVoiceGenerator(
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

    public LeadVoiceGenerator(MarkovChainOrder2<Pitch> markovChain, Pitch seed1, Pitch seed2) {
        this(markovChain, seed1, seed2, null);
    }

    @Override
    public VoiceType voiceType() {
        return VoiceType.LEAD;
    }

    @Override
    public List<NoteEvent> onTick(TickEvent tick) {
        if (conductor != null && !conductor.isLeadActive(tick.tickIndex(), tick.ppq())) {
            return List.of();
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        double cutoff = (conductor != null)
            ? conductor.getFilterCutoff(tick.tickIndex(), tick.ppq())
            : 0.85;

        // In peak techno, the acid line is almost 100% continuous (driving 16ths)
        if (random.nextDouble() > (0.88 + (0.10 * cutoff))) {
            return List.of();
        }

        // Query Order-2 Markov Chain for next acid note
        Pitch nextPitch = markovChain.next(currentContext);
        currentContext = currentContext.shift(nextPitch);

        // 303 Resonant Accent Modeling:
        // Steps 3, 6, 11, 14 have high probability of screaming acid accents
        int step = (int) (tick.tickIndex() % (tick.ppq() * 4));
        boolean isAccentStep = (step == 3 || step == 6 || step == 11 || step == 14);

        int baseVel = (int) Math.round(72 + (cutoff * 44));
        if (isAccentStep && random.nextDouble() < 0.65) {
            baseVel = 124; // Squelching 303 Accent!
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
