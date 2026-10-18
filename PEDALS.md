# Effects Pedals: Notes

Working notes on each pedal: what it does to the tone, and its known issues and shortcomings.
Kept up to date as pedals are added. A short player-facing list will be compiled from it at the end.

## How every pedal works

These apply to every pedal, so the per-pedal sections don't repeat them.

- **Instruments plug into pedals, not speakers.** A held instrument plugs in by right-clicking a pedal with it
  (shift + right-click unplugs it), a block instrument with the speaker cable (the instrument and the pedal, in
  either order). Pedals chain into each other with the cable, and an instrument plays through the pedal it's
  plugged into and every pedal after it.
- **The effect follows the instrument everywhere.** Its notes sound through its pedals where it's played, through
  each of its speakers, and to its own player. Speakers just relay the instrument's sound.
- **Pedals can be shared.** Several pedals (and instruments) can plug into the same pedal, so a band can each have
  their own pedals, all ending at one shared pedal. Turning a shared pedal's knobs changes it for everyone.
- **Pedals only apply while loaded.** An instrument plays dry while its pedal's chunk isn't loaded, or from another
  dimension. A chain stops at the first pedal that isn't loaded. A pedal that was broken (or replaced by another
  at the same spot) unplugs its instruments once its spot is loaded.
- **Recordings remember the pedals.** Each recorded note keeps the chain it was played through, and plays back
  with it on any looper, even after the pedals are changed or gone.
- **Effects are rendered ahead of time, not live.** Each sound file is processed once per pedal setting and pitch
  it's played at, on the client, and cached (`pedal_cache_mb`, default 256 MB). The server only stores the knobs and sends a few bytes
  with each note.
- **Knobs change the next note, not the one already sounding.** A note keeps the sound it started with. That
  includes held notes, which only pick up a change once they're played again.
- **Knobs turn in 16 steps**, shown as 0 to 10. Each step is a new rendered variant of every sound played through it.
- **Every pedal sounds the same at every pitch.** Notes are pitched by OpenAL resampling a sound file (playing it
  faster or slower), which would move everything an effect did along with it: a 2 kHz filter would sit at 4 kHz an
  octave up. So a chain is rendered for the exact pitch a note plays at, with its filters, timing and sample rates
  worked out for that pitch, and they come out as set once it's resampled.
