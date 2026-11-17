# scoreline

A Java library for a live football scoreboard. It keeps the matches in progress and lists them by total
score.

```java
var scoreboard = new Scoreboard();

var match = scoreboard.start(new StartMatch("Mexico", "Canada"));
scoreboard.update(new UpdateScore(match.id(), 0, 5));

List<MatchDto> summary = scoreboard.summary();

var finished = scoreboard.finish(match.id());
```

## Build

```
./gradlew build
```

This compiles, runs Error Prone and NullAway, runs the tests and checks the formatting. Gradle needs a JDK 17
or newer to launch. The build downloads JDK 25 itself when the machine has none. The jar it produces runs on
Java 21 or newer.

## Assumptions

The statement leaves these open.

| Question | Decision |
|---|---|
| How is a match identified? | By a `MatchId` that `start` issues. `update` and `finish` take that id. |
| Can a team be in two matches? | No. A team plays one match at a time, at home or away. |
| When are two team names the same team? | When they match after surrounding whitespace is stripped, ignoring case. The exact rule is under "Simple name matching". |
| Can a team play itself? | No. |
| Is an update a new score or a goal? | A new score. It replaces the old one. |
| Can a score go down? | Yes. That is how a disallowed goal is corrected. |
| Is an update to the same score an update? | Yes. It is logged and listeners are told. |
| What happens to a finished match? | It leaves the scoreboard. `finish` returns it with its final score. |
| What does "most recently started" mean? | The order in which `start` was called on this scoreboard. |
| What does an invalid request do? | It throws and leaves the scoreboard unchanged. |
| Is it safe to use from several threads? | Yes. |

Invalid requests throw unchecked exceptions:

| Exception | Cause |
|---|---|
| `NullPointerException` | A `null` argument. |
| `IllegalArgumentException` | A blank team name, a team against itself, a negative score, a total above `Integer.MAX_VALUE`. |
| `TeamAlreadyPlayingException` | `start` for a team that is in a match in progress. |
| `MatchNotFoundException` | `update` or `finish` with an id that has no match in progress. |
| `IllegalStateException` | `start`, `update` or `finish` called from inside a listener. A `null` argument, a blank name and an invalid score are rejected before that. |

`TeamAlreadyPlayingException` and `MatchNotFoundException` extend `IllegalStateException`: the request is
valid, and the scoreboard is in the wrong state for it. They carry what was rejected, as `team()` and
`matchId()`.

## Reasoning

### One entry point

`Scoreboard` is the only class a caller creates and calls. It takes commands (`StartMatch`, `UpdateScore`)
and a `MatchId`, and returns `MatchDto`. These are plain records in the `dto` package. The `event` package
holds the events and the `Subscription` that `subscribe` returns. The domain classes (`Team`, `Score`,
`LiveMatch`, `LiveMatches`) are package-private.

A caller cannot build a domain object or hold one, so I can change the inside without breaking anyone.

### Identity by id

The first version identified a match by its two team names. I changed it. Names are a poor identity: the
same two teams can meet again, and one name can be written in several ways. With an id, `update` and `finish`
cannot hit the wrong match because of spelling.

### Order by start sequence

The scoreboard never reads the time. Two matches can start within the same clock tick, and a wall clock can
go backwards. A counter in the scoreboard cannot do either, so the tie-break is exact and the tests need no
clock.

### Immutable state behind one lock

`LiveMatches` holds every match in progress and is immutable. `start`, `updateScore` and `finish` return the
next state or throw. `Scoreboard` runs a change under one `ReentrantLock` and stores the result in a volatile
field.

Changes happen one at a time, so "is this team already playing?" and "add the match" cannot interleave. A
reader takes no lock. `summary()` reads the field once and returns a list that was built when the state was
created, so it sees the board before a change or after it, never halfway.

The first version used a compare-and-set loop on an `AtomicReference`. A review pointed out that it bought
nothing at this size and gave changes no single order, so I use one lock.

### Tests through the public API

Every test goes through the public API. No test knows about `LiveMatches` or `Team`, so the tests change
when the API changes and stay as they are when the internals do.

Coverage is 100% of lines and branches, as a result of that rule: code the public API cannot reach was
removed. The tests were also run against deliberate bugs, such as dropping the tie-break, comparing team
names with case or taking the lock off `update`, and each one fails a test.

The example from the statement is `SummaryTest.shouldOrderExampleFromExerciseStatement`. With the lock
removed, `ConcurrencyTest` failed in ten runs out of ten.

