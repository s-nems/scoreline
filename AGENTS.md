# AGENTS.md

`scoreline` is a small Java library for a live football scoreboard: start a match, update its score, finish
it, and list the matches in progress by total score, then by most recent start.

## Commands

| Task | Command |
|---|---|
| Format | `./gradlew spotlessApply` |
| Build, analyse, test | `./gradlew build` |
| One test class | `./gradlew test --tests '*SummaryTest'` |

Run the formatter and the build as two invocations. In one, the formatter rewrites files while the compiler
reads them.

## Design

- `Scoreboard` is the only entry point. It takes commands and ids from `dto` and returns `MatchDto`.
  Everything else in its package is package-private and never leaves it.
- `LiveMatches` is an immutable aggregate: each operation returns the next state. `Scoreboard` swaps it under
  one lock and publishes it through a volatile field, so reads take no lock.
- A match is identified by the `MatchId` it gets when it is started. A team plays one match at a time, by name,
  ignoring case and surrounding whitespace.
- Scores are absolute and may go down, which is how a goal is corrected.
- "Most recently started" follows a start sequence in the aggregate. The library never reads the time.

## Conventions

- Classic Javadoc, one or two sentences. Tags only for exceptions a caller can get.
- `var` for locals, static imports, `requireNonNull(value)`, messages with `formatted`.
- Logging is `log.info` for what happened and `log.warn` for what was rejected, inline.
- Tests are plain `@Test` methods named `should...`, with `// given`, `// when`, `// then`.
- Sample data comes from `SampleMatches`.

A step is done when `./gradlew build` passes.

## Working here

- Follow the conventions already in the code and the tests.
- Test first, and only through the public API: `Scoreboard`, `dto` and `exception`.
- Raise an ambiguous requirement instead of guessing, and record the answer in `README.md`.
- Each implementation step is reviewed by a separate agent.
- Conventional Commits as `type: Description`, one logical change per commit. Never push.
