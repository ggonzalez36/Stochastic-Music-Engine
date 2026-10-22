# Stochastic Music Engine (Java, Markov Chains, MIDI)

An algorithmic music generator written in Java 21. It learns note transitions from small built-in corpora with **order-2 Markov chains**, samples the next note in $O(1)$ with **Vose's alias method**, sequences four voices (lead, bass, pads, drums) on virtual threads with a drift-compensated clock, and sends the result as **MIDI** to the Java software synthesizer or to a DAW (Ableton Live, Logic Pro, Reaper, FL Studio, Bitwig) through a virtual MIDI port. The code follows **Domain-Driven Design (DDD)** naming and a **ports-and-adapters (hexagonal)** design, so composition logic is independent from MIDI output.

> **Scope:** personal hobby/learning project; the training corpora are small hand-written note sequences.
> - **Implemented:** order-2 Markov chain with backoff to order-1 and a global prior, Vose's alias sampler, immutable value objects (Java records), a park-then-spin precision clock on an absolute timeline, four voice generators with an 8-section song conductor, virtual-thread note scheduling, and MIDI / logging / composite output adapters. Mathematical checks live in `VerificationTest.java`, a `main`-based verification program (not a JUnit suite).
> - **Not measured:** timing accuracy depends on the OS scheduler and MIDI driver; the logging adapter reports observed jitter, but no results are published.
> - **Planned:** JUnit tests and CI with `mvn verify`, and the genre libraries described in the [Roadmap](#6-roadmap-upcoming-sound-libraries--genre-expansions).

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
                                      |  * Hierarchical Backoff (Order 2 -> 1 -> 0) |
                                      +----------------------+----------------------+
                                                             |
                                      +----------------------v----------------------+
                                      |         POLYPHONIC ENGINE & LOOM            |
                                      |  * Drift-Free Timeline Reference             |
                                      |  * PrecisionClock (LockSupport + SpinWait)  |
                                      |  * Virtual Thread Voice Orchestration       |
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
            |             DAW             |
            | (Ableton / Logic / Reaper)  |
            +-----------------------------+
```

---

## 2. Core Modules & Technical Specifications

### A. Domain & Mathematical Core
1. **2nd-Order Markov Chain (Cadena de Markov de Orden 2)**:
   - Conditioned on a 2-state compound history: $P(S_t \mid S_{t-2}, S_{t-1})$.
   - Reduces the aimless, memoryless wandering of order-1 chains and captures more melodic direction, cadences and tension intervals from the corpus.
   - **Hierarchical Backoff** (a simplified Katz-style scheme, without probability discounting):
     $$\text{Level 2: } (S_{t-2}, S_{t-1}) \to S_t \quad\xrightarrow{\text{fallback}}\quad \text{Level 1: } S_{t-1} \to S_t \quad\xrightarrow{\text{fallback}}\quad \text{Level 0: Global Prior Scale Vocabulary}$$
     Once trained, generation never reaches a dead end or absorbing state.

2. **Vose's Alias Method ($O(1)$ Sampling)**:
   - Compiles empirical frequency distributions into probability and alias tables in $O(N)$ preprocessing time.
   - Samples each subsequent note in **$O(1)$ time** (two random numbers and one comparison), using `ThreadLocalRandom` to avoid lock contention across virtual threads. Constant-time sampling keeps per-tick work small and predictable.

3. **Immutability with Java Records**:
   - `Pitch`: MIDI note numbers $[0..127]$, fundamental frequency in Hertz ($440 \cdot 2^{(n-69)/12}$), enharmonic matching, pitch classes, and octave indexing.
   - `Duration`: Rhythmic subdivisions, PPQ tick representations, and real-time nanosecond conversions.
   - `Velocity`: Dynamic markings (ppp to fff), normalized amplitude scaling, and metric groove accentuations.
   - `NoteEvent`: Channel routing, temporal tick stamps, duration, and voice attribution.
   - `TransitionState<T>`: Generic compound history tuple $(S_{t-2}, S_{t-1})$ for n-gram contexts.

---

### B. Concurrency & Timing (Virtual Threads)
1. **Precision Clock (`PrecisionClock`)**:
   - **No `Thread.sleep()`**: Avoids coarse OS sleep quantization, which can add milliseconds of jitter on common schedulers.
   - **Drift-Free Timeline**: Operates on an absolute timeline reference ($t_k = t_0 + k \cdot \Delta t_{\text{tick}}$). Processing latency on tick $k$ does not propagate to tick $k+1$.
   - **Hybrid Park/Spin**: Employs `LockSupport.parkNanos()` for the coarse interval, followed by a short `Thread.onSpinWait()` loop during the final 150 microseconds to get closer to the scheduled timestamp.

2. **Polyphonic Orchestration with Virtual Threads (Project Loom, Java 21)**:
   - Concurrently evaluates multiple independent voices on every tick:
     - **Voice 1 (Acid 303 Lead / Pluck Arp)**: MIDI Channel 1 (index 0). D Phrygian / Minor sequences with squelch accents and progressive 16-bar filter sweeps.
     - **Voice 2 (Heavy Rolling 16th Bass & Sub-Rumble)**: MIDI Channel 2 (index 1). Relentless galloping 16th motor with kick sidechaining and offbeat 8th drives.
     - **Voice 3 (Industrial Rave Stabs & Dark Drones)**: MIDI Channel 3 (index 2). Sharp syncopated rave stabs during drops and sustained dark pads during breakdowns.
     - **Voice 4 (Crushing 909 Techno Drums)**: MIDI Channel 10 (index 9). Punchy 4-on-the-floor kick, kick sub-rumble, searing offbeat 909 open hi-hat, layered claps, and ride cymbals.
   - **Asynchronous Note-Offs**: Each active note starts a short-lived virtual thread that parks for the note duration before sending `NOTE_OFF`, which keeps scheduling simple without a separate timing queue.

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
   - Diagnostic tracing with `LongAdder` and `DoubleAdder`. Calculates observed jitter ($\text{actualNanos} - \text{scheduledNanos}$).
4. **`CompositeOutputAdapter`**:
   - Transparent fan-out dispatcher to broadcast simultaneously to both MIDI ports and telemetry loggers.

---

## 3. Project Structure

```
Stochastic-Music-Engine/
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
                        ├── VerificationTest.java            # main-based math verification checks
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
                        │       ├── MarkovChainOrder2.java   # Order-2 Chain with Backoff
                        │       └── ScaleCorpus.java         # D Phrygian & Acid Techno Corpora
                        ├── timing/
                        │   ├── TickEvent.java               # High-res Tick Record & Jitter Metas
                        │   └── PrecisionClock.java          # Park/spin precision clock
                        ├── song/
                        │   ├── SongSection.java             # 8-Section Macro-Arrangement Enum
                        │   └── TechnoSongConductor.java     # Track Director & Terminal HUD
                        ├── engine/
                        │   ├── VoiceGenerator.java          # Voice Strategy Interface
                        │   ├── LeadVoiceGenerator.java      # Acid 303 Synth Generator
                        │   ├── BassVoiceGenerator.java      # Rolling 16th Rumble Bass Generator
                        │   ├── PadVoiceGenerator.java       # Rave Stabs & Dark Drone Generator
                        │   ├── DrumVoiceGenerator.java      # 909 Techno Percussion Generator
                        │   └── ReactivePolyphonicEngine.java# Virtual-thread polyphonic engine
                        ├── port/
                        │   └── OutputPort.java              # Hexagonal Outbound Contract
                        └── adapter/
                            ├── JavaSoundMidiAdapter.java    # Java Sound MIDI Adapter
                            ├── LoggingOutputAdapter.java    # Latency & Diagnostic Adapter
                            └── CompositeOutputAdapter.java  # Fan-out Multi-port Dispatcher
```

---

## 4. Compilation & Execution

### Prerequisites
- **JDK 21 or newer** (e.g. Eclipse Temurin or Amazon Corretto). The Maven build compiles with `--release 21`; the code uses Java 21 features (records, virtual threads) and no newer language features.
- The Windows scripts (`build.bat`, `run.bat`) look for a local JDK 25 at `C:\Program Files\Java\jdk-25.0.4.1\` and otherwise use `java`/`javac` from `PATH`. `build.bat` compiles with `--release 25`, so it needs JDK 25; with JDK 21, use Maven.

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

### Option 3: Run the Mathematical Verification Checks
```bash
# After build.bat (classes in bin/):
java -cp bin com.algorithmic.music.VerificationTest
```

---

## 5. Connecting to a DAW

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
2. Launch `.\run.bat`. The engine streams a generated four-voice acid techno track into your DAW in real time.

---

## 6. Roadmap: Upcoming Sound Libraries & Genre Expansions

The engine has been architected under a **decoupled Hexagonal Architecture**, allowing the addition of new genre-specific libraries, training corpora, and expression modules without altering the precision clock (`PrecisionClock`) or the polyphonic core (`ReactivePolyphonicEngine`).

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
3.  **Unchanged Sequencer Core**: The precision clock and the MIDI adapters do not need to change to drive a new genre library.