- **Transposing costs memory, not playing at the default pitch.** Most instruments have a sound file per note, so
  at the default transpose notes play at their file's own pitch and share one rendered copy. Each other transpose
  (or octave swap, or MIDI note past the instrument's range) renders its own copies, as much again as the default.
- **Held notes only get time-based effects (the Delay and Reverb) on release.** Their attack and hold go through the rest of the chain,
  since echoes of a looping sound would pile up while it's held.
- **Live effects (Tremolo, Vibrato) aren't rendered at all.** They change a note's volume or pitch while it plays, so they reach
  held notes the whole time they're held, and turning their knobs never renders anything. They always act as if
  they were last in the chain, wherever the pedal actually sits. The Lo-fi pedal is partly live: its wobble
  is, while its tape tone is rendered like any other pedal.
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
- **Chains go in order, up to 4 pedals.** No path through the pedals can be longer, counting from any pedal an
  instrument could plug into. A soft limiter at the end of every chain rounds off peaks above 85%,
  so stacked effects don't clip harshly.
- **First note after a change.** A sound is rendered the first time it is played through a new setting, which
  delays that note by a few milliseconds. The performer's own instrument is rendered ahead of time; listeners
  render as they hear it.
- **The instrument screen shows the chain** in its top-right corner (e.g. "Pedals: Fuzz → Delay"), under the speaker
  counter. It lists only the pedals that are on and loaded, so it's exactly what the notes go through, and updates as
  pedals are stomped or plugged in. Hovering a pedal's name shows its knobs, from 0 to 10 as on the pedal. It
  doesn't show which pedal block is which.
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
- **Memory.** Each rendered note is up to 4 seconds longer. An instrument fully played through a
  delay takes up to about 70 MB (the long bass samples), a good share of the default 256 MB cache. With several
  players' delays heard at once, other variants get let go more often. Raise `pedal_cache_mb` if notes start to
  take a moment to sound again.
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
- **Downsampling is the same at every pitch:** the sample rate is set as heard, so a transposed note is crushed to
  the same rate as any other. Its aliasing doesn't follow the note, so different notes alias into different
  inharmonic tones, like a real bitcrusher.
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

## Tremolo Pedal

**Signal path:** the note's volume rises and falls in a steady pulse while it plays. Applied live, on top of
the rest of the chain, rather than rendered into the sound.

| Knob | Range | Effect |
|---|---|---|
| Speed | 1 to 12 pulses a second (exponential) | Low: a slow swell. Middle: the classic amp tremolo. High: a fast flutter |
| Depth | 0% to 98% dip in volume | Low: a gentle shimmer. High: the note chops in and out |
| Shape | Smooth sine to nearly square | Low: smooth and wavy. High: hard on/off chopping, like a stutter or helicopter effect |

**How it changes the tone:**
- **Movement.** Sustained notes (flute, violin, pads, held keyboard notes) come alive with a pulse, which is the
  classic use.
- **Rhythm.** A square shape at high depth chops notes into a rhythm, especially at speeds that match the tempo.
- **Plucked notes** pulse as they decay, which can sound like a vibraphone's motor or a surf guitar amp.
- **The tone itself is unchanged.** Only the volume moves; there's no pitch wobble (that would be vibrato) and no
  added harmonics.

**How it behaves:**
- **Held notes pulse the whole time,** from attack through hold to release, unlike the Delay.
- **Every note on a client shares one clock,** so chords and fast passages pulse together, as through one real
  pedal.
- **Volume updates every 10 ms,** fast enough for smooth pulses up to the top speed. It goes through the same path
  as every other volume change, so Sound Physics' twin sources pulse along with it.
- **It never makes notes louder.** The volume only dips below the note's own, so the loudness cap is kept.
- **Cheap.** No rendering and no cache memory: just a volume change per playing note, 100 times a second, on each
  listener's client. Turning its knobs costs nothing beyond resending the chain.

**Issues and shortcomings:**
- **Not in sync between players.** Each client runs its own clock, so two players hear the pulses at slightly
  different moments. Within one client everything is in sync.
- **No tempo sync.** Speed is set by ear, with no link to a song's tempo or the looper.
- **Its place in the chain doesn't matter.** It always acts last. A Delay after it doesn't repeat the pulsing:
  the echoes pulse along with the note, in time, instead of carrying the pulse from when they were played.
- **The pulse starts on the first update.** A note's first few milliseconds (until the next tick) play at full
  volume. At high depth and a slow speed, a note started during a dip can start with a brief blip.
- **Never fully silent.** The deepest dip is 98%, just short of silence, since vanilla stops a sound whose volume
  reads exactly 0 when volume settings change.
- **Dozens of fast notes** each get their own volume update 100 times a second. That's light, but it's the first
  pedal with a cost that scales with how many notes are playing at that moment.

## Vibrato Pedal

**Signal path:** the note's pitch bends up and down in a steady wobble while it plays. Applied live, on top of
the rest of the chain, the same way as the Tremolo (which moves the volume instead).

| Knob | Range | Effect |
|---|---|---|
| Speed | 2 to 10 wobbles a second (exponential) | Low: a slow, seasick bend. Middle: a singer's or violinist's vibrato. High: a fast, nervous warble |
| Depth | 0 to 1 semitone either way | Low: a subtle shimmer. Middle: an expressive vibrato. High: a wide, warbling detune |
| Shape | Smooth sine to straight-lined triangle | Low: smooth and natural. High: a more even, mechanical sweep |

**How it changes the tone:**
- **Expression.** Sustained instruments (flute, violin, saxophone, synths) get the vibrato a player would add
  by hand. At low depth it brings flat-sounding samples to life.
- **Warble.** High depth and speed give the wobbly, tape-like warble of a worn cassette or a Leslie speaker
  turned up.
- **Pitched, not filtered.** The pitch bends by resampling, so the whole sound shifts with it, including its
  timbre and (very slightly) its speed. That's natural for most instruments, but noticeable on voices, where
  the vowel sound bends with the pitch.

**How it behaves:**
- **Held notes wobble the whole time,** from attack through release.
- **Every note on a client shares one clock,** so chords bend together, as through one real pedal.
- **Smooth start.** A note starts at its own pitch, which is the middle of the wobble, so there's no jump when
  the vibrato takes over.
- **With a Tremolo too,** both run off the same clock: at the same speed they move in lockstep.
- **Sound Physics' twin sources** follow the pitch, so the wobble survives its crossfades.
- **Cheap,** like the Tremolo: no rendering or cache memory, a pitch change per playing note 100 times a second.

**Issues and shortcomings:**
- **Pure vibrato, not chorus.** There's no Mix knob, since the note itself is bent rather than blended with an
  unbent copy. A chorus needs that second copy, so it would be a separate pedal.
- **Not in sync between players,** and no tempo sync, as with the Tremolo.
- **It always acts last.** A Delay's echoes are part of the note's sound, so they bend along with the note in
  time, rather than keeping the wobble they were played with.
- **Out of tune with other instruments** at high depth, by design: a wide vibrato on one instrument in an
  ensemble sounds detuned against the rest.

## Octave Pedal

**Signal path:** three parts mixed together, built like a classic analog octaver so it never changes a sound's
length:
- **Sub:** the octave below, from a flip-flop. The note's polarity flips every other cycle (counted at its zero
  crossings), which halves its pitch. A 1.2 kHz low-pass rounds it off.
