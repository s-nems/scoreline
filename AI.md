# AI usage

## Summary

I built this library with Claude Code. One session (Fable 5.2) acted as an orchestrator, spawning worker subagents (Opus 5.5/Haiku 5.5) for token-saving purposes. It discussed the design with me, split the work into steps and reviewed what came back. Each step went to a separate worker agent at medium effort, with a written brief: implementation, web research, build experiments on a copy of the project, and reviews.

The AI proposed, argued and wrote the code and the documents. I decided, read the code as it was written
and sent it back many times. The prompt history below shows where.

## How the output was checked

- `./gradlew build` after every step: compiler warnings as errors, Error Prone, NullAway, tests, formatting.
- Reviews by agents that had not seen the design discussion: of the plan, of the whole code, of the
  additional operation with the README, and of the finished repository. They got the statement and the
  material under review and were asked for defects. Their findings changed the design twice.
- Deliberate bugs. After each rewrite of the tests, a worker broke the production code in a known way (no
  tie-break, case-sensitive team names, `finish` returning the wrong match, no lock) and confirmed that a
  test failed.
- Experiments on a copy before a build change went in: Error Prone with NullAway on JDK 25, two formatters,
  NullAway without `package-info.java`.
- My own reading of the code. Most prompts in the second half of the history are review comments.

## Where the AI and I disagreed

| Topic | AI | Me | Result |
|---|---|---|---|
| Build tool | Maven, because the statement says so. | Gradle. | Gradle, explained in the README. |
| Score update | Absolute score. | A goal event with scorer and minute. | Absolute score. The AI's argument held: a disallowed goal would need a second operation. |
| Tie-break | A counter. A clock can repeat or go backwards. | A clock, injected so tests can freeze it. | A counter. Later the clock was removed, since nothing else needed time. |
| Concurrency | A compare-and-set loop. | Asked for the standard approach and left the choice to the AI. | One lock, after a reviewer agent showed the loop gave changes no single order. |
| Facade arguments | Plain parameters, to keep the API small. | Command objects, because they are the API of the package. | Commands: `StartMatch`, `UpdateScore`. |
| Logging | None in the core, then one `DEBUG` line per change. | `info` for each change, `warn` for each rejection, plain calls. | Mine. |
| Lombok, static mapper | Against both. | Asked about both. | Neither. |

Things the AI produced that I rejected outright: Markdown doc comments (`///`), Javadoc that restated
signatures, an 800-line test class, helper methods that built log messages, a test of log output, a 175-line
`AGENTS.md`.

## Prompt history

The orchestrator wrote this summary of our conversation at the end of the session. It keeps the order and
the substance of what I asked for, not the wording.

### Setup

| # | Prompt | Outcome |
|---|---|---|
| 1 | Build the library from the attached statement. Work as an orchestrator that hands tasks to worker agents (Opus 5.5/Haiku 5.5). JDK 25 and Gradle. Write `AGENTS.md` and a `CLAUDE.md` that points to it. Bootstrap the project with a worker, and meanwhile analyse the task and discuss technology, architecture and trade-offs with me, with web research if needed. | A worker bootstrapped the build. Another verified current tool versions. The AI asked which build tool to use (see 4). |
| 2 | The project is called `scoreline` and will be public. The last step is a clean git history in Conventional Commits. Do not push, I review everything first. | Local commits only. |
| 3 | Model the domain with DDD and keep to established engineering practice. | Tactical DDD: aggregate, value objects, domain language. |
| 4 | Gradle, with a justification in the README. | Recorded as a trade-off. |

### Design discussion

| # | Prompt | Outcome |
|---|---|---|
| 5 | Answers to the seven open decisions the AI listed. Identify a match by its teams. Expose value objects in the API. Consider modelling an update as a goal event with scorer and minute. Inject a `Clock` so that tests control time. For concurrency and for team name matching, use the standard, well-established approach. Compare the candidates for the extra feature by their value to a consumer and by how interesting they are to build. Structure the package around one public facade, with everything else package-private. | Counter-arguments on the goal event and on the clock. A comparison of candidate features. |
| 6 | What are Error Prone and NullAway? Try them and report. | Tested on a copy, then added. |
| 7 | Research well-regarded examples of DDD in small libraries and apply what fits, without over-engineering. | A report on a reference DDD repository and where a library has to differ from it. |
| 8 | Is anything in JDK 26 or 27 worth using? | No. Every language feature there is still a preview. JDK 25 stays. |
| 9 | Keep the absolute score, the argument against goal events is sound. The clock is only there so that no code calls `now()`. Use subscription as the extra feature. | Design settled. |
| 10 | Have a separate agent, with no knowledge of our discussion, review the plan against the statement. | Five findings, among them an ordering hole in subscription on top of the compare-and-set loop, and gold-plating (clock, start time, configuration class). |
| 11 | Remove the clock, the start time and the configuration class. | Done. Writes moved behind one lock. |

### Review of the code as it was written

