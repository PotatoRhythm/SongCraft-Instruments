# Effects Pedals: Notes

Working notes on each pedal: what it does to the tone, and its known issues and shortcomings.
Kept up to date as pedals are added. A short player-facing list will be compiled from it at the end.

## How every pedal works

These apply to every pedal, so the per-pedal sections don't repeat them.

- **Effects are rendered ahead of time, not live.** Each sound file is processed once per pedal setting, on the
  client, and cached (`pedal_cache_mb`, default 64 MB). The server only stores the knobs and sends a few bytes
  with each relayed note.
- **Knobs change the next note, not the one already sounding.** A note keeps the sound it started with. That
  includes held notes, which only pick up a change once they're played again.
- **Knobs turn in 16 steps**, shown as 0 to 10. Each step is a new rendered variant of every sound played through it.
- **Tone shifts with pitch.** A sound is rendered at its file's own pitch, and OpenAL resamples it for other notes
  (up to 2 octaves either way). Any frequency inside the effect moves with the note: a filter at 2 kHz sits at
  4 kHz an octave up, and at 1 kHz an octave down. High notes come out brighter and low notes darker than the
  knob suggests, by up to 4x in frequency.
- **Time-based effects (the Delay) are the exception.** Resampling would also stretch their timing, so chains
  with one are rendered separately for every pitch a note plays at, with timing and filters corrected for it.
  That's one rendered variant per note rather than per sound file, so these chains use more cache memory and
  render more often.
- **Held notes only get time-based effects on release.** Their attack and hold go through the rest of the chain,
  since echoes of a looping sound would pile up while it's held.
- **The level is set from the sound file's own loudness.** The volume trims in `sounds.json`, the instrument's
  volume slider and distance are all applied afterwards. Pedals don't change how loud instruments are relative
  to each other, but anything that depends on the file's level (such as the fuzz's gate) treats quiet and loud
  recordings differently.
- **Pedals never make an instrument louder.** After a chain renders, the result is turned down if it is louder
  than the original sound, either on average (RMS) or at its peaks. The player's volume slider applies on top,
  so a note through pedals is never louder than the same note at that volume without them. Level knobs only
  turn down: fully up is as loud as the original. This compares measured levels, not perceived loudness: an
  effect that adds bright harmonics (like fuzz) can still sound a little louder at the same level, since ears
  are most sensitive to the upper mids.
- **Chains go in order, up to 4 pedals.** A soft limiter at the end of every chain rounds off peaks above 85%,
  so stacked effects don't clip harshly.
- **First note after a change.** A sound is rendered the first time it is played through a new setting, which
  delays that note by a few milliseconds. The performer's own instrument is rendered ahead of time; listeners
  render as they hear it.
- **The performer hears the nearest speaker's pedals** on their own notes, switching once another is 3+ blocks
  closer. Nothing in the UI shows which speaker that is yet.
- **Someone standing by the performer hears the performer's notes without pedals.** Only the nearest copy of a
  note is played, and next to the performer that's the performer's own unprocessed copy, not a speaker's.
- **Only speaker blocks take pedals**, not speakers on boats or minecarts.
- **Sound Physics Remastered** (when installed) adds its room reverb after the pedals, as a room would.

## Fuzz Pedal

**Signal path:**
1. 100 Hz high-pass filter
2. Gain of 6 to 46 dB (Drive)
3. Slightly asymmetric soft clipper
4. DC blocker
5. 700 Hz to 12 kHz low-pass filter (Tone)
6. Noise gate
7. Loudness matched to the dry sound, then Level

| Knob | Range | Effect |
|---|---|---|
| Drive | 6 to 46 dB of gain into the clipper | Low: warm, mostly clean overdrive. High: squared-off, buzzy fuzz with long sustain |
| Tone | 700 Hz to 12 kHz low-pass | Low: dark and woolly. High: bright and fizzy |
| Level | -24 to 0 dB, compared to the dry sound | Fully up (the default) is as loud as without the pedal |

**How it changes the tone:**
- **Harmonics.** Clipping adds odd harmonics. The asymmetry adds even ones on top, for a warmer, more "transistor"
  fuzz than a plain square wave.
