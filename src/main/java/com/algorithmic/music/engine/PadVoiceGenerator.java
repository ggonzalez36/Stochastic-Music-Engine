package com.algorithmic.music.engine;

import com.algorithmic.music.domain.markov.ScaleCorpus;
import com.algorithmic.music.domain.model.Duration;
import com.algorithmic.music.domain.model.NoteEvent;
import com.algorithmic.music.domain.model.Pitch;
import com.algorithmic.music.domain.model.Velocity;
import com.algorithmic.music.domain.model.VoiceType;
import com.algorithmic.music.song.SongSection;
import com.algorithmic.music.song.TechnoSongConductor;
import com.algorithmic.music.timing.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Industrial Rave Stab & Dark Drone Generator (MIDI Channel 3).
 *
 * <p>Produces:
 * <ul>
 *     <li><b>Syncopated Rave Stabs:</b> Sharp, dissonant chords on offbeats during drop sections.</li>
 *     <li><b>Industrial Drones:</b> Dark sustained pads during the breakdown.</li>
 * </ul>
 */
public final class PadVoiceGenerator implements VoiceGenerator {

    private final List<List<Pitch>> chordStabs;
    private final TechnoSongConductor conductor;
    private int currentStabIndex = 0;

    public PadVoiceGenerator(TechnoSongConductor conductor) {
        this.chordStabs = ScaleCorpus.industrialRaveStabs();
        this.conductor = conductor;
    }

    public PadVoiceGenerator() {
        this(null);
    }

    @Override
    public VoiceType voiceType() {
        return VoiceType.HARMONY;
    }

    @Override
    public List<NoteEvent> onTick(TickEvent tick) {
        int stepInBar = (int) (tick.tickIndex() % (tick.ppq() * 4));
        SongSection section = (conductor != null)
            ? conductor.currentSection(tick.tickIndex(), tick.ppq())
            : SongSection.DROP_1;

        // In BREAKDOWN: play dark sustained drone every 2 bars
        if (section == SongSection.BREAKDOWN || section == SongSection.INTRO) {
            int ticksPerDrone = tick.ppq() * 4 * 2;
            if ((tick.tickIndex() % ticksPerDrone) != 0) {
                return List.of();
            }

            List<Pitch> chord = chordStabs.get(currentStabIndex);
            currentStabIndex = (currentStabIndex + 1) % chordStabs.size();

            List<NoteEvent> events = new ArrayList<>(chord.size());
            Duration duration = Duration.ofTicks(ticksPerDrone - 2, tick.ppq());

            for (Pitch pitch : chord) {
                events.add(new NoteEvent(
                    voiceType(),
                    channel(),
                    pitch,
                    Velocity.of(78),
                    duration,
                    tick.tickIndex()
                ));
            }
            return events;
        }

        // In DROPS & GROOVE: play punchy, syncopated industrial rave stabs on steps 3 and 11
        boolean isStabStep = (stepInBar == 3 || stepInBar == 11);
        if (!isStabStep) {
            return List.of();
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        // 50% chance to stab for rhythmic syncopation
        if (random.nextDouble() > 0.60) {
            return List.of();
        }

        List<Pitch> stabPitches = chordStabs.get(currentStabIndex);
        if (random.nextDouble() < 0.35) {
            currentStabIndex = (currentStabIndex + 1) % chordStabs.size();
        }

        List<NoteEvent> events = new ArrayList<>(stabPitches.size());
        Duration duration = Duration.SIXTEENTH; // Short, punchy stab

        for (Pitch pitch : stabPitches) {
            int vel = 108 + random.nextInt(-4, 5);
            events.add(new NoteEvent(
                voiceType(),
                channel(),
                pitch,
                Velocity.of(vel),
                duration,
                tick.tickIndex()
            ));
        }

        return events;
    }

    @Override
    public void reset() {
        this.currentStabIndex = 0;
    }
}