- **Up:** the octave above, from folding the note's negative half up (its absolute value), which doubles its
  pitch. A 5 kHz low-pass tames it.
- **Dry:** the note itself.

| Knob | Range | Effect |
|---|---|---|
| Sub | 0% to 100% (default about 67%) | Low: a hint of weight. High: a big, organ-like bass under every note |
| Up | 0% to 100% (default off) | Low: a ringing shimmer. High: a bright, glassy octave on top |
| Dry | 0% to 100% (default 100%) | Turn it down to hear only the octaves, for a bass line or a synth-like tone |

**How it changes the tone:**
- **Fuller and organ-like.** Sub fills out thin instruments; Sub and Up together sound like organ stops.
- **The sub is buzzy, not pure.** The flip-flop also adds a tone a fifth above the note, at 1.5x its
  frequency, which gives it the hollow, synth-like buzz of analog octavers. The low-pass rounds most of it off.
- **The octave up is bright and ring-like,** with other even harmonics on top.
- **Octave fuzz.** Octave then Fuzz gives the classic screaming octave-fuzz tone.

**How it behaves:**
- **No echoes, so no extra length:** held notes get it from attack to release.
- **Stays in tune at every pitch.** The octaves are worked out from each sound itself, so they stay octaves
  when the note is pitched, and its filters sit where they're set whatever the note.
- **Stereo-safe.** Cycles are counted on both channels mixed together, so the octave below stays in phase
  between left and right.
- **Within the loudness cap.** All three knobs up gets louder than the note, so the cap turns it back down.

**Issues and shortcomings:**
- **Tracks single notes best, like any real octaver.** Chords, samples with several notes in them, bells and
  other instruments with strong, clashing overtones (steel drum, kalimba, bells) can confuse the cycle counting,
  turning the sub into a glitchy, sputtering ring-mod sound. Drums and percussion mostly sputter.
- **It can lock onto an overtone.** On sounds whose fundamental is weak next to their overtones, the "octave
  below" can come out an octave below an overtone instead, so it sounds like the wrong note.
- **Possible thumps on held notes.** The attack, hold and release are rendered separately, so the octave below
  can restart flipped at each loop of the hold sound, heard as a soft thump or click at each repeat. Most likely
  with high Sub on held instruments.
- **Octave up disappears after heavy fuzz.** A Fuzz turned up squares the note off, and folding a square wave
  leaves it nearly flat, so a Fuzz-then-Octave chain with high drive barely has an octave above. Put the Octave
  first for octave fuzz.
- **Bitcrusher before it** gives the cycle counting stepped, noisy edges, so the sub tends to sputter.
- **Decays sputter at the very end.** As a note fades into its noise floor, the counting can go erratic for a
  moment before it stops.

## EQ Pedal

**Signal path:** three filters in a row, like an amp's tone stack: a low shelf for the bass, a wide peak for the
mids, and a high shelf for the treble. A band left in the middle isn't run at all.

| Knob | Range | Effect |
|---|---|---|
| Bass | -10.5 to +12 dB below 100 Hz (flat in the middle) | Down: thinner and tighter. Up: fuller and boomier |
| Mid | -10.5 to +12 dB around 800 Hz, a wide band (Q 0.8) | Down: scooped and hollow. Up: honky, pushed forward |
| Treble | -10.5 to +12 dB above 3.2 kHz | Down: darker and softer. Up: brighter and airier |

Knobs show their boost or cut in dB rather than 0 to 10, in 1.5 dB steps.

**How it changes the tone:**
- **Shaping, not distortion.** It only changes how loud each part of the sound is, so it adds no grit or harmonics.
- **Scoops and pushes.** Mid down with bass and treble up gives the classic scooped metal tone; mid up helps a
  part cut through a busy mix.
