---
description: Manually review all current uncommitted changes
---

1. Run `git status` and `git diff` to collect all changes.
2. Check each changed file for: functional correctness and edge cases, regressions in existing features (especially shared or common components), security and input validation, error handling and resource cleanup, type or lint errors, missing tests.
3. Run available tests, type checks and lint, and record the actual results.
4. Report findings grouped as Critical / Major / Minor with file and line, then list remaining risks.
5. Review only. Do not fix anything unless asked.