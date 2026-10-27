---
trigger: always_on
---

# Mandatory Code Review After Implementation

Whenever you finish implementing, modifying, debugging, or refactoring code for a user task, you MUST perform a code review before declaring the task complete.

Exception: skip this workflow for trivial changes that cannot affect behavior (comment edits, typo fixes, formatting-only changes). State in the final reply that the review was skipped and why.

## Required workflow

1. Finish the implementation.
2. Inspect the complete diff of all files changed for the task.
3. Independently review the changes for:
   - Functional correctness and edge cases.
   - Regressions in existing features, especially in shared or common components.
   - Security and data validation.
   - Error handling and resource management.
   - Type errors, lint issues, and missing tests.
4. Independent review:
   - If subagent invocation is supported in this environment, invoke an independent code-review subagent (e.g. `code-reviewer`) to review the changes. The reviewer is read-only: it reports findings and must NOT modify files.
   - If subagent invocation is not supported or the subagent is unavailable, state this explicitly in the completion report ("independent subagent review not performed") and rely on step 3.
   - Do not assume a subagent has run unless it actually ran.
5. Run relevant tests, type checks, and lint checks when available.
6. If actionable defects are found, fix them, inspect the new diff, and review the fixes again.
7. Repeat steps 3-6 for at most 3 review-fix rounds, or until no known critical or high-severity issues remain. If issues remain after 3 rounds, stop and report them with the reason they are unresolved.

## Completion report

Before declaring the task complete, report:

- Files changed.
- Review findings and fixes.
- Whether an independent subagent review was actually performed.
- Commands and tests actually executed, with results.
- Remaining risks or unverified areas.

## Constraints

- Never claim that a test passed or an independent subagent review was completed without evidence.
- Do not modify unrelated files or perform destructive operations without authorization.
- Keep fixes minimal and scoped to the task; avoid refactoring code outside the diff.