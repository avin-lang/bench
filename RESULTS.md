# Results

The latest timing run: **2026-11-18**. Generated tables: [results/2026-11-18.md](results/2026-11-18.md)
(also as JSON, [results/2026-11-18.json](results/2026-11-18.json)); every individual measurement is in
[results/raw-2026-11-18/](results/raw-2026-11-18/) (hyperfine's own JSON, one file per program).

**Scope.** Six single-threaded programs, one machine, one run of the suite. Nothing here says anything
about multi-threaded code, other machines or other programs.

## Setup

- **Machine**: Apple M5 Pro, 64 GB, macOS 27.0.1, on AC power, `caffeinate -dimsu`.
- **Timing**: hyperfine 2.0.0, `-N` (no shell), **1 warm-up and 5 timed runs per column**; mean ±
  sample standard deviation of wall time. Run through marshal with `--exclusive --quiet-load auto`, so no
  other marshal job ran; the load average was 5.8 at the start and 4.5 at the end, from processes
  outside marshal (a virtual machine at ~36% of a core was the busiest), so the figures are not from an
  idle machine.
- **Verification**: every column's output was checked against the reference (small size, byte for
  byte) and against the C program's sha256 at the timed size before timing began; all 96 passed.
- **Sizes**: binarytrees 18, fannkuchredux 11, nbody 50,000,000, spectralnorm 5,500, fasta 25,000,000,
  knucleotide on the output of fasta 10,000,000 (101.7 MB).

## Compilers, flags and checks

| column | compiler | flags | runtime settings | overflow checks | bounds checks |
|---|---|---|---|---|---|
| avin-idiomatic, avin-tuned | avin 0.0.32 | `avin build -O2` | defaults | on (traps) | on |
| scala-native | scala-cli 1.19.0 (Scala Native, its default Scala 3) | `--native --native-mode release-fast` | defaults | off (wraps) | on |
| scala-native-tuned | the same | `--native-mode release-full --native-lto thin --native-gc immix` | `GC_INITIAL_HEAP_SIZE=256m` | off | on |
| kotlin-native | Kotlin/Native 2.4.21 | `kotlinc-native -opt` | defaults | off (wraps) | on |
| kotlin-native-tuned | the same | `-opt -Xbinary=gc=cms` | defaults | off | on |
| swift | Apple Swift 6.4.2 | `swiftc -O` | — | on (traps) | on |
| swift-tuned | the same | `swiftc -Ounchecked -wmo` | — | off | off |
| ocaml | OCaml 5.4.1, default switch (no flambda) | `ocamlopt` | defaults (knucleotide's program sets its own minor heap) | off (wraps) | on |
| ocaml-tuned | OCaml 5.4.1 + flambda | `ocamlopt -O3 -unsafe -inline 200` | `OCAMLRUNPARAM=s=4M,o=200` | off | off |
| go | go 1.27.3 | `go build` | defaults | off (wraps) | on |
| go-tuned | the same | `go build -gcflags=-B` | `GOGC=400` | off | off |
| crystal | Crystal 1.22.3 (LLVM 23.1.6) | `crystal build --release --no-debug` | defaults | on (raises) | on |
| crystal-tuned | the same | `--release --no-debug --mcpu native` | `GC_INITIAL_HEAP_SIZE=268435456` | on | on |
| c | Apple clang 21.0.0 | `clang -std=gnu17 -O3 -march=native` | — | off | off |
| rust | rustc 1.94.0 | `-C opt-level=3 -C target-cpu=native` | — | off in release | on |

## Mean wall time, seconds (± standard deviation, 5 runs)

| program | avin-idiomatic | avin-tuned | scala-native | scala-native-tuned | kotlin-native | kotlin-native-tuned | swift | swift-tuned | ocaml | ocaml-tuned | go | go-tuned | crystal | crystal-tuned | c | rust |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| binarytrees | 0.617 ± 0.007 | 0.618 ± 0.002 | 0.513 ± 0.010 | 0.272 ± 0.005 | 0.732 ± 0.011 | 0.733 ± 0.008 | 1.994 ± 0.055 | 1.312 ± 0.025 | 0.295 ± 0.006 | 0.199 ± 0.004 | 0.560 ± 0.005 | 0.494 ± 0.005 | 0.515 ± 0.003 | 0.418 ± 0.007 | 0.659 ± 0.043 | 0.816 ± 0.027 |
| fannkuchredux | 3.566 ± 0.023 | 3.582 ± 0.059 | 1.657 ± 0.004 | 1.590 ± 0.009 | 1.611 ± 0.018 | 1.568 ± 0.035 | 1.674 ± 0.004 | 1.607 ± 0.004 | 2.158 ± 0.008 | 2.152 ± 0.006 | 1.665 ± 0.006 | 1.630 ± 0.003 | 1.684 ± 0.006 | 1.596 ± 0.008 | 1.615 ± 0.007 | 1.553 ± 0.009 |
| nbody | 3.068 ± 0.026 | 1.961 ± 0.004 | 7.817 ± 0.044 | 2.947 ± 0.023 | 2.225 ± 0.008 | 2.212 ± 0.004 | 1.617 ± 0.004 | 1.618 ± 0.021 | 1.699 ± 0.024 | 1.665 ± 0.002 | 2.575 ± 0.018 | 2.586 ± 0.006 | 1.516 ± 0.003 | 1.514 ± 0.003 | 1.615 ± 0.003 | 0.940 ± 0.004 |
| spectralnorm | 1.203 ± 0.003 | 1.205 ± 0.003 | 6.206 ± 0.067 | 0.754 ± 0.002 | 0.794 ± 0.001 | 0.793 ± 0.001 | 0.818 ± 0.009 | 0.781 ± 0.002 | 2.590 ± 0.016 | 2.664 ± 0.002 | 2.081 ± 0.008 | 3.121 ± 0.024 | 0.882 ± 0.007 | 0.814 ± 0.009 | 0.996 ± 0.006 | 0.358 ± 0.006 |
| fasta | 8.147 ± 0.113 | 2.748 ± 0.048 | 3.761 ± 0.223 | 2.714 ± 0.036 | 2.954 ± 0.016 | 2.927 ± 0.006 | 0.827 ± 0.010 | 2.189 ± 0.018 | 2.746 ± 0.009 | 2.486 ± 0.014 | 3.404 ± 0.006 | 3.391 ± 0.010 | 6.051 ± 0.018 | 6.170 ± 0.037 | 0.585 ± 0.005 | 1.103 ± 0.016 |
| knucleotide | 4.184 ± 0.018 | 3.425 ± 0.019 | 10.269 ± 0.043 | 4.850 ± 0.021 | 10.844 ± 0.131 | 11.083 ± 0.178 | 5.290 ± 0.225 | 5.174 ± 0.198 | 5.177 ± 1.613 | 2.726 ± 0.020 | 2.799 ± 0.033 | 2.795 ± 0.028 | 3.736 ± 0.081 | 3.740 ± 0.050 | 1.043 ± 0.012 | 0.883 ± 0.007 |

## Ratios

| program | avin-idiomatic ÷ C | avin-tuned ÷ C | best rival plain | avin-idiomatic ÷ it | avin-tuned ÷ it | best rival, any column | avin-tuned ÷ it |
|---|---:|---:|---|---:|---:|---|---:|
| binarytrees | 0.94 | 0.94 | ocaml 0.295 s | 2.09 | 2.10 | ocaml-tuned 0.199 s | 3.10 |
| fannkuchredux | 2.21 | 2.22 | kotlin-native 1.611 s | 2.21 | 2.22 | kotlin-native-tuned 1.568 s | 2.28 |
| nbody | 1.90 | 1.21 | crystal 1.516 s | 2.02 | 1.29 | crystal-tuned 1.514 s | 1.30 |
| spectralnorm | 1.21 | 1.21 | kotlin-native 0.794 s | 1.52 | 1.52 | scala-native-tuned 0.754 s | 1.60 |
| fasta | 13.93 | 4.70 | swift 0.827 s | 9.85 | 3.32 | swift 0.827 s | 3.32 |
| knucleotide | 4.01 | 3.28 | go 2.799 s | 1.49 | 1.22 | ocaml-tuned 2.726 s | 1.26 |
| **geometric mean** | **2.53** | **1.90** | | | | | |

Geometric means of the ratio to C for the other columns: scala-native 3.39, scala-native-tuned 1.52,
kotlin-native 2.00, kotlin-native-tuned 1.99, swift 1.63, swift-tuned 1.75, ocaml 1.83, ocaml-tuned 1.52,
go 1.89, go-tuned 1.97, crystal 1.71, crystal-tuned 1.62, rust 0.86.

**avin-idiomatic is more than 1.3× the best rival's plain column on all six programs**;
[FINDINGS.md](FINDINGS.md) traces each to a cause in the compiler, library or runtime.

## Caveats in this run

- **Some tuned settings were slower than plain**: swift-tuned fasta (2.19 s against 0.83 s) and go-tuned
  spectralnorm (3.12 s against 2.08 s). Neither was investigated in phase 1. They are reported as
  measured; a later run should look into them before the tuned columns are taken as each rival's best.
- **ocaml knucleotide varied** (5.18 ± 1.61 s); its other columns did not.
- **binarytrees at depth 18 is short** (0.2–2 s): start-up and heap growth weigh more than at the
  Benchmarks Game's depth 21.
- **Peak memory** and **CPU time** are in the generated table; Go's and Crystal's collectors used
  more CPU than wall time on binarytrees (1.37 s and 1.80 s CPU for 0.56 s and 0.52 s wall).
