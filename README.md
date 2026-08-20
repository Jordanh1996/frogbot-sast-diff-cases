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

## Observed results (2026-08-20, frogbot v3.5.0, tokyoshiftleft)

"Diff scan reported" is the source-branch scan's own output (location-grouped count) from the run
log; "PR comment" is what the developer actually saw.

| PR | Case | Diff scan reported | Hypothesis | PR comment |
|----|------|--------------------|------------|------------|
| 1 | control-unrelated | nothing | ✅ | green banner |
| 2 | control-new-vuln | 1 — `NewVuln.java` | ✅ | **green banner — finding swallowed** |
| 3 | add-source | 1 — at `Sink.java` (untouched) | ✅ | green banner — swallowed |
| 4 | remove-source | nothing | ✅ | green banner |
| 5 | modify-middle | 1 — at `Sink.java` (untouched) | ✅ resurfaced | green banner — swallowed |
| 6 | benign-middle | nothing | ✅ | green banner |
| 7 | modify-sink | 1 — at `Sink.java` | ✅ | green banner — swallowed |
| 8 | remove-sink | nothing | ✅ | green banner |
| 9 | add-flow-middle | **nothing** | ❌ new route ≠ new flow | green banner |
| 10 | tech-change | **2 — `Sink.java` + `Legacy.java` canary** | ✅ baseline loss proven | only foreign SCA violations; SAST section says "Not Found" |
| 11 | sink-line-shift | nothing | ✅ fingerprint survives | green banner |
| 12 | rename-middle | 1 — at `Sink.java` | ✅ resurfaced | green banner — swallowed |

### Case 10 — the baseline-loss proof

Debug log of the re-run (`JFROG_CLI_LOG_LEVEL=DEBUG`):

```
Searching for target '' with technology 'Pip' in results with base path '/tmp/jfrog.cli.temp.-…'
Comparing target /tmp/jfrog.cli.temp.-… [unknown], relative: ''
No target found
```

and no `Diff mode - SAST results to compare provided` line — the source scan ran without a
baseline. Case 5's log has `Found target …` followed by `Diff mode - SAST results to compare
provided`. One `requirements.txt` flips a repo from "diffed" to "everything is new".

### Two findings beyond the diff mechanics

1. **Violations mode swallows new SAST findings.** The tenant profile sets
   `include_vulnerabilities_and_violations: false` and a watch exists, so only violations are
   posted — and no SAST violation was ever generated server-side (the watch's violation count is 0
   across all 12 PRs, `sast` policy criteria notwithstanding). Net effect: PRs 2/3/5/7/12 introduced
   SAST findings the scanner detected, and every one of them got the green "no new security issues"
   banner.
2. **A new route between an existing source and sink is not a new finding** (case 9):
   `significant_full_path` keys on the significant steps, so alternate paths through the middle
   don't re-fire. Good for noise; worth knowing when reasoning about coverage.
