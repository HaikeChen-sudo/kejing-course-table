#!/usr/bin/env bash
set -euo pipefail

# gfxinfo 的零帧分位数是占位值，不能被当成有效的性能结果。
# 用法：bash scripts/check-frame-timing.sh gfxinfo.txt [P95毫秒上限]
awk -v limit="${2:-33}" '
/^Total frames rendered:/ && !seen { frames=$4; seen=1 }
/^95th percentile:/ && !timing { p95=$3+0; timing=1 }
END {
  if (!frames || !timing) { print "INVALID: no frame samples"; exit 2 }
  printf "frames=%d P95=%dms limit=%dms %s\n", frames, p95, limit, (p95<=limit ? "PASS" : "FAIL")
  exit (p95>limit)
}' "$1"
