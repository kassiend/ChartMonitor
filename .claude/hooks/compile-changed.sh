#!/usr/bin/env bash
# Stop hook — checks compilation of affected modules at the end of an agent turn.
# Collects the list of changed files via git and compiles the matching modules.
# If the repo is not initialized or there are no changes — exits silently.

set -euo pipefail

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$project_dir"

# git available? repo initialized?
command -v git >/dev/null 2>&1 || exit 0
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || exit 0

# List of changed .kt / .kts files (staged + unstaged + untracked).
changed=$(
  { git diff --name-only --diff-filter=d
    git diff --cached --name-only --diff-filter=d
    git ls-files --others --exclude-standard
  } 2>/dev/null | grep -E '\.(kt|kts)$' | sort -u || true
)

[[ -z "$changed" ]] && exit 0

# Collect the set of modules.
modules=()
while IFS= read -r f; do
  IFS='/' read -r seg1 seg2 _ <<< "$f"
  case "$seg1" in
    app)
      modules+=(":app:compileDebugKotlin")
      ;;
    core|feature|benchmark)
      [[ -n "$seg2" && -d "$seg1/$seg2" ]] && modules+=(":$seg1:$seg2:compileDebugKotlin")
      ;;
    build-logic)
      # included build — has its own gradlew
      (cd build-logic && ./gradlew -q "${seg2:-convention}:assemble" >/dev/null 2>&1) || true
      ;;
  esac
done <<< "$changed"

# Dedup.
if [[ ${#modules[@]} -gt 0 ]]; then
  uniq_modules=($(printf '%s\n' "${modules[@]}" | sort -u))
  ./gradlew -q "${uniq_modules[@]}" >/dev/null 2>&1 || {
    echo "⚠️  compile-changed hook: compilation failed for: ${uniq_modules[*]}" >&2
    exit 0
  }
fi
