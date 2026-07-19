# intervvval landing — iteration scaffolds

Three style directions to compare and pick from. Each file is now a **full assembled landing page** (header, hero, problem, how-it-works, why-not-a-todo, waitlist, FAQ, footer) with the timer-icon logo. Same copy (from `../../PRE-LANDING.md`), same component system, different visual direction. Image slots are still placeholders.

## View
Open any file directly in a browser (no build step):
```
open v1-calm-focus.html
open v2-indie-warm.html
open v3-editorial-split.html
```

## Variants
| File | Direction | Distinctive | Fonts (Fontshare) |
|------|-----------|-------------|-------------------|
| `v1-calm-focus.html` | Focused | Crisp high-contrast surface for heavy work, signal-green accent, interval-tick motif, dark inverted focus-mode band | Clash Display + General Sans |
| `v2-indie-warm.html` | Indie Warm | Off-white, terracotta accent, centered, testimonial chip + avatar proof, rounder | Cabinet Grotesk + Satoshi |
| `v3-editorial-split.html` | High Accessibility | Dark, high-contrast for night + low-vision use, legible sans (no serif), amber signal accent, split hero + card grid | Clash Grotesk + Switzer |

Fonts are deliberately off the Inter/Roboto/Fraunces AI-default set. Each variant uses a different pairing.

## Interaction rules baked in
- **Emphasize by de-emphasize:** one primary filled button per row; secondary/tertiary actions are link-style (underline), not buttons.
- **Dangerous actions** (delete etc.) are link-style in orange/red (`.link-danger`), never a filled button.
- **Hover = lighter** than base color (not darker).

## Shared system (`shared/`)
- `shadcn.css` — ported recipes: buttons, `.link-action`/`.link-danger`, badges/tags, card, input, accordion, header/footer, image placeholder slots, layout helpers. Driven by CSS vars.
- `tokens.css` — per-variant theme classes (`.theme-focus`, `.theme-warm`, `.theme-a11y`) swapping colors, radius, fonts. All-sans, no serif. One system (playbook §5).

Switching a variant = swap the theme class on `<html>`. Fonts via Google Fonts CDN.

## Ban list honored (playbook §7b)
No em-dashes in copy · mixed component shapes (no pill-everything) · no purple/blue gradient / glassmorphism · plain-founder copy, no hype · no symmetric default card grid used as the only layout · placeholders labelled, not stock imagery.

## Not built yet
Final assembled landing page, live demo widget wiring, form backend, real images. Pick a direction first.
