# bench

A cross-language benchmark suite that times [avin](https://github.com/avin-lang/avin), a natively
compiled, garbage-collected language, against the natively compiled high-level languages it is closest
to: **Scala Native, Kotlin/Native, Swift, OCaml, Go and Crystal**, with **C and Rust** as ceiling
references rather than rivals. The programs are six from the [Computer Language Benchmarks
Game](https://benchmarksgame-team.pages.debian.net/benchmarksgame/): binary-trees, fannkuch-redux,
n-body, spectral-norm, fasta and k-nucleotide.

Its purpose is to find where avin's generated code or runtime loses, and why. Each program where
`avin-idiomatic` is more than 1.3× the best rival is traced to a cause in [FINDINGS.md](FINDINGS.md);
the latest numbers are in [RESULTS.md](RESULTS.md). Every claim is scoped: single-threaded programs,
one machine, these six programs.

## Columns

- **`avin-idiomatic`** is ordinary avin as its documentation teaches it. **This is the form that counts.**
- **`avin-tuned`** is the fastest avin written under the same rules with any documented feature; a
  copy of the idiomatic program where nothing beats it.
- **One plain column per rival**: idiomatic code in that language, built with its normal release
  flags and run with its runtime's defaults.
- **One tuned column per rival** (`<rival>-tuned`): the strongest single-threaded program an expert
  writes under the same rules, including the build flags and GC/runtime settings an expert would set.
- **`c` and `rust`**, plain: the ceiling. C is the baseline of every ratio.

[SOURCES.md](SOURCES.md) names the origin and licence of every file, what was changed, and exactly
what each tuned column does.

## The rules

1. **Single-threaded, in every language.** No threads, tasks or parallel loops in any program. (A
   runtime's own collector threads are the runtime's: Go's, Kotlin/Native's and Scala Native's
   collectors may use other cores, which the CPU-time table in each result shows.)
2. **The idiomatic form counts.** Tuned columns are reported beside the plain ones, never instead.
3. **Every program does the same work.** Trees are built from individually allocated nodes;
   k-mers are counted in the language's standard hash table, keyed by the 2k-bit code; fasta picks
   each symbol by scanning the cumulative probabilities.
4. **Output is verified before anything is timed.** A wrong answer stops the run.
5. **Plain release flags**: avin `avin build -O2` (its release level); C `clang -O3 -march=native`;
   Rust `-C opt-level=3 -C target-cpu=native`; Swift `-O`; Go `go build`; OCaml `ocamlopt` (default
   switch); Crystal `crystal build --release --no-debug`; Kotlin/Native `kotlinc-native -opt`; Scala
   Native `scala-cli package --native --native-mode release-fast`.
6. **Checks that remain.** avin keeps integer overflow traps and bounds checks (it has no switch to
   remove them). Swift `-O`, Rust, Go, OCaml, Kotlin/Native, Scala Native and Crystal keep their bounds
   checks; Swift also traps on overflow, and Crystal raises on overflow. C has neither. The tuned
   columns of Swift (`-Ounchecked`), Go (`-B`) and OCaml (`-unsafe`) drop bounds checks.

## Layout

```
programs/<name>/<column>/   one source file per directory
expected/ref/               the Benchmarks Game's output (and input) at a small size
expected/<name>.sha256      sha256 of the output at the timed size, from the C program
scripts/build.zsh           builds everything into build/ and generates k-nucleotide's input
scripts/run.zsh             verifies, times, and writes results/<date>.{json,md}
results/                    one JSON and one Markdown table per run, and raw-<date>/ with every measurement
```

## Running it

Requires avin, clang, rustc, swiftc, go, OCaml through opam (a default switch, and a switch named
`flambda` made with `opam switch create flambda --packages=ocaml-variants.5.4.1+options,ocaml-option-flambda`),
crystal, kotlinc-native, scala-cli and [hyperfine](https://github.com/sharkdp/hyperfine) 2.x.

On this machine every build and timing goes through marshal, the timing run with `--exclusive`:

```
caffeinate -dimsu marshal submit --kind build --deadline 90m --label "avin-bench build" --dir . -- zsh scripts/build.zsh
caffeinate -dimsu marshal submit --kind build --deadline 60m --label "avin-bench verify" --dir . -- zsh scripts/run.zsh --verify
caffeinate -dimsu marshal submit --exclusive --quiet-load auto --deadline 150m --label "avin-bench timing" --dir . -- zsh scripts/run.zsh
```

`run.zsh` times one warm-up and five runs per column (`BENCH_WARMUP`, `BENCH_RUNS`), keeping the raw
hyperfine JSON in `results/raw-<date>/`.
