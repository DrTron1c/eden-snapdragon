# Eden SX2A, Snapdragon build

A personal build of [Eden SX2A](https://git.eden-emu.dev/moceure/eden-nx2-android), tuned for
Adreno 7xx (Snapdragon 8 Gen 2 and 8 Gen 3). Unofficial and experimental: not Eden, and not
supported by the Eden, SX2A or Eglantine teams. No games, keys or firmware are included.

## Which APK

| File | Package name | Use |
| --- | --- | --- |
| `standard` | `dev.eden.nx2` | Start here. Installs over Eden SX2A and keeps its data |
| `antutu-id` | `com.antutu.ABenchMark` | May get higher clocks on some drivers. Can't sit alongside AnTuTu |
| `genshin-id` | `com.miHoYo.Yuanshen` | May unlock the system's own frame generation. Can't sit alongside Genshin Impact |

The last two are separate apps with their own settings and saves.

## What changed from Eden SX2A

- **Fewer render pass splits**, from Eglantine `f5009903`. They are expensive on tiled GPUs like Adreno.
- **Android performance hints for Switch 2 games.** Render threads rely on these for core placement,
  and only the Switch 1 present path was reporting frames.
- **Performance screen**: frame rate, frame generation, resolution, upscaling and accuracy in one
  place. Open it from Home, from a game's Properties (per game), or use the same controls in-game
  in Quick Settings.
- **Custom frame rate**: Auto, Game default, or any Custom target from 10 to 120 FPS. Auto locks
  Pokémon Pokopia to 30 FPS. A target above the game's own rate has no effect.
- **More resolution steps**: 0.625X and 0.875X. Resolution changes apply the next time the game starts.
- **Upscaling filter names fixed**: MMPX, Zero-Tangent, B-Spline, Mitchell and Spline-1 were showing
  the wrong labels.
- **Built-in frame generation works without Lossless Scaling.** It used to be greyed out unless the
  Lossless Scaling library was installed. Off by default.
- **Shader packs** (long-press a game, then Properties): export a game's shaders to a zip, then import
  it or download it from a link on another device, so they are ready before you play. A pack must
  come from this same build, ideally on the same GPU family. None is included, because a pack is
  built from the game's own shaders.
