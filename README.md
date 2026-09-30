# Reactive Stochastic Music Engine (Java 25 LTS & Project Loom)

An enterprise-grade, high-performance reactive stochastic algorithmic composition engine implemented in pure **Java 25 LTS**. Designed around **Domain-Driven Design (DDD)** and **Hexagonal Architecture (Ports and Adapters)**, this engine completely decouples algorithmic composition from output rendering, offering zero-jitter microsecond timing and native MIDI streaming into professional DAWs (Ableton Live, Logic Pro, Reaper, FL Studio, Bitwig).

---

## 1. Architectural Blueprint (Hexagonal & DDD)

```
                                      +---------------------------------------------+
                                      |            DOMAIN / CORE MODEL              |
                                      |  (Java Records: Pitch, Duration, Velocity)   |
                                      +----------------------+----------------------+
                                                             |
                                      +----------------------v----------------------+
                                      |             MATHEMATICAL CORE               |
                                      |  * Order-2 Markov Chain (Harmonic Context)  |
                                      |  * Vose's Alias Method O(1) Discrete Sampler|
                                      |  * Katz Hierarchical Backoff Smoothing      |
                                      +----------------------+----------------------+
                                                             |
                                      +----------------------v----------------------+
                                      |         POLYPHONIC ENGINE & LOOM            |
                                      |  * Drift-Free Timeline Reference             |
                                      |  * PrecisionClock (LockSupport + SpinWait)  |
                                      |  * Loom Virtual Thread Voice Orchestration  |
                                      |  * Macro-Arrangement Song Conductor         |
                                      +----------------------+----------------------+
                                                             |
                                          [ OUTBOUND PORT: OutputPort ]
                                                             |
                           +---------------------------------+---------------------------------+
                           |                                                                   |
            +--------------v--------------+                                     +--------------v--------------+
            |      PRIMARY ADAPTER        |                                     |     DIAGNOSTIC ADAPTER      |
            |   JavaSoundMidiAdapter      |                                     |    LoggingOutputAdapter     |
            |  (ShortMessage NOTE_ON/OFF) |                                     | (Latency & Jitter Tracing)  |
            +--------------+--------------+                                     +-----------------------------+
                           |
            +--------------v--------------+
            | VIRTUAL MIDI BUS / HARDWARE |
            |  (IAC / loopMIDI / LoopBe)  |
            +--------------+--------------+
                           |
            +--------------v--------------+
            |       PROFESSIONAL DAW      |
            | (Ableton / Logic / Reaper)  |
            +-----------------------------+
```

---

## 2. Core Modules & Technical Specifications

### A. Domain & Mathematical Core
1. **2nd-Order Markov Chain (Cadena de Markov de Orden 2)**:
   - Conditioned on a 2-state compound history: $P(S_t \mid S_{t-2}, S_{t-1})$.
   - Eliminates the harmonic degeneration and repetitive memoryless loops of Order-1 chains, capturing authentic melodic direction, cadences, and tension intervals.
   - **Katz-style Hierarchical Backoff Smoothing**:
     $$\text{Level 2: } (S_{t-2}, S_{t-1}) \to S_t \quad\xrightarrow{\text{fallback}}\quad \text{Level 1: } S_{t-1} \to S_t \quad\xrightarrow{\text{fallback}}\quad \text{Level 0: Global Prior Scale Vocabulary}$$
     Guarantees infinite generation without dead-ends, absorbing states, or runtime exceptions.

2. **Vose's Alias Method ($O(1)$ Sampling)**:
   - Compiles empirical frequency distributions into probability and alias tables in $O(N)$ preprocessing time.
   - Generates each subsequent note in strict **$O(1)$ constant time** (requiring only two random numbers and a single comparison), using `ThreadLocalRandom` for zero lock contention across virtual threads. Eliminates timing jitter spikes inside high-speed audio loops.

3. **Immutability with Java Records**:
   - `Pitch`: MIDI note numbers $[0..127]$, fundamental frequency in Hertz ($440 \cdot 2^{(n-69)/12}$), enharmonic matching, pitch classes, and octave indexing.
   - `Duration`: Rhythmic subdivisions, PPQ tick representations, and real-time nanosecond conversions.
   - `Velocity`: Dynamic markings (ppp to fff), normalized amplitude scaling, and metric groove accentuations.
   - `NoteEvent`: Channel routing, temporal tick stamps, duration, and voice attribution.
   - `TransitionState<T>`: Generic compound history tuple $(S_{t-2}, S_{t-1})$ for n-gram contexts.