| # | Prompt | Outcome |
|---|---|---|
| 12 | Replace the Markdown `///` comments with standard Javadoc, and keep comments concise. | Classic Javadoc. |
| 13 | Keep `AGENTS.md`, `AI.md` and `README.md` short and plain. | A writing rule set, later reduced to the conventions in `AGENTS.md`. |
| 14 | Have a reviewer agent check each implementation step. The test classes are too large, reduce them. | One test class per operation. Tests cut by a third with the same coverage. |
| 15 | Indent with four spaces. Find a widely used formatter whose output is closer to the IntelliJ default than Google's. | palantir-java-format, later replaced (see 31). |
| 16 | Conventions: no `@Nested`, static imports, `var` for local variables. | Applied. |
| 17 | `requireNonNull` without a message. Documentation is Javadoc, not `//` blocks. | Applied. |
| 18 | Explain what `serialVersionUID` does here. It serves no purpose, so remove it. | Removed, lint adjusted. |
| 19 | Name the test classes `StartingMatchTest` and so on. Structure test bodies as given, when, then. | Applied. |
| 20 | Compare a flat package with a separate package for exceptions. | The AI argued for flat. See 21. |
| 21 | Correct the direction: one facade, everything else package-private, only DTOs and the facade exposed. Tests go through the facade. | `dto` package, domain hidden, value object tests removed. Code the facade could not reach was deleted. |
| 22 | Identify a match by an id in `update` and `finish`, with a typed `MatchId` around the UUID. | Yes to both. |
| 23 | Remove `package-info.java` if nothing needs it. Move the exceptions to an `exception` package. Build messages with `formatted`. | Both `package-info.java` files removed, with the JSpecify dependency. |
| 24 | Use a lock class from the JDK in place of a bare `Object`. | `ReentrantLock`. |
| 25 | Give `start` and `update` one typed argument each. | The AI advised against it. I kept the request: `start(StartMatch)`, `update(UpdateScore)`. |
| 26 | Consider a static mapper class. | No. `LiveMatch.toDto()` is one line. |
| 27 | Add logging that states precisely what the library does, without noise. | `System.Logger` at `DEBUG` with helper methods and a test. I rejected all of it (28). |
| 28 | Simplify the logging: no test of log output, no helper methods for messages, plain `log.info` calls, no debug level. | SLF4J, inline calls. |
| 29 | Three log lines are too few. The log should show everything the library did. | `info` for each change, `warn` at each rejection. |
| 30 | Shorten the comments further. Parameters and return values need no tag, least of all in package-private classes. Javadoc does not mention logging. | Comment lines in production code dropped by two thirds. |
| 31 | Put each component of a longer record on its own line. | The formatter collapsed it, so the build moved to the Eclipse formatter, tested on a copy first. |
| 32 | Move shared test data to a fixture. Shorten test names such as `listsTheExampleFromTheExerciseStatementInTheExpectedOrder`, they are hard to read. | `SampleMatches`, names starting with `should`, the example written as five lines. |
| 33 | Validate the fields of `StartMatch` inside `StartMatch`. | Commands check their own fields for null. |
| 34 | Remove expressions from test annotations, they are hard to read. | Plain tests by default. Literal rows only. |
| 35 | `requireInProgress` calls a lookup and discards the result. Restructure it. | One lookup whose result is used. |
| 36 | `AGENTS.md` has grown too long. The conventions are in the code now, so reduce it. | Cut from 175 lines to under 50. |

### Finishing

| # | Prompt | Outcome |
|---|---|---|
| 37 | Is the git history good enough to publish? | No. See below. |
| 38 | Run independent reviews of the finished repository and list the findings. | Tests added for rules the documents stated and no test pinned: `update` and `finish` under contention, the summary a listener sees, an update to the same score. Exceptions carry their data. The jar targets Java 21. Inaccurate sentences in the documents corrected. |

Between 36 and 37 the work needed no new prompt. A reviewer agent read the whole code, as agreed in 14, and
found no defect; its findings on names and tests were applied. Then came the thread-safety test, `subscribe`
(the feature chosen in 9), and a review of that feature and the README.

The commit history was rewritten at the end. The working history had 35 small commits that followed the
review comments above back and forth. I had it rebuilt as a short series in which each commit is one logical
change and builds on its own. That is why the earlier versions this file and the README mention (identity by
team names, the compare-and-set loop, the clock) are not in the history.

## Artifacts

- [AGENTS.md](AGENTS.md): the rules every agent read before working. It grew with each correction and was
  then cut down to what the code does not show.
- [CLAUDE.md](CLAUDE.md): points Claude Code to `AGENTS.md`.
- Worker briefs. Each implementation step had a written brief with the target API, the behaviour as a list,
  the deliberate bugs to check and a required report. They are not kept in the repository.
- Research notes from workers: current versions of the build tools, the final features of JDK 25 to 27, the
  layout of a reference DDD repository.
- The review reports, summarised in the tables above.
