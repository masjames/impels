# Nanobanana image prompts — problem cards (v3 High Accessibility)

3 separate, self-contained prompts (nanobanana makes one image at a time, no transparency).
Each uses a solid deep-slate background so the set stays cohesive with the dark page. Slot = `.ph-card`, 4:3.

---

## Prompt 1 — `coworker-interrupt.png`
Card: "Coworker asks mid-task" — an ask arrives while you're deep in focus; impels catches it.

```
Flat vector editorial illustration, 4:3, centered, minimal with generous negative space.
Solid deep slate background, hex #0d1014. Two-tone subject only: warm amber #f0a63a as the
single accent, soft off-white #e6e4dd for line work. Clean rounded 2px strokes, high contrast,
no gradients, no shadows, no text, no logos, no watermark.
Scene: a person at a desk in deep focus on a laptop, concentric off-white focus arcs around
their head. From the side a second person leans in with an amber speech bubble; the bubble is
being caught mid-air by a small amber pin/marker (capture), not landing on the desk.
Mood: focused, calm but alert, tactile and reliable.
```

## Prompt 2 — `boss-drops-task.png`
Card: "Boss drops a task" — a must-do lands, gets parked and resurfaced at the right interval.

```
Flat vector editorial illustration, 4:3, centered, minimal with generous negative space.
Solid deep slate background, hex #0d1014. Two-tone subject only: warm amber #f0a63a as the
single accent, soft off-white #e6e4dd for line work. Clean rounded 2px strokes, high contrast,
no gradients, no shadows, no text, no logos, no watermark.
Scene: a hand entering from the top edge dropping a single task card downward; below it an
amber slotted tray/queue catches the card. A small amber clock badge sits on the tray to imply
it resurfaces later. Off-white card and hand, amber tray and clock. Sense of ordered parking,
not clutter.
Mood: focused, calm but alert, tactile and reliable.
```

## Prompt 3 — `do-it-in-a-bit.png`
Card: "'I'll do it in a bit'" — without repeat nudges a task fades; impels pings on an interval until done.

```
Flat vector editorial illustration, 4:3, centered, minimal with generous negative space.
Solid deep slate background, hex #0d1014. Two-tone subject only: warm amber #f0a63a as the
single accent, soft off-white #e6e4dd for line work. Clean rounded 2px strokes, high contrast,
no gradients, no shadows, no text, no logos, no watermark.
Scene: a single task item at center with a row of repeating amber tick/pulse marks radiating
outward at even intervals (the nag rhythm), each tick slightly stronger, ending in an amber
checkmark. A faint off-white ghosted duplicate behind it shows the task almost fading, rescued
by the recurring pings.
Mood: focused, calm but alert, tactile and reliable.
```

---

## After generating (all three)
- Keep them consistent: same #0d1014 background, same amber, same stroke weight, so the grid reads as one set.
- Compress: `cwebp -q 80 in.png -o out.webp` (playbook §4).
- Drop into `assets/placeholders/`, swap each `.ph-card` div for `<img src="assets/placeholders/NAME.webp" alt="[describe the scene]" />`.
- The card flips to a light surface on hover; a dark-background image is fine there — it reads as a framed tile.