### Logging

The library logs through SLF4J: `info` for each change and each subscription, `warn` for each request the
scoreboard rejects, `error` for a listener that throws. A `null` argument is not logged. `summary()` is not
logged either, because it is called far more often than anything else.

## Trade-offs

### Gradle, where the statement says Maven

This departs from the statement, and I did it knowingly. The Gradle toolchain downloads JDK 25 when it is
missing, so the build is one command with no setup. The wrapper is pinned by checksum and dependency versions
sit in one catalog. The cost is that there is no `pom.xml`, so `mvn verify` fails here. The checks themselves
are not tied to Gradle: Error Prone, NullAway, Spotless and JaCoCo all run under Maven too.

### The caller keeps the id

`update` and `finish` need the `MatchId` from `start`. A caller that lost it can find it in `summary()`.

### Each change copies the list

A change builds a new sorted list, which is O(n log n), and `summary()` is O(1). A scoreboard holds a handful
of matches and is read much more often than it changes. With thousands of matches I would index them by id
and by team.

### Work under the lock

Log lines and listener callbacks run while the lock is held. That keeps them in the order of the changes. A
slow log appender or a slow listener delays other changes, never readers. Delivering events on another
thread would need a queue and a thread of the library's own, and the events could then lag behind
`summary()`.

### One dependency

Every consumer gets `slf4j-api` on its classpath. Without a logging backend SLF4J prints a short notice once
and discards the lines.

### Random ids

`Scoreboard` generates a `UUID` for each match, so an id cannot be predicted. Tests take it from what `start`
returns. The aggregate receives the id as an argument, so the domain logic has no randomness in it.

### Simple name matching

Names are trimmed with `String.strip` and compared after `toLowerCase(Locale.ROOT)`. Whitespace inside a
name is kept, so `South  Korea` with two spaces is a different team from `South Korea`. A no-break space is
not stripped, and the German sharp s does not match `SS`. There is no Unicode normalisation and no list of
aliases. A real feed would carry team ids.

### Left out on purpose

- Goals as events with scorer and minute. A disallowed goal would need a second operation to retract it, and
  a repeated message would count twice. That data belongs to a match timeline, and the scoreboard is a view
  of the current score.
- A clock and a start time. No requirement needs time once the order comes from a counter.
- A history of finished matches. `finish` returns the final score, and the caller decides where it goes.

## The additional operation: subscribe

```java
var subscription = scoreboard.subscribe(event -> {
    switch (event) {
        case MatchStarted started -> show(started.match());
        case ScoreUpdated updated -> show(updated.match());
        case MatchFinished finished -> hide(finished.match());
    }
});

subscription.cancel();
```

A listener gets one event for each successful `start`, `update` and `finish`. `ScoreboardEvent` is a sealed
interface, so the `switch` above is checked by the compiler and needs no default branch.

`Scoreboard` gains one method. `cancel` is how a subscription ends, and the three event types are what the
method delivers.

### Why this one

A scoreboard is live. Without `subscribe`, a consumer has to call `summary()` in a loop and compare lists to
find out what changed.

I considered a lookup of a match by team and a list of finished matches. A caller can build both on top of
`summary()` and `finish`. A caller cannot build push delivery.

### Rules

- Events arrive in the order the changes happened, on the thread that made the change.
- When a listener runs, `summary()` already shows the change.
- A listener must be quick, because the next change waits for it.
- A listener must not change the scoreboard. A call to `start`, `update` or `finish` from the thread a
  listener runs on throws `IllegalStateException`. Otherwise other listeners would hear about the second
  change before the first. A listener that hands the change to another thread and waits for it blocks
  forever. Two scoreboards whose listeners change each other can deadlock when both change at once.
- A listener that throws an exception is logged at `error`. The change stands, the caller gets its result
  and the other listeners get the event.
- An `Error` is not caught. It reaches the caller of `start`, `update` or `finish` although the change
  stands, and the listeners after the failing one miss the event.
- After `cancel`, an event that was already being delivered may still arrive.

### Known limit

A new subscriber gets events from then on, and no event carries a sequence number. A consumer that needs the
whole picture subscribes first, holds the events back, reads `summary()`, and then applies the events it
held. A `subscribe` that returns the current summary under the lock would remove that step.

## AI

[AI.md](AI.md) describes how AI was used, with the prompt history and the artifacts that guided the work.
