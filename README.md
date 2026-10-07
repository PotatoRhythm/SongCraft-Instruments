# SongCraft Instruments

Instruments!?

This mod is a heavily modified continuation of the Genshin Instruments and Even More Instruments! mods. It includes most content from the original mods + many new instruments and features.

## New Features:
- Players now have the option to hear their own notes after they reach the server. This allows syncing with other players with high latency.
- Added sounds from GW2, FFXIV, Heartopia, Where Winds Meet, and Sky: Children of the Light
- Reworked the record system and added group recording
- Several new instrument items including the instruments from Guild Wars 2.
- Added Jianpu and Guild Wars 2 style button icons.
- Notes can now be manually dampened to shorten them.
- Customizable note particle system.
- Speaker block that can be connected to instruments and looper.
- Updated UI for quick access to volume and sound type options while playing music.
- Balanced instrument volume levels and increased range.
- Octave Swap control mode added.
- Heartopia control mode added.
- Various other minor changes and fixes.

## New Instrument Items:
- Microphone
- Drum Set
- Electric guitar
- Bass guitar
- Harp
- Cello
- Double Bass
- Steel Drum
- Concertina
- Flute
- Xiao
- Drum
- Kalimba
- Ocarina
- Animal Call
- Synth
- Bell
- Guild Wars 2 Instruments
  - Flute
  - Magnanimous Choir Bell
  - Marriner's Horn
  - Musical Bass Guitar
  - Musical Frame Drum
  - Musical Harp
  - Musical Lute
  - Musical Minstrel
  - Musical Verdarach
  - Pipe Organ
  - Quaggan Organ
  - Unbreakable Choir Bell
  - Black Lion Drum Set
  - Ornate Grand Piano

## Commands / Config:
**Commands**
- `/screcord export <name>` — Saves the held record to `.minecraft/songcraft_records`
- `/screcord import <name>` — Burns a file from that folder onto the held empty record
- `/screcord files` — Lists your record files
- `/screcord stats` — Shows how many recordings are stored and their size (operators)
- `/screcord cleanup <days>` — Deletes recordings no record has been seen with in that many days (operators)

**Server config**
- `max_notes` (default `1000000`) — The most notes one record can hold, including imports. -1 for no limit
- `import_operators_only` (default `false`) — Only operators can import records
- `import_daily_limit_kb` (default `10240`) — How much new record data each player can import per 24 hours. Operators are exempt. -1 for no limit

## Build / Run
- Use Gradle wrapper: `./gradlew build`
- Compatible with Minecraft 1.20.1 Forge

## Credits
- Original [Genshin Instruments](https://github.com/StavWasPlayZ/Genshin-Instruments) mod by StavWasPlayZ
- Original [Even More Instruments!](https://github.com/StavWasPlayZ/Even-More-Instruments) mod by StavWasPlayZ
- Full credits, including the source and licence of every sound: [CREDITS.md](CREDITS.md)

## License
This mod is based on Genshin Instruments and Even More Instruments!, which are licensed under **GNU GPL v3**.  
This fork is also licensed under **GNU GPL v3**.