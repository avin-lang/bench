#!/bin/zsh
# Verifies every program's output, then times every program with hyperfine.
#
#   scripts/run.zsh             verify, then time, then write results/<date>.{json,md}
#   scripts/run.zsh --verify    verify only (further arguments name the languages)
#
# Verification runs first and any wrong answer stops the run before a single timing is taken.
# Each program is checked twice: at its small size against the Benchmarks Game's published output
# (expected/ref/), and at the timed size against the sha256 in expected/<program>.sha256.
# Run it through marshal, the timing run with --exclusive (README, Running it).

source ${0:A:h}/lib.zsh
cd $ROOT

verify_only=0
[[ $1 == --verify ]] && verify_only=1
if (( verify_only && $# > 1 )); then LANGS=(${@:2}); fi

RUNS=${BENCH_RUNS:-5}
WARMUP=${BENCH_WARMUP:-1}

# Runs the program for a size and writes its output to stdout.
run_program() {  # <program> <language> <small|big>
  local prog=$1 lang=$2 size=$3
  local cmd=(${(z)$(runner $prog $lang)})
  if [[ $BIG[$prog] == - ]]; then
    if [[ $size == small ]]; then $cmd < expected/ref/$prog.in; else $cmd < $INPUT; fi
  elif [[ $size == small ]]; then
    $cmd $SMALL[$prog]
  else
    $cmd $BIG[$prog]
  fi
}

failures=0
print "== verifying output"
for prog in $PROGRAMS; do
  for lang in $LANGS; do
    if [[ ! -x $(exe $prog $lang) ]]; then
      print "MISSING  $prog/$lang (run scripts/build.zsh)"
      failures=$((failures + 1))
      continue
    fi
    if ! run_program $prog $lang small | cmp -s - expected/ref/$prog.out; then
      print "WRONG    $prog/$lang at the small size"
      failures=$((failures + 1))
      continue
    fi
    got=$(run_program $prog $lang big | shasum -a 256 | cut -d' ' -f1)
    want=$(cut -d' ' -f1 expected/$prog.sha256)
    if [[ $got != $want ]]; then
      print "WRONG    $prog/$lang at the timed size"
      failures=$((failures + 1))
    else
      print "ok       $prog/$lang"
    fi
  done
done

if (( failures > 0 )); then
  print "== $failures program(s) failed verification; no timings taken"
  exit 1
fi
(( verify_only )) && exit 0

stamp=$(date +%Y-%m-%d)
raw=$ROOT/results/raw-$stamp
rm -rf $raw
mkdir -p $raw

print "== timing ($WARMUP warm-up, $RUNS runs per column)"
uptime > $raw/load-start.txt
for prog in $PROGRAMS; do
  args=(-N --warmup $WARMUP --runs $RUNS --output null --export-json $raw/$prog.json)
  cmds=()
  if [[ $BIG[$prog] == - ]]; then
    args+=(--input $INPUT)
    for lang in $LANGS; do cmds+=(-n $lang "$(runner $prog $lang)"); done
  else
    for lang in $LANGS; do cmds+=(-n $lang "$(runner $prog $lang) $BIG[$prog]"); done
  fi
  print -- "-- $prog"
  hyperfine $args $cmds || exit 1
done
uptime > $raw/load-end.txt

python3 -I scripts/report.py $raw $stamp