---

### B. Concurrency & Microsecond Timing (Project Loom)
1. **Microsecond Precision Clock (`PrecisionClock`)**:
   - **Zero `Thread.sleep()`**: Eliminates coarse OS sleep quantization (which introduces 1–15 ms jitter on standard OS schedulers).
   - **Drift-Free Timeline**: Operates on an absolute timeline reference ($t_k = t_0 + k \cdot \Delta t_{\text{tick}}$). Processing latency on tick $k$ does not propagate to tick $k+1$.
   - **Hybrid Park/Spin**: Employs `LockSupport.parkNanos()` for the coarse interval, followed by a tight `Thread.onSpinWait()` loop during the final 150 microseconds to hit exact hardware timestamps.

2. **Polyphonic Orchestration with Project Loom**:
   - Concurrently evaluates multiple independent voices on every tick:
     - **Voice 1 (Acid 303 Lead / Pluck Arp)**: MIDI Channel 1 (index 0). D Phrygian / Minor sequences with squelch accents and progressive 16-bar filter sweeps.
     - **Voice 2 (Heavy Rolling 16th Bass & Sub-Rumble)**: MIDI Channel 2 (index 1). Relentless galloping 16th motor with kick sidechaining and offbeat 8th drives.
     - **Voice 3 (Industrial Rave Stabs & Dark Drones)**: MIDI Channel 3 (index 2). Sharp syncopated rave stabs during drops and sustained dark pads during breakdowns.
     - **Voice 4 (Crushing 909 Techno Drums)**: MIDI Channel 10 (index 9). Punchy 4-on-the-floor kick, kick sub-rumble, searing offbeat 909 open hi-hat, layered claps, and ride cymbals.
   - **Asynchronous Note-Offs**: Each active note triggers an ephemeral Virtual Thread that parks for the exact note duration before dispatching `NOTE_OFF`. Zero hung notes, zero complex timing queues.

---

### C. Ports & Adapters (Hexagonal Architecture)
1. **`OutputPort`**: Interface defining reactive output contracts (`send`, `noteOn`, `noteOff`, `allNotesOff`).
2. **`JavaSoundMidiAdapter`**:
   - Dispatches raw byte packets via Java Sound API (`javax.sound.midi.ShortMessage`).
   - Automatically detects virtual loopback ports:
     - **macOS**: `IAC Driver`
     - **Windows**: `loopMIDI`, `LoopBe1`, `VirtualMIDI`
     - **Linux**: `ALSA Virtual RawMIDI`
   - Gracefully falls back to the Java Runtime internal software synthesizer (`Gervill` / `Microsoft GS Wavetable Synth`) if no virtual loopback cable is open.
3. **`LoggingOutputAdapter`**:
   - Microsecond diagnostic tracing with `LongAdder` and `DoubleAdder`. Calculates real-time jitter ($\text{actualNanos} - \text{scheduledNanos}$).
4. **`CompositeOutputAdapter`**:
   - Transparent fan-out dispatcher to broadcast simultaneously to both MIDI ports and telemetry loggers.

---

## 3. Project Structure

```
Music-proyect/
├── pom.xml
├── README.md
├── run.bat                          # One-click Windows runner
├── build.bat                        # Recompilation script
└── src/
    └── main/
        └── java/
            └── com/
                └── algorithmic/
                    └── music/
                        ├── Main.java                        # Executable CLI Runner
                        ├── VerificationTest.java            # Unit & Math Test Suite
                        ├── domain/
                        │   ├── model/
                        │   │   ├── Pitch.java               # Value Object: MIDI, Hz, Octaves
                        │   │   ├── Duration.java            # Value Object: Ticks, Beats, Nanos
                        │   │   ├── Velocity.java            # Value Object: Dynamics, Velocity
                        │   │   ├── NoteEvent.java           # Immutable Event Record
                        │   │   ├── VoiceType.java           # Polyphonic Voice Enum
                        │   │   └── TransitionState.java     # Markov (S_{t-2}, S_{t-1}) Context
                        │   └── markov/
                        │       ├── AliasMethod.java         # Vose's Alias Method O(1) Sampler
                        │       ├── TransitionDistribution.java # Cumulative Empirical Frequencies
                        │       ├── MarkovChainOrder2.java   # Order-2 Chain with Katz Backoff
                        │       └── ScaleCorpus.java         # D Phrygian & Acid Techno Corpora
                        ├── timing/
                        │   ├── TickEvent.java               # High-res Tick Record & Jitter Metas
                        │   └── PrecisionClock.java          # Microsecond Loom Precision Clock
                        ├── song/
                        │   ├── SongSection.java             # 8-Section Macro-Arrangement Enum
                        │   └── TechnoSongConductor.java     # Track Director & Terminal HUD
                        ├── engine/
                        │   ├── VoiceGenerator.java          # Voice Strategy Interface
                        │   ├── LeadVoiceGenerator.java      # Acid 303 Synth Generator
                        │   ├── BassVoiceGenerator.java      # Rolling 16th Rumble Bass Generator
                        │   ├── PadVoiceGenerator.java       # Rave Stabs & Dark Drone Generator
                        │   ├── DrumVoiceGenerator.java      # 909 Techno Percussion Generator
                        │   └── ReactivePolyphonicEngine.java# Structured Loom Polyphonic Engine
                        ├── port/
                        │   └── OutputPort.java              # Hexagonal Outbound Contract
                        └── adapter/
                            ├── JavaSoundMidiAdapter.java    # Low-level Native MIDI Adapter
                            ├── LoggingOutputAdapter.java    # Latency & Diagnostic Adapter
                            └── CompositeOutputAdapter.java  # Fan-out Multi-port Dispatcher
```

