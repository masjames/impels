# Image slot spec

Placeholder boxes in the scaffolds map to these real assets. Compress every asset at add-time: `sips` crop + `cwebp -q 75` (playbook §4). No raw AI PNGs committed.

| Slot | Ratio | Use | Style direction |
|------|-------|-----|-----------------|
| Hero phone mockup | 9:19 | Real app screen in a phone frame, or live demo widget | Clean, single screen, warm light. Show the capture + nag flow, not a cluttered list. |
| Demo / screenshot (wide) | 16:10 | Product screenshot or embedded live widget | Real UI, real task text ("Send deck to boss"), no lorem. |
| Illustration (square) | 1:1 | v3 flanking hero art | Simple line/flat, one color from the theme accent. No 3D blobs, no gradients. |
| Avatar | 1:1 circle | v2 testimonial + proof row | Real unfiltered faces (indiepa.ge style), not stock. |
| Icon | 40x40 | Step markers, feature glyphs | Single-stroke, matches accent. Rounded 10px container. |

## Naming
`hero-phone.webp`, `demo-wide.webp`, `illus-left.webp`, `illus-right.webp`, `avatar-01.webp`, `icon-catch.webp` ...

## Rules
- Real content over stock (playbook ban list).
- One accent color per illustration, pulled from the chosen variant's `--primary`.
- Export webp, keep under ~60KB each where possible.
