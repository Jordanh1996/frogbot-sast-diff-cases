# Frogbot v3 — SAST diff-scan behavior matrix

Each pull request in this repository isolates one kind of change and shows, end to end, what a
Frogbot v3 `scan-pull-request` reports for it. Runtime: `jfrog/frogbot@v3` (latest release) against
a JAS-entitled JFrog platform, SAST enabled via the tenant's default config profile, and a
`gitRepository` watch + SAST policy so violations are generated, not just vulnerabilities.

## Baseline (main)

Two cross-file taint flows share one sink, plus one self-contained finding:

| # | Flow | Finding location |
|---|------|------------------|
| F1 | `Entry.readEnvTarget` (env) → `Middle.pass` → `Sink.exec` | `Sink.java:5` |
| F2 | `Entry.readPropertyTarget` (property) → `Middle.pass` → `Sink.exec` | `Sink.java:5` |
| F3 | `Legacy.archive` — env → exec, single file | `Legacy.java:6` |

F3 exists only as a canary: no case ever touches `Legacy.java` or anything on its flow, so any PR
that reports it proves the diff baseline was lost and the whole tree was reported.

A PR scan should report **only what the PR introduces**. The engine diffs by each result's
`significant_full_path` fingerprint (a hash over the flow), and a finding is always *located at its
sink* — those two facts drive every outcome below.

## Cases

| PR | Case | Change | Files touched | Hypothesis |
|----|------|--------|---------------|------------|
| 1 | control-unrelated | edit docs file | docs | nothing reported |
| 2 | control-new-vuln | add self-contained vulnerable file | new file | 1 finding on the new file |
| 3 | add-source | new source feeding existing flow | `Entry.java` | 1 new finding at `Sink.java` (untouched) |
| 4 | remove-source | delete source F2 | `Entry.java` | nothing reported |
| 5 | modify-middle | edit statement inside `Middle.pass` | `Middle.java` | F1+F2 resurface at `Sink.java` (untouched) |
| 6 | benign-middle | add unrelated method to `Middle` | `Middle.java` | nothing reported |
| 7 | modify-sink | edit the sink statement | `Sink.java` | F1+F2 reported |
| 8 | remove-sink | delete the exec call | `Sink.java` | nothing reported |
| 9 | add-flow-middle | new route through `Middle` to `Sink` | `Entry.java`, `Middle.java` | 1 new finding at `Sink.java` |
| 10 | tech-change | add `requirements.txt` only | new file | **baseline loss**: all 3 findings incl. canary F3 |
| 11 | sink-line-shift | benign method above the sink | `Sink.java` | fingerprint stable → nothing reported |
| 12 | rename-middle | `Middle.java` → `Forwarder.java` | rename | F1+F2 resurface at `Sink.java` |

Case 10 targets `SearchTargetResultsByRelativePath` (jfrog-cli-security `utils/results/common.go`):
a technology-set mismatch between the branches drops the baseline silently, and
`newSastScanManager` treats a missing baseline as "not a diff scan".

## Reading a result

Per PR: the Frogbot comment (findings + violations sections), and the workflow debug log —
`Diff mode - SAST results to compare provided` present/absent, `No target found`, and the
`roots:` list of the scanner input.