---

## 4. Compilation & Execution

### Prerequisites
- **JDK 25 LTS** installed (Oracle OpenJDK, Eclipse Temurin, or Amazon Corretto).
- System environment configured or binary located at `C:\Program Files\Java\jdk-25.0.4.1\bin\java.exe`.

### Option 1: One-Click Execution (Windows)
Double-click **`run.bat`** in the project root, or execute from PowerShell / CMD:
```powershell
# Continuous live playback:
.\run.bat

# Timed sample playback (e.g., 30 or 60 seconds):
.\run.bat 30
.\run.bat 60
```

### Option 2: Run with Maven
```bash
mvn clean compile exec:java
```

### Option 3: Run Mathematical Verification Tests
```bash
& "C:\Program Files\Java\jdk-25.0.4.1\bin\java.exe" -cp bin com.algorithmic.music.VerificationTest
```

---

## 5. Connecting to Professional DAWs

To stream the engine's polyphonic MIDI output directly into your DAW:

### A. Configure a Virtual MIDI Bus
1. **Windows**: Install [loopMIDI](https://www.tobias-erichsen.de/software/loopmidi.html) or [LoopBe1](https://www.nerds.de/en/loopbe1.html). Create a port named `loopMIDI Port 1`.
2. **macOS**: Open **Audio MIDI Setup** > **Show MIDI Studio** > Double-click **IAC Driver** > Check **Device is online**.
3. **Linux**: Load the ALSA virmidi module: `sudo modprobe snd-virmidi`.

### B. Route Channels in your DAW
1. **Ableton Live / Logic / Reaper / FL Studio**:
   - Open DAW MIDI Preferences and enable `Input: loopMIDI` (or `IAC Driver`).
   - **Track 1**: Set MIDI From to `loopMIDI Port 1`, **Channel 1**. Load an Acid 303 / Saw Synth preset (e.g. Roland Cloud TB-303, Phoscyon, Diva, Serum).
   - **Track 2**: Set MIDI From to `loopMIDI Port 1`, **Channel 2**. Load a Rolling Sub-Bass / Moog Bass with light saturation.
   - **Track 3**: Set MIDI From to `loopMIDI Port 1`, **Channel 3**. Load an Industrial Rave Stab / Dark Drone preset.
   - **Track 4**: Set MIDI From to `loopMIDI Port 1`, **Channel 10**. Load a 909 / Warehouse Drum Kit (Punchy Kick, Searing Offbeat 909 Open Hat, Crisp Claps).
   - Set all tracks to **Input Monitor (In)**.
2. Launch `.\run.bat`. You will hear a real-time, polyphonic Peak-Time Acid Techno track streamed directly into your DAW!

---

## 6. Roadmap: Upcoming Sound Libraries & Genre Expansions

The engine has been architected under a **decoupled Hexagonal Architecture**, allowing the addition of new genre-specific libraries, training corpora, and expression modules without altering the microsecond precision clock (`PrecisionClock`) or the polyphonic core (`ReactivePolyphonicEngine`).

The following modular sound libraries are planned for incorporation:

```
                                +-------------------------------------------------------+
                                |          GENRE LIBRARIES & EXPANSION ROADMAP          |
                                +---------------------------+---------------------------+
                                                            |
                 +--------------------------+---------------+--------------------------+
                 |                          |                                          |
  +--------------v-------------+ +----------v----------------+ +-----------------------v---+
  |     ACID TECHNO LIBRARY    | | PROGRESSIVE TECHNO LIBRARY | |  HOUSE / DEEP LIBRARY     |
  | * Roland TB-303 Emulation  | | * Cinematic Arpeggios     | | * Swing / Shuffle Groove  |
  | * MIDI Slides & Portamento | | * Extended Dm9, Bbmaj7#11 | | * Korg M1 Organ Stabs     |
  | * CC 74 (Cutoff/Resonance) | | * 32-Bar Deep Breakdowns  | | * Jackin' Chicago Drums   |
  +----------------------------+ +---------------------------+ +---------------------------+
                 |                                                         |
                 +--------------------------+------------------------------+
                                            |
                              +-------------v--------------+
                              |   VOICES & SPEECH LIBRARY  |
                              | * Stochastic Vocal Chops   |
                              | * Spoken Word Dark Speech  |
                              | * Formant & Pitch Bends    |
                              +----------------------------+
```

### 1. Acid Techno Library (TB-303 Emulation & Extreme Resonance)
*   **Slide & Portamento Articulation**: MIDI Legato / CC 65 (*Portamento On/Off*) and note overlapping to emulate the pitch-sliding glide of the Roland TB-303.
*   **Real-Time Analog Filter Automation**: Continuous stochastic generation of **MIDI CC 74 (Filter Cutoff)** and **CC 71 (Resonance)** curves, delivering tempo-synced acid squelches and resonance chirps.
*   **Dissonant Phrygian / Locrian Corpora**: High-tension melodic patterns utilizing minor 2nd intervals and industrial tritones ($D \leftrightarrow E\flat$ and $D \leftrightarrow A\flat$).

### 2. Progressive Techno & Melodic Deep Library
*   **Long-Evolution Arpeggiators**: Hypnotic melodic motifs engineered to develop across 32 or 64-bar macro-cycles.
*   **Extended Harmonic Composition**: Suspended 9th, 11th, and 13th chords ($Dm9$, $Bbmaj7\sharp 11$, $Fmaj7$, $Csus2$, $Asus4$).
*   **Cinematic Tension Conductors**: Extended 32-bar breakdowns where kick and bass completely drop out to make space for massive atmospheric reverb pads.

### 3. House, Tech House & Deep House Library
*   **Quantization Engine with Swing / Shuffle**:
    *   Micro-timing offset algorithm for even 16th notes ($t_{\text{swing}} = t_{\text{grid}} + \Delta_{\text{shuffle}}$), configurable between **54% and 68% swing**.
    *   Recreates the infectious, bouncy groove characteristic of Chicago House, UK Garage, and modern Tech House (Fisher, Michael Bibi, Chris Lake).
*   **Chord Stabs & M1 Organ Bass**:
    *   Fast-attack minor chord bursts for syncopated dancefloor stabs.
    *   Elastic basslines with syncopated 8th-note grooves and tied notes.
*   **Jackin' Drum Machines**:
    *   Clap and snare layers with humanized dynamics and semi-open hi-hats with variable decay times.

### 4. Voices & Stochastic Vocal Chops Library (Voices & Speech)
*   **Dedicated Sampler Channel (MIDI Channel 4 or 5)**:
    *   Stochastic trigger mapping for sampler instruments (Kontakt, Ableton Simpler, Serato Sample) to fire pitch-accurate vocal chops.
*   **Intelligent Vocal Phrasing Algorithm**:
    *   Structured Call-and-Response patterns to avoid robotic or monotone loops.
*   **Spoken Word & Dark Industrial Speech**:
    *   Scheduled triggering of robotic, dark, or poetic spoken-word phrases during breakdowns, modulated with pitch bend and mod wheel (CC 1).

---

### How New Libraries Integrate into the Codebase
Thanks to clean separation of concerns:
1.  **New Scales & Harmonies**: Simply add new sequences into `com.algorithmic.music.domain.markov.ScaleCorpus`.
2.  **New Voice Roles**: Implement the `VoiceGenerator` interface (e.g., `AcidVoiceGenerator`, `HouseGrooveVoiceGenerator`, `VocalChopVoiceGenerator`).
3.  **Untouched Sequencer Core**: The microsecond precision clock in **Java 25 LTS** and universal MIDI adapters remain completely unmodified, driving any newly added genre library effortlessly.
