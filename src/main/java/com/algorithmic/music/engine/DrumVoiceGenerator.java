package com.algorithmic.music.engine;

import com.algorithmic.music.domain.model.Duration;
import com.algorithmic.music.domain.model.NoteEvent;
import com.algorithmic.music.domain.model.Pitch;
import com.algorithmic.music.domain.model.Velocity;
import com.algorithmic.music.domain.model.VoiceType;
import com.algorithmic.music.song.TechnoSongConductor;
import com.algorithmic.music.timing.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hard-hitting Peak-Time / Industrial Techno Drum Kit Generator (General MIDI Channel 10).
 *
 * <p>Implements authentic warehouse techno drum elements:
 * <ul>
 *     <li><b>Crushing 909 Kick (MIDI 36):</b> Relentless 4-on-the-floor at maximum velocity 127.</li>
 *     <li><b>Reverberated Sub-Rumble (MIDI 41):</b> Low-frequency 16th rumble behind the kick.</li>
 *     <li><b>Searing Offbeat 909 Open Hat (MIDI 46):</b> Massive offbeat driving pulse on steps 2, 6, 10, 14.</li>
 *     <li><b>Layered Clap & Snare (MIDI 39 & 38):</b> Heavy backbeat on 4 and 12.</li>
 *     <li><b>Industrial Ride Cymbal (MIDI 51):</b> Relentless offbeat energy during peak drops.</li>
 * </ul>
 */
public final class DrumVoiceGenerator implements VoiceGenerator {

    public static final Pitch KICK        = Pitch.of(36); // Bass Drum 1
    public static final Pitch SUB_RUMBLE  = Pitch.of(41); // Low Floor Tom (Used for Kick Sub-Rumble)
    public static final Pitch CLAP        = Pitch.of(39); // Hand Clap
    public static final Pitch SNARE       = Pitch.of(38); // Acoustic Snare
    public static final Pitch CLOSED_HAT  = Pitch.of(42); // Closed Hi-Hat / 16th Shaker
    public static final Pitch OPEN_HAT    = Pitch.of(46); // Searing 909 Open Hi-Hat
    public static final Pitch RIDE_CYMBAL = Pitch.of(51); // Ride Cymbal

    private final TechnoSongConductor conductor;

    public DrumVoiceGenerator(TechnoSongConductor conductor) {
        this.conductor = conductor;
    }

    public DrumVoiceGenerator() {
        this(null);
    }

    @Override
    public VoiceType voiceType() {
        return VoiceType.DRUMS;
    }

    @Override
    public List<NoteEvent> onTick(TickEvent tick) {
        int stepInBar = (int) (tick.tickIndex() % (tick.ppq() * 4)); // 0..15 in a 16-tick bar
        List<NoteEvent> events = new ArrayList<>(5);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        boolean kickAllowed = (conductor == null) || conductor.isKickActive(tick.tickIndex(), tick.ppq());
        boolean clapAllowed = (conductor == null) || conductor.isClapActive(tick.tickIndex(), tick.ppq());
        boolean snareRiser = (conductor != null) && conductor.isSnareRiserActive(tick.tickIndex(), tick.ppq());

        // 1. Crushing 909 Kick: 4-on-the-floor
        boolean isKickStep = (stepInBar % tick.ppq()) == 0;
        if (isKickStep && kickAllowed) {
            events.add(new NoteEvent(
                voiceType(),
                channel(),
                KICK,
                Velocity.of(127), // Maximum punch
                Duration.SIXTEENTH,
                tick.tickIndex()
            ));
        }

        // 2. Techno Sub-Rumble (16th-note sub rumble on steps 1, 2, 3, etc.)
        if (kickAllowed && (stepInBar % 2 != 0)) {
            events.add(new NoteEvent(
                voiceType(),
                channel(),
                SUB_RUMBLE,
                Velocity.of(88 + random.nextInt(-3, 4)),
                Duration.SIXTEENTH,
                tick.tickIndex()
            ));
        }

        // 3. Layered Clap & Snare: Backbeats on steps 4 and 12
        boolean isBackbeat = (stepInBar == 4 || stepInBar == 12);
        if (isBackbeat && clapAllowed) {
            events.add(new NoteEvent(
                voiceType(),
                channel(),
                CLAP,
                Velocity.of(124),
                Duration.SIXTEENTH,
                tick.tickIndex()
            ));
            events.add(new NoteEvent(
                voiceType(),
                channel(),
                SNARE,
                Velocity.of(105),
                Duration.SIXTEENTH,
                tick.tickIndex()
            ));
        }

        // 4. Snare Build-up Riser before drops
        if (snareRiser) {
            // Rapid accelerating snare rolls on 16ths
            events.add(new NoteEvent(
                voiceType(),
                channel(),
                SNARE,
                Velocity.of(90 + (stepInBar * 2)),
                Duration.SIXTEENTH,
                tick.tickIndex()
            ));
        }

        // 5. Searing 909 Offbeat Open Hi-Hat (Steps 2, 6, 10, 14)
        boolean isOffbeatEighth = (stepInBar == 2 || stepInBar == 6 || stepInBar == 10 || stepInBar == 14);
        if (isOffbeatEighth && kickAllowed) {
            events.add(new NoteEvent(
                voiceType(),
                channel(),
                OPEN_HAT,
                Velocity.of(118 + random.nextInt(-3, 4)),
                Duration.EIGHTH,
                tick.tickIndex()
            ));

            // Heavy Ride Cymbal on drops for peak warehouse energy
            if (conductor != null && conductor.currentSection(tick.tickIndex(), tick.ppq()).targetEnergy() >= 0.95) {
                events.add(new NoteEvent(
                    voiceType(),
                    channel(),
                    RIDE_CYMBAL,
                    Velocity.of(96 + random.nextInt(-4, 5)),
                    Duration.SIXTEENTH,
                    tick.tickIndex()
                ));
            }
        } else {
            // 6. Driving 16th Closed Hi-Hats
            int hatVel = (stepInBar % 2 == 0) ? 82 : 68;
            events.add(new NoteEvent(
                voiceType(),
                channel(),
                CLOSED_HAT,
                Velocity.of(hatVel + random.nextInt(-3, 4)),
                Duration.SIXTEENTH,
                tick.tickIndex()
            ));
        }

        return events;
    }

    @Override
    public void reset() {}
}
