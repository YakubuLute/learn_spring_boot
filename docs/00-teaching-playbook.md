# Teaching Playbook

The style is the same in every session, so learners always know what's coming next.
This page explains that style so you can run the sessions, or write new ones the same way.

## The core belief

> Beginners don't learn Spring by reading about Spring. They learn by **predicting** what will
> happen, **seeing** what actually happens, and **explaining** the difference.

Spring feels like magic because a lot happens out of sight. Your job is to make the hidden parts visible:
print the beans, show the SQL, read the stack trace together, decode the Basic-auth header.

## The session rhythm (≈ 2.5–3 hours)

Every session guide uses these blocks, in this order:

| Block | Icon | Time | What happens |
|-------|------|------|--------------|
| Hook | 🪝 | 5 min | A concrete problem the learners feel *before* you give them the tool. ("Restart the app. Where did your todos go?") |
| Big idea | 💡 | 10–15 min | One sentence + one everyday analogy + one diagram. No code yet. |
| Code-along | 🛠️ | 60–75 min | **I do → We do → You do.** You type, they type with you, then they finish a piece alone. |
| Predict → Run → Explain | 🔮 | sprinkled | Before running something, everyone commits to a prediction (thumbs, chat, sticky note). Then run. Then discuss *why*. |
| Break it on purpose | 💥 | 15 min | Deliberately cause the most common errors so learners see them *with you* first, not alone at 11 pm. |
| Checkpoint | ✅ | 5 min | A command with expected output. Nobody moves on until it's green for everyone. Pair the fast with the stuck. |
| Exercises | 🧪 | 30–45 min | 🟢 core (everyone), 🟡 stretch, 🔴 challenge. |
| Quiz | ❓ | 10 min | 5 questions, answers hidden in `<details>`. Use them as an exit ticket. |
| Recap | 🧭 | 5 min | Three sentences. Learners write their own before you show yours. |
| Facilitator notes | 🎓 | — | For you: common mistakes, questions learners ask, what to skip if you're short on time. |

### I do → We do → You do

1. **I do:** You type live, thinking out loud ("I'm adding `@PathVariable` because the id is *inside* the URL…"). Don't paste code. Typing slowly is the point.
2. **We do:** Learners type the next, similar piece with you. You ask them to call out the next line.
3. **You do:** They write the last similar piece alone (for example, you build GET-by-id together, then they build DELETE alone).

### Predict → Run → Explain

Put the question up *before* running. Example from Session 2:

> 🔮 *We have two classes implementing `Notifier` and neither has `@Primary`. What happens when we start the app?*
> A) Spring picks the first one alphabetically  B) Spring uses both  C) The app refuses to start

Wrong predictions are good: the surprise is what makes the lesson stick. Never laugh at a wrong prediction. Ask "what made you think that?"

### Break it on purpose

Every session has a 💥 section. Run it as a game:

- Make the change on screen.
- Learners predict the error *in their own words*.
- Run it, then read the error together **from the bottom of the stack trace up** to the `Caused by:` line.
- Undo the change.

Spring error messages are long but good. After you've read a few together, learners stop panicking at them, and that's the main skill you're building.

## Room management

- **Stuck signal:** a red sticky note or a "🔴" in chat means "I'm stuck". Have a learner at a green ✅ checkpoint go and help them before you step in.
- **Pairs:** in exercises, one *driver* (types) and one *navigator* (reads the guide, spots typos). Swap every 15 min.
- **Parking lot:** good questions that are off-topic go on a "parking lot" list that you answer at the end.
- **Same app every session:** don't start fresh projects. Learners feel the app grow, and every session's change has a reason.

## How to prepare a session

1. Run the finished app and all `curl` commands in the guide yourself.
2. Do the 💥 "break it" steps once so you've seen each error message.
3. Pick which 🧪 exercises are mandatory for your group.
4. Have the finished code (this repo) open in a second window. If the room falls behind, copy-paste the file and keep the *concept* moving.

## Assessing learners

Per session, a learner is "done" when they can:

- ✅ pass every checkpoint,
- 🧪 finish the 🟢 core exercises,
- ❓ score at least 4/5 on the quiz,
- 🗣️ explain the session's big idea in one sentence to a peer.

The final capstone (after Session 6): learners add a new feature end to end, for example **tags on todos**
(entity + repository + service + REST + GraphQL + security rule + rate limit + tests).
