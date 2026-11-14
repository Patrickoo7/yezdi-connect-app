# Design notes

The app's look is inspired by [MotoBridge](https://motobridge26.web.app/), a browser dashboard for
the same bikes. Only the general visual style is reused (dark palette, card layout, type scale).
No code, images, logos or text were copied.

## Palette (dark only, suits the Galaxy M31's AMOLED screen)

| Token | Hex | Use |
|---|---|---|
| background | `#0F1115` | screen background, status bar |
| surface | `#171A21` | cards, bottom bar |
| surfaceHigh | `#1E2330` | inputs, nested cards, selected states |
| outline | `#2A2F3A` | 1 dp card borders |
| outlineVariant | `#232836` | dividers |
| text | `#E8EAF0` | primary text |
| muted | `#8A90A2` | secondary text, section labels, inactive icons |
| accent | `#FF5A1F` | primary buttons, active tab, highlights |
| ok | `#2FB673` | "connected", "allowed" |
| warn | `#E8C547` on `#3A3420` | notice boxes |
| error | `#FF7A6B` on `#3A1F1F` | error / disconnected banner |

## Type

System sans (Roboto on Android). Rough scale at the M31's width (unit ≈ 15 sp):

- **Screen title**: ~30 sp, extra bold, UPPERCASE ("RIDES"), with a muted subtitle under it.
- **Section label**: ~12 sp, bold, UPPERCASE, wide letter spacing (0.12 em), muted.
- **Stat value**: ~28 sp bold; **stat label**: ~13 sp muted.
- **Body**: ~15 sp, line height 1.5.
- **Wordmark**: bold, UPPERCASE, very wide letter spacing; first word in text color, second in accent.

## Shapes and spacing

- Card radius ~18 dp, button/input radius ~13 dp, pills fully rounded.
- Cards: surface color + 1 dp outline, ~18 dp inner padding, ~12 dp gap between cards.
- Screen side padding 16 dp.

## Layout patterns

- Top bar: wordmark on the left. A full-width status banner sits under it when something needs
  attention (e.g. bike not connected).
- Screen header: big uppercase title + muted subtitle.
- Stats: two-column grid of tiles (big number, small label).
- Primary action: full-width accent button.
- Numbered steps: accent-tinted circle with the number, text beside it.
- Bottom navigation: icon + label; the active tab gets an accent icon/label and a tinted pill.