- **Sustain.** At high drive the clipper flattens a note's decay, so quiet tails come up close to the attack's
  level until the gate closes. Plucked and struck instruments (guitar, koto, kalimba, piano) start to sound bowed.
- **Attack.** Loudness is matched over the whole sound, so a fuzzed note's attack is quieter than the dry
  attack while its tail is louder. Percussive instruments lose punch.
- **Lows.** The 100 Hz high-pass keeps bass from turning to flub, but thins out low instruments. Double bass, bass
  guitar, cello and low drums lose some of their fundamental, more so on notes pitched down.
- **Polyphony.** Chords and samples with several notes in them (some synths and bells) turn into a gritty,
  intermodulated mush, as with a real fuzz. Single notes fuzz best.

**Issues and shortcomings:**
- **Aliasing on high notes.** The clipper isn't oversampled, so at high drive some of its harmonics fold back as
  inharmonic fizz. Notes pitched up make it worse, since resampling raises it further.
- **The tone filter is gentle** (a single 6 dB per octave stage). Even fully dark, some fizz gets through. Real
  fuzz tone circuits cut more steeply.
- **The gate can cut notes short.** It closes between -48 and -60 dB of the sound file's level, ignoring the
  volume trims. Quietly recorded samples get their tails cut earlier than loud ones. On instruments with long,
  soft releases (bells, harp, pads), the very end of the note may fade out early.
- **Possible clicks on held notes.** The attack, hold and release sounds are each rendered separately, starting
  from silence. A hold sound that loops can pick up a slight click or level dip at each repeat, where the
  filters restart. Expected from how the sounds are rendered rather than confirmed by ear. Worth listening for on
  flute, violin and other held instruments at high drive.
- **Two fuzzes in a row** behave like one fuzz with more drive and a second tone filter. Each stage matches its
  loudness to its input, so stacking them doesn't get louder, only more compressed.
- **Drums and percussion.** Work, but mostly come out as noise bursts with a long, gated tail.

## Delay Pedal

**Signal path:** the note, plus a repeating echo of it. Each repeat goes back through the delay at the Feedback
level and through a 4 kHz low-pass filter, so every echo is quieter and darker than the last, like an analog or
tape delay.

| Knob | Range | Effect |
|---|---|---|
| Time | 60 ms to 1 s between echoes (exponential) | Low: slapback and doubling. Middle: rhythmic echoes. High: long, spacious repeats |
| Feedback | 0% to 70% of each echo comes back | Low: a single echo. High: a long trail of fading repeats |
| Mix | 0% to 100%, the first echo's loudness compared to the note | Low: subtle ambience. High: echoes as loud as the note |

**How it changes the tone:**
- **Space and rhythm.** The note itself is unchanged; the echoes add depth, or a rhythm when the time fits the tempo.
- **Darker repeats.** Each echo loses some treble, so long trails soften into the background rather than
  piling up harshly.
- **Short times thicken.** Below about 100 ms the echo blends with the note, making it sound doubled or metallic
  rather than repeated.
- **Notes ring on.** The echoes last up to 4 seconds past the end of the note, so fast passages blur together.

**How it behaves:**
- **Timing holds at every pitch.** Each note is rendered for the pitch it plays at, so the echo spacing and the
  darkening stay the same from the lowest note to the highest.
- **Dampening fades instead of cutting.** A dampened note with echoes fades out over its tail's length (between
  0.5 and 2 seconds) instead of the usual 0.5, so the echoes trail off rather than stopping dead. The note itself
  fades out more slowly too.
- **Held notes echo on release.** Instruments with a release sound (like the flute) play their release through
  the delay. Those without one play echoes of a short slice of the held sound (0.3 s, fading out) when the
  note is let go, while the note itself fades out as usual.
- **Order with the Fuzz matters.** Fuzz then Delay gives clean echoes of a fuzzed note. Delay then Fuzz fuzzes
  the echoes along with the note: they intermodulate into a dense, sustained wall, and the fuzz's compression
  brings the later echoes up nearly as loud as the note.

