package com.algorithmic.music.song;

/**
 * Defines the macro-architectural arrangement sections of an authentic Melodic Techno track.
 * Each section dictates track mutes, filter cutoffs, dynamic intensity, and rhythmic density.
 */
public enum SongSection {
    INTRO(0, 16, "INTRO - Atmospheric Emergence & Sub Pulse", 0.3),
    GROOVE_ENTRY(16, 32, "GROOVE - Rolling Bass & Driving Hats Enter", 0.6),
    BUILDUP_1(32, 48, "BUILDUP 1 - Rising Tension & Lead Filter Sweep", 0.8),
    DROP_1(48, 80, "DROP 1 - Peak Melodic Techno Energy (Full Orchestra)", 1.0),
    BREAKDOWN(80, 112, "BREAKDOWN - Kick Drops Out & Ethereal Pads Swell", 0.45),
    BUILDUP_2(112, 128, "BUILDUP 2 - Snare Rush & Maximum Climax Riser", 0.95),
    DROP_2_CLIMAX(128, 160, "DROP 2 - Ultimate Peak Drop & Screaming Lead", 1.0),
    OUTRO(160, 192, "OUTRO - Deconstruction & Final Ambient Resolution", 0.35);

    private final int startBar;
    private final int endBar;
    private final String description;
    private final double targetEnergy;

    SongSection(int startBar, int endBar, String description, double targetEnergy) {
        this.startBar = startBar;
        this.endBar = endBar;
        this.description = description;
        this.targetEnergy = targetEnergy;
    }

    public int startBar() {
        return startBar;
    }

    public int endBar() {
        return endBar;
    }

    public int totalBars() {
        return endBar - startBar;
    }

    public String description() {
        return description;
    }

    public double targetEnergy() {
        return targetEnergy;
    }

    public static SongSection forBar(long bar) {
        long moduloBar = bar % 192; // Loops cleanly after 192 bars (approx 6 minutes)
        for (SongSection section : values()) {
            if (moduloBar >= section.startBar && moduloBar < section.endBar) {
                return section;
            }
        }
        return INTRO;
    }
}
