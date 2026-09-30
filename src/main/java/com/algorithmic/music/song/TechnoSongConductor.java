package com.algorithmic.music.song;

import com.algorithmic.music.timing.TickEvent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Conductor & Macro-Arrangement Director for the Melodic Techno Song.
 *
 * <p>Directs the compositional state machine across the 192-bar timeline:
 * <ul>
 *     <li>Automates track muting/unmuting (e.g. cutting the kick during the breakdown).</li>
 *     <li>Calculates progressive synthesizer filter sweep curves.</li>
 *     <li>Triggers snare build-up rushes before drops.</li>
 *     <li>Renders a live ASCII HUD showing song progression, bar counters, and energy levels.</li>
 * </ul>
 */
public final class TechnoSongConductor {

    public static final String TRACK_TITLE = "Aethelgard - Echoes of Eternity (Original Melodic Techno Mix)";
    public static final int TOTAL_SONG_BARS = 192;

    private final AtomicLong lastReportedBar = new AtomicLong(-1);

    /**
     * Retrieves the current song section for a given global tick.
     */
    public SongSection currentSection(long tickIndex, int ppq) {
        long currentBar = tickIndex / (ppq * 4);
        return SongSection.forBar(currentBar);
    }

    /**
     * Checks if the Kick drum is active.
     * Quintessential Melodic Techno technique: Kick completely cuts out during the Breakdown!
     */
    public boolean isKickActive(long tickIndex, int ppq) {
        SongSection section = currentSection(tickIndex, ppq);
        return switch (section) {
            case BREAKDOWN -> false; // Complete kick dropout during emotional breakdown!
            case OUTRO -> (tickIndex / (ppq * 4) % 192) < 184;
            default -> true; // 909 Kick hits from the very first beat!
        };
    }

    /**
     * Checks if the Rolling Bassline is active.
     */
    public boolean isBassActive(long tickIndex, int ppq) {
        SongSection section = currentSection(tickIndex, ppq);
        long bar = (tickIndex / (ppq * 4)) % 192;
        return switch (section) {
            case BREAKDOWN -> false; // Bass mutes in breakdown
            case INTRO -> bar >= 2;  // Rolling Bass enters on bar 2 (instant momentum)
            case OUTRO -> bar < 176;
            default -> true;
        };
    }

    /**
     * Checks if the Hypnotic Lead is active.
     */
    public boolean isLeadActive(long tickIndex, int ppq) {
        SongSection section = currentSection(tickIndex, ppq);
        long bar = (tickIndex / (ppq * 4)) % 192;
        return switch (section) {
            case INTRO -> bar >= 4; // 303 Acid line filters in from bar 4
            default -> true;
        };
    }

    /**
     * Checks if the Ethereal Chord Pads are active.
     */
    public boolean isPadActive(long tickIndex, int ppq) {
        SongSection section = currentSection(tickIndex, ppq);
        return switch (section) {
            case INTRO -> true;
            case BREAKDOWN -> true;
            case DROP_2_CLIMAX -> true;
            case OUTRO -> true;
            default -> false; // Ducked during pure rolling bass drops
        };
    }

    /**
     * Checks if the Hand Clap is active.
     */
    public boolean isClapActive(long tickIndex, int ppq) {
        SongSection section = currentSection(tickIndex, ppq);
        long bar = (tickIndex / (ppq * 4)) % 192;
        return switch (section) {
            case BREAKDOWN, OUTRO -> false;
            case INTRO -> bar >= 4; // Claps enter on bar 4
            default -> true;
        };
    }

    /**
     * Checks if a high-energy snare roll / riser is active during the last 4 bars of a buildup.
     */
    public boolean isSnareRiserActive(long tickIndex, int ppq) {
        long bar = tickIndex / (ppq * 4) % 192;
        boolean inBuildup1End = (bar >= 44 && bar < 48);
        boolean inBuildup2End = (bar >= 124 && bar < 128);
        return inBuildup1End || inBuildup2End;
    }

    /**
     * Calculates the normalized [0.0..1.0] filter cutoff envelope based on arrangement energy.
     */
    public double getFilterCutoff(long tickIndex, int ppq) {
        SongSection section = currentSection(tickIndex, ppq);
        long bar = tickIndex / (ppq * 4) % 192;
        double sectionProgress = (double) (bar - section.startBar()) / Math.max(1, section.totalBars());

        return switch (section) {
            case INTRO -> 0.25 + (0.15 * sectionProgress);
            case GROOVE_ENTRY -> 0.45 + (0.15 * sectionProgress);
            case BUILDUP_1, BUILDUP_2 -> 0.60 + (0.38 * sectionProgress); // Sweeps to 0.98 at drop!
            case DROP_1, DROP_2_CLIMAX -> 0.95; // Wide open screaming saw filter
            case BREAKDOWN -> 0.50 + (0.30 * sectionProgress); // Sweeping open over pads
            case OUTRO -> 0.60 * (1.0 - sectionProgress); // Filtering back down
        };
    }

    /**
     * Renders an interactive Terminal HUD when entering a new bar.
     */
    public void onTick(TickEvent tick) {
        if (!tick.isBarStart()) return;

        long currentBar = (tick.tickIndex() / (tick.ppq() * 4)) % TOTAL_SONG_BARS;
        long last = lastReportedBar.get();

        if (last != currentBar) {
            lastReportedBar.set(currentBar);
            SongSection section = SongSection.forBar(currentBar);

            // Print visual banner every 4 bars or on section changes
            if (currentBar == section.startBar() || (currentBar % 4 == 0)) {
                printArrangementHUD(currentBar, section, tick);
            }
        }
    }

    private void printArrangementHUD(long bar, SongSection section, TickEvent tick) {
        int progressBlocks = (int) Math.round(((double) bar / TOTAL_SONG_BARS) * 20);
        String progressBar = "=".repeat(progressBlocks) + "-".repeat(Math.max(0, 20 - progressBlocks));

        boolean kick = isKickActive(tick.tickIndex(), tick.ppq());
        boolean bass = isBassActive(tick.tickIndex(), tick.ppq());
        boolean lead = isLeadActive(tick.tickIndex(), tick.ppq());
        boolean pads = isPadActive(tick.tickIndex(), tick.ppq());
        boolean clap = isClapActive(tick.tickIndex(), tick.ppq());

        System.out.printf(
            "\n>>> [TRACK HUD] BAR %03d/%d [%s] | %s%n" +
            "    INSTRUMENTS: KICK [%s] | BASS [%s] | LEAD [%s] | PADS [%s] | CLAP [%s] | ENERGY: %3.0f%%%n" +
            "-------------------------------------------------------------------------------------%n",
            bar + 1, TOTAL_SONG_BARS, progressBar, section.description(),
            kick ? "ACTIVE" : " MUTED",
            bass ? "ACTIVE" : " MUTED",
            lead ? "ACTIVE" : " MUTED",
            pads ? "ACTIVE" : " MUTED",
            clap ? "ACTIVE" : " MUTED",
            section.targetEnergy() * 100.0
        );
    }
}