**Issues and shortcomings:**
- **Knobs show 0 to 10, not milliseconds or beats.** There's no tempo sync, so matching echoes to a song's tempo
  is by ear.
- **No stereo ping-pong.** Echoes come from the same place as the note.
- **The loudness cap can turn notes down.** Where echoes overlap a long note, the total gets louder, and the cap
  (see above) turns the whole note down to compensate, including the note itself. Most noticeable on sustained
  instruments with a high Mix.
- **More voices.** Each note sounds for up to 4 seconds longer, so fast playing has more notes sounding at once.
  On busy servers this can reach Minecraft's limit on simultaneous sounds sooner, and Sound Physics twin sources
  run out sooner (the crossfades fall back to gliding).
- **Memory.** One rendered variant per note, each up to 4 seconds longer. An instrument fully played through a
  delay can take a large share of the default 64 MB cache, so other variants get let go more often. Raise
  `pedal_cache_mb` if notes start to take a moment to sound again.
- **Held-note echo tails are an approximation.** They echo a slice of the held sound, not exactly what was
  playing when the note was let go, and at the volume the note had then. Very short presses (released during
  the attack) use the same slice.
- **Dampened notes fade more slowly.** Notes through a delay fade out over up to 2 seconds when dampened, so
  quick dampen-and-replay playing sounds smeared.

## Bitcrusher Pedal

**Signal path:** sample-rate reduction (each sample held for a while, with no smoothing filter) and bit-depth
reduction (samples rounded to fewer levels), then blended with the original by Mix.

| Knob | Range | Effect |
|---|---|---|
| Crush | Off, then 12 bits down to 2 (fewer bits as it turns up) | Low: faint grain on quiet parts. Middle: chunky, 8-bit console grit. High: harsh, nearly square-wave buzz |
| Downsample | Off, then 32 kHz down to 1 kHz | Low: a slight metallic sheen. Middle: ringing, lo-fi "game sound" aliasing. High: muffled, with loud whistling aliases |
| Mix | 0% to 100% crushed (default 100%) | Lower blends the crushed sound under the clean note, for grit without losing the note's body |

**How it changes the tone:**
- **Grit, not hiss.** Without dither, the rounding error follows the note itself, so it comes across as distortion
  that moves with the note rather than a steady background noise.
- **Aliasing.** Downsampling folds high frequencies back down as inharmonic, bell-like tones. That's the classic
  retro-game character, and is left in on purpose.
- **Decays break up.** As a note fades, fewer levels are left to describe it, so its tail turns into stepped,
  crunchy noise and then cuts to silence. The higher the crush, the earlier notes end.
- **Held notes work all the way through.** It isn't time-based, so attacks, holds and releases are all crushed.

**How it behaves:**
- **Downsampling shifts with pitch,** like the Fuzz's tone: it's applied at the sound file's own pitch, so notes
  played above a sample sound less downsampled and notes below more. The aliasing stays tied to the note, so
  each note keeps a consistent character.
- **In chains:** Fuzz then Bitcrusher crushes an already squared-off signal, which is dense and aggressive.
  Bitcrusher then Fuzz fuzzes the crushed sound, which smears the steps. Bitcrusher then Delay gives crushed
  echoes that soften as they repeat. Delay then Bitcrusher crushes the echoes too, so at high crush the quieter
  echoes round to silence and only the first few survive.

**Issues and shortcomings:**
- **No tone control or smoothing filter.** At high downsample on high notes the aliasing can be piercing, and the
  only way to soften it is Mix (or a Fuzz after it, with its tone turned down).
- **Notes end early at high crush.** Quiet instruments and soft releases (harp, bells, pads) lose their tails first.
- **Downsample depends on the sound file's own sample rate,** so instruments recorded at 44.1 kHz and 48 kHz come
  out slightly differently at the same knob setting.
- **Perceived loudness can rise at extreme settings.** The level stays under the loudness cap, but a squared-off,
  bright waveform sounds louder than its measured level.

## Player-facing list

To be compiled once more pedals are in.
