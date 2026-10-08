#!/usr/bin/env bash
# PostToolUse hook — formats the touched Kotlin/Gradle file via Spotless.
# Runs only for .kt / .kts, resolves the module from the path, calls <module>:spotlessApply.
# Spotless failures do not block the agent — hidden behind `|| true`.

set -euo pipefail

input_json=$(cat)
file=$(printf '%s' "$input_json" | jq -r '.tool_input.file_path // empty')

[[ -z "$file" ]] && exit 0
[[ "$file" == *.kt || "$file" == *.kts ]] || exit 0
[[ -f "$file" ]] || exit 0

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
rel=$(python3 -c "import os,sys; print(os.path.relpath(sys.argv[1], sys.argv[2]))" "$file" "$project_dir" 2>/dev/null || echo "$file")

# Resolve the module from the first two path segments.
# Examples:
#   app/src/main/...                    → :app
#   core/data/src/main/...              → :core:data
#   feature/monitor/src/main/...        → :feature:monitor
#   build-logic/convention/src/main/... → :build-logic:convention (separate included build)
module=""
IFS='/' read -r seg1 seg2 _ <<< "$rel"
case "$seg1" in
  app)
    module=":app"
    ;;
  core|feature|benchmark)
    [[ -n "$seg2" && -d "$project_dir/$seg1/$seg2" ]] && module=":$seg1:$seg2"
    ;;
  build-logic)
    # Included build — spotless lives inside it
    (cd "$project_dir/build-logic" && ./gradlew -q "${seg2:-convention}:spotlessApply" >/dev/null 2>&1) || true
    exit 0
    ;;
  *)
    exit 0
    ;;
esac

[[ -z "$module" ]] && exit 0

(cd "$project_dir" && ./gradlew -q "${module}:spotlessApply" >/dev/null 2>&1) || true
