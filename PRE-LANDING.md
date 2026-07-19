# intervvval — pre-landing (Gate 1 smoke test)

> Single source for the smoke-test page. Copy + structure live here first, then port to `landing/index.html` once wording is settled. Goal of this page: prove demand. One job = capture emails from people who feel this pain.

---

## Positioning (the moat, pinned)

For people whose day gets hijacked by other people's asks. A coworker pings, the boss drops a task, and you are already mid-focus. intervvval catches each ask in two taps and nags you back at the right interval, so nothing slips and you stay heads-down until it does.

Not another to-do list. It reminds you again and again on an interval until the thing is actually done.

One line: **Catch every ask, stay focused, get nagged until it's done.**

---

## Page structure

### 1. Hero

**Headline:** Every "hey, can you..." lands somewhere you'll actually see it.

**Sub:** You're mid-focus, the boss drops a task, a coworker needs a thing. Two taps to catch it. intervvval reminds you on repeat until it's done, so nothing quietly dies in your head.

**Primary CTA:** Get early access
**Field:** email input + button. No other fields.

**Trust line under CTA:** No spam. One email when it's ready.

> Embed rule (playbook §7): put the real interactive capture widget here, not a screenshot. A working "add reminder -> nag preview" demo running on localStorage.

### 2. The problem (three real moments, not features)

- Coworker asks for something while you're deep in another task.
- Boss tells you to do a thing. It has to happen, but not right now.
- You tell yourself "I'll do it in a bit" and a bit becomes never.

Copy: Your head is not a reliable queue. Sticky notes don't nag. Calendar reminders fire once and vanish.

### 3. How it works (three steps)

1. **Catch it fast.** Two taps: what + who asked + how often to remind.
2. **Stay focused.** One active thing up top, the rest wait quietly below.
3. **Get nagged.** Fires every X minutes until you mark it done. Snooze or done straight from the notification.

### 4. Why not just a to-do app

Plain-spoken, no hype:
- To-do apps remind once. intervvval keeps nagging on an interval until done.
- Built for interruptions, not for planning your week.
- Capture is faster than opening a note. Two taps, back to work.

### 5. Waitlist CTA (repeat)

**Headline:** Want it to stop slipping through?
**CTA:** Get early access (same email capture as hero).

### 6. Footer

- One line on who's building it.
- Contact email.
- No fake social links.

---

## Copy ban list (applied)

- No em-dashes in copy.
- No pill-everything. Mix button/badge shapes.
- No purple/blue gradient, no glassmorphism.
- No "Supercharge / Unlock the power of" hype.
- No symmetric 3-card grid as default layout.
- Real demo widget over stock imagery.
- Voice: plain founder, not SaaS template.

---

## Decisions to resolve before HTML (log in docs/roadmap.md)

| Question | Options |
|----------|---------|
| Waitlist backend | Formspree vs Tally vs Google Form |
| Hosting | Vercel static (playbook default) |
| Demo widget | Live localStorage capture vs static |
| Name spelling | `intervvval` (three v) confirmed? |

---

## Next after this page

- [ ] Write `docs/roadmap.md` with 3-gate ladder (playbook §1).
- [ ] Port this copy to `landing/index.html`, static, no framework.
- [ ] Wire email field to chosen form backend.
- [ ] Build the localStorage demo widget to embed in hero.
