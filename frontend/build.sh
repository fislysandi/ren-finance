#!/bin/bash
set -euo pipefail
cd /build/frontend
echo "=== Checking find-resource ==="
clojure -M -e '(require (quote cljd.compiler)) (println "find-resource:" (cljd.compiler/find-resource "ren_finance/main.cljd"))' 2>&1
echo "=== Running cljd compile with verbose ==="
clojure -M:cljd compile 2>&1 || true
echo "=== Checking output ==="
ls -la lib/cljd-out/ 2>/dev/null || echo "NO cljd-out"
ls -la lib/cljd-out/ren-finance/ 2>/dev/null || echo "NO ren-finance output"
echo "DONE"