- **Taming effects.** After a Fuzz or Bitcrusher, treble down softens harsh fizz and aliasing; before a Fuzz,
  bass down keeps low notes from turning to mud.

**How it behaves:**
- **Same at every pitch,** like every pedal: the bands stay at 100 Hz, 800 Hz and 3.2 kHz as heard.
- **Rendered once per sound file and pitch,** with no extra length, so held notes get it from attack to release.

**Issues and shortcomings:**
- **Boosts make the rest quieter.** Pedals never make a note louder, so after a boost the whole sound is turned
  down until it's as loud as before. Boosting the bass makes it stand out more, not louder: the mids and treble
  come out quieter instead.
- **Fixed frequencies.** The bands can't be moved, so it can't reach a narrow problem frequency the way a
  parametric EQ can. Three knobs keep it quick to dial in.
- **Gentle shelves.** The bass and treble shelves are halfway at their corner frequency (100 Hz, 3.2 kHz) and only
  reach the full boost or cut about an octave past it.
- **Instruments with little bass or treble** (piccolo, bells, bass guitar's treble) barely respond to those knobs:
  there's nothing there to boost.

## Reverb Pedal

**Signal path:** the note, plus the sound of it ringing on in a room. Built like the classic Freeverb: eight
feedback loops of different lengths in parallel, each darkened by the Tone on every pass, then four diffusers in a
row that smear their echoes into a smooth wash. The right channel's loops are slightly longer than the left's, so
stereo sounds come out wide.

| Knob | Range | Effect |
|---|---|---|
| Decay | 0.3 to 4 s to die away by 60 dB (exponential) | Low: a small room, short and tight. High: a big hall or cathedral |
| Tone | 1.5 to 12 kHz damping | Low: warm and soft, like a carpeted room. High: bright and shimmery, like a stone hall or plate |
| Mix | 0% to 100% | How loud the reverb is: at full, it carries as much energy as the note itself |

**How it changes the tone:**
- **Space.** Notes sound like they're played in a room, from a small booth to a cathedral. Sustained instruments
  (pads, strings, flutes, voices) get the classic lush wash; plucked ones get a ringing halo.
- **Darker as it fades.** High frequencies die away faster than the lows, as in a real room, and more so with the
  Tone down. The Decay is the time for the low and middle frequencies.
- **Short decays thicken.** Below about half a second it mostly adds body and width rather than an audible tail.

**How it behaves:**
- **Same at every pitch,** like every pedal: the decay and tone come out as set whatever the note.
- **Held notes get it on release,** like the Delay: instruments with a release sound play it through the reverb,
  and those without ring on from a short slice of the held sound. While held, they play dry.
- **Dampening fades instead of cutting,** over up to 2 seconds, like the Delay, so the reverb dies away rather than
  stopping dead.
- **Fast to render**: about 20 ms for a 2 second stereo note with the longest decay, so a first note after a knob
  change is only slightly late.
- **With the Delay:** Delay then Reverb gives echoes that each bloom into the room, the classic ambient wash.
  Reverb then Delay repeats the whole reverberant sound, which gets muddy quickly.

**Issues and shortcomings:**
- **Held notes are dry while held.** The biggest compromise: sustained instruments, where reverb matters most,
  only get it once let go. A live reverb that reaches held notes (through OpenAL, like Sound Physics Remastered's)
  could be added later as another mode.
- **Notes ring on.** Up to 4 seconds past each note, so fast passages blur together, more notes sound at once
  (towards Minecraft's limit on simultaneous sounds), and each rendered note takes more cache memory, like the Delay.
- **The loudness cap can turn notes down.** The reverb adds to the note while it's sounding, so at a high Mix the
  whole note is turned down slightly to stay within the cap.
- **Stacks with Sound Physics Remastered,** whose own room reverb is added on top when installed. A big pedal reverb
  in a stone cave can be a lot; turn the Mix down there.
- **Metallic on clicks and drums.** Like any simple algorithmic reverb, very sharp sounds (drums, the kalimba's
  attack) can bring out a slight metallic flutter in the tail, most at short decays with the Tone up.

## Lo-fi Pedal

**Signal path:** a worn cassette tape, in four parts:
- **Bandwidth** (rendered): two-pole low and high cuts for the tape's narrow range, narrower the older it is.
- **Warmth** (rendered): soft tape saturation that rounds off the peaks, slightly lopsided so it adds the even
  harmonics of tape rather than a fuzz's odd ones; then a little more body in the low mids and softer highs.
- **Wow and flutter** (live): the pitch drifts slowly and unevenly, with a faster, finer shimmer on top.
- **Tape noise** (its own sound): a soft bed of tape hiss and vinyl crackle with faint pops, rolling on its own while
  the instrument plays, rather than with each note.

| Knob | Range | Effect |
|---|---|---|
| Age | Fresh to 30 years old | Low: full range, with a faint hiss. High: muffled (highs from 16 kHz down to 3.5 kHz, lows from 30 up to 150 Hz) and hissy (-54 to -34 dB of hiss) |
| Wobble | None to ±30 cents of wow, plus ±8 of flutter | Low: steady. Middle: a gentle, nostalgic drift. High: a seasick, warped tape |
| Warmth | Clean to fully saturated | Low: clean (fully down, no saturation or tilt at all). Middle: rounded, thicker notes. High: driven tape, with up to +4.5 dB of body around 200 Hz and -3 dB above 5 kHz |

**The tape deck:**
- **Each Lo-fi pedal is one tape.** Everyone plugged into the same Lo-fi pedal (directly, or through other pedals
  first) plays through the same tape, so a band sharing one hears one tape deck.
- **It rolls while anyone plays.** Opening an instrument plugged into it, or the first note through it, presses the
  play key: a click, and the motor spinning the noise up to speed. It keeps rolling between notes, and while notes are
  held. Once everyone on it has gone quiet for 10 seconds (or closed their instruments), the stop key clicks and the
  motor winds the noise down in pitch, like a tape stopping.
- **One hiss per tape, like the band's room tone.** However many players and speakers a tape has, each listener
  hears one hiss for it, not coming from any one spot. It's louder the nearer they are to the nearest place the tape
  is heard from (a player playing through it, or one of their speakers), and fades out 32 blocks away, as notes do.
  A player counts as a place while they play, and stops counting once they go quiet, without stopping the tape for
  the others. Players coming within range of a rolling tape hear its hiss fade in, without the clunk.
- **The key clunks come from the nearest place,** so they're heard from where the band is.
- **The hiss follows the loudest player's volume.**
- **Recordings roll their tapes as they were played.** Each recorded note keeps which Lo-fi pedal it went through,
  and a looper plays back each tape as a deck of its own, at the looper and its speakers: starting with its first
  note, and stopping 10 seconds after its last, just as it did live. Two players recorded through two different Lo-fi
  pedals play back as two tapes.
- **Everyone hears the same tape.** The wobble follows the world's game time, through a repeatable random
  function, so every player hears the same drift at the same moment, give or take a tick (50 ms). Every note through
  it, on every player's game, wobbles together, like one tape.

**How it changes the tone:**
- **Nostalgic and warm.** A little Age, Wobble and Warmth gives the classic lo-fi hip hop sound; more gives a worn,
  warped VHS or answering-machine sound.
- **Warmth on its own** works as a gentle tape saturator, for thickening any instrument without the worn sound.
- **Held notes and chords wobble together**, since the whole tape drifts at once.

**Issues and shortcomings:**
- **Out of tune with other instruments** at high Wobble, by design: a warped tape drifts against the band.
- **Its sounds are real recordings, all CC0 from Freesound** (no credit required), mixed into a seamless 12 second
  loop: tape hiss from "Tape Hiss" by D.S.G. (freesound.org/s/241415), crackle from "CRACKLE (6).wav" by DefySolipsis
  (freesound.org/s/531438), pops picked out of "Audio Mistakes, Pops" by f-r-a-g-i-l-e (freesound.org/s/512504) and
  "StaticAndPop.wav" by transitking (freesound.org/s/59289). The play and stop keys are the first two presses of
  "Tape Deck Buttons and Switches" by Nekkowe (freesound.org/s/403247). They're cut from Freesound's high quality
  previews; the masters are in `InstrumentSoundFiles/Lofi_Pedal`, with the other candidates in its `Candidates` folder.
- **Opening an instrument starts the tape at full volume** until its first note sets the player's volume.
- **The motor sounds only reach the hiss.** A tape stopping also slows the music down, but by then the notes have
  usually finished; notes still ringing when it stops don't wind down with it.
- **Warmth's saturation only bites on loud notes.** Quiet notes and decaying tails pass at their own level, so they
  get Warmth's tone tilt but little of its harmonics, as on real tape.
- **A deck's places update with its notes,** so a looper on a moving boat or minecart leaves its hiss behind until
  its next note.

## Player-facing list

To be compiled once more pedals are in.
