# Google Flow (Veo) — hero explainer video

Goal: 8-16s, 16:9 landscape, for the hero `.ph-video` slot. Uses the 3 nanobanana images as
scene anchors. Explains: an ask arrives → you catch it in two taps → it nags you on an interval
until done. Same dark/amber aesthetic so it's seamless with the page.

Flow makes short clips (~8s each). Build as 2 chained clips (~8s + ~8s = ~16s), or 3 short
scenes if using "extend"/scene-builder. Import the matching image as the start frame of each scene.

## Global style (keep identical across every clip)
```
Flat 2D vector motion-graphics, minimal, high contrast, generous negative space.
Solid deep slate background #0d1014. Two-tone only: amber #f0a63a accent + off-white #e6e4dd
line work. Clean rounded strokes, smooth easing, subtle motion, no camera shake, no gradients,
no photoreal, no on-screen text, no logos except the final beat. 16:9, calm-but-alert mood.
```

---

## Clip 1 (~8s) — the interruption, caught
Start frame: `coworker-interrupt.png`
```
[global style] Hold on a person in deep focus at a laptop, off-white focus arcs pulsing gently
around their head. An amber speech bubble slides in from the right (a coworker's ask). Just
before it hits the desk, a small amber pin snaps onto it — captured — and the bubble shrinks
into a neat amber task chip that tucks to the side. The person stays focused, undisturbed.
Motion: smooth slide-in, quick satisfying snap on capture, settle. ~8 seconds.
```

## Clip 2 (~8s) — parked, then nagged until done
Start frame: `boss-drops-task.png` → morph to `do-it-in-a-bit.png`
```
[global style] A hand drops a task card from the top into an amber slotted tray; a small amber
clock badge ticks once. Cut/morph to the same task at center emitting repeating amber pulse
ticks outward at even intervals (the nag rhythm), each stronger than the last. A faint off-white
ghost of the task behind it stops fading and snaps back. Final tick lands as an amber checkmark;
the task marks done.
End beat (last ~1.5s): everything clears to the deep slate; the intervval timer mark (circle with
an "i" inside) draws on in amber at center.
Motion: drop + tick, rhythmic pulses, checkmark pop, clean logo draw-on. ~8 seconds.
```

---

## Notes
- Order of meaning: catch (clip 1) → park + nag until done (clip 2). Matches page copy.
- No on-screen text until you optionally add the wordmark under the final logo in post.
- Export 16:9, then compress for web (e.g. `ffmpeg -crf 30 -vf scale=1280:-2`, mp4/h264 + a webm).
- Drop file in `assets/`, swap the `.ph-video` div for:
  `<video src="assets/hero.mp4" autoplay muted loop playsinline poster="assets/hero-poster.webp"></video>`
- If you want 3 scenes instead of 2, split clip 2 into "park" and "nag-until-done" (~5s each) to
  land in the 8-16s window.
