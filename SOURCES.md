# Sources

Where every program comes from, under what licence, and what was changed.

**Benchmarks Game archive**: `https://benchmarksgame-team.pages.debian.net/benchmarksgame/download/benchmarksgame-sourcecode.zip`,
fetched 2026-11-18 (the project at https://salsa.debian.org/benchmarksgame-team/benchmarksgame).
**Licence: BSD-3-Clause**, copyright (c) 2004-2008 Brent Fulgham, 2005-2024 Isaac Gouy, reproduced in
[LICENSE-benchmarksgame.md](LICENSE-benchmarksgame.md). Each file keeps its header naming its
contributor.

**hanabi1224/Programming-Language-Benchmarks**: https://github.com/hanabi1224/Programming-Language-Benchmarks,
fetched 2026-11-18 (default branch). The repository is MIT-licensed (copyright (c) 2021 hanabi1224); the
files it took from the Benchmarks Game keep their BSD-3 headers.

**sysl-lang/bench**: the C, Rust, Swift and Go programs, `expected/` and the harness's shape are
copied from the sibling suite `sysl-lang/bench`, whose SOURCES.md records the archive file and every
change for each of them. They are unchanged here.

The avin programs, both forms, are written for this repository.

## The work every program does

Every program is single-threaded, and every language does the same work (README rule 3):

| program | reference algorithm |
|---|---|
| binary-trees | every node a separate heap allocation, built, walked and dropped |
| fannkuch-redux | every permutation in the counting order, flipping until a 1 leads |
| n-body | the pairwise advance of five bodies, then the energy |
| spectral-norm | ten rounds of A, then A transposed, applied to a vector, with A computed on the fly |
| fasta | the 139968-state generator, each symbol chosen by scanning the cumulative probabilities |
| k-nucleotide | every k-mer counted into the language's standard hash table, keyed by its 2k-bit code |

## Plain columns

| program | language | origin | changes |
|---|---|---|---|
| all six | C | sysl-lang/bench (`nbody.gcc-6`, `spectralnorm.gcc-8`, `fannkuchredux.gcc-3`, `binarytrees.gcc`, `fasta.gcc-2`, `knucleotide.gcc` + klib `khash.h`, MIT) | as there |
| all six | Rust | sysl-lang/bench (archive Rust entries, threads removed) | as there |
| all six | Swift | sysl-lang/bench (archive Swift entries, Dispatch removed; fasta a port of C) | as there |
| all six | Go | sysl-lang/bench (`binarytrees.go-7`, `fannkuchredux.go-8`, `nbody.go`, `spectralnorm.go-8`, `fasta.go-8`, `knucleotide.go-4` with integer keys, goroutines removed) | as there |
| binarytrees | OCaml | archive `binarytrees.ocaml-5.ocaml` | unchanged |
| fannkuchredux | OCaml | archive `fannkuchredux.ocaml-2.ocaml` | unchanged |
| nbody | OCaml | archive `nbody.ocaml` | unchanged |
| spectralnorm | OCaml | archive `spectralnorm.ocaml-2.ocaml` | unchanged |
| fasta | OCaml | archive `fasta.ocaml-3.ocaml` | unchanged |
| knucleotide | OCaml | archive `knucleotide.ocaml` | keyed by the 2k-bit `int` code (a `Hashtbl.Make` over `int`) instead of the k-mer string; the input reading, `Gc.set`, sorting and output are the archive's |
| binarytrees | Crystal | hanabi1224 `binarytrees/1.cr` | unchanged |
| fannkuchredux | Crystal | hanabi1224 `fannkuch-redux/1.cr` (from kostya/crystal-benchmarks-game) | unchanged |
| nbody | Crystal | hanabi1224 `nbody/1.cr` | unchanged |
| fasta | Crystal | hanabi1224 `fasta/1.cr` | unchanged |
| spectralnorm | Crystal | none | **a port of the C program, written for this suite**: hanabi1224's `1.cr` starts `u` as an `Array(Int32)` and reassigns it to `Array(Float64)`, making it a union, and allocates two arrays per pass |
| knucleotide | Crystal | none | **a port of the C program, written for this suite**, counting into the standard `Hash(UInt64, Int32)` |
| binarytrees | Kotlin/Native | hanabi1224 `binarytrees/1.kt` | unchanged |
| nbody | Kotlin/Native | hanabi1224 `nbody/1n.kt` (from the archive's Java-derived program) | its `Double.toString(9)` formatted the fraction with a `Float` power of ten and no zero padding; replaced by an exact round-to-9-decimals formatter (Kotlin/Native has no `printf`) |
| fannkuchredux, spectralnorm, fasta, knucleotide | Kotlin/Native | none | **ports of the C programs, written for this suite**: the archive's Kotlin entries use `java.*` and threads, which Kotlin/Native lacks. k-nucleotide counts into the standard `HashMap<Long, Int>` |
| all six | Scala Native | none | **ports of the C programs, written for this suite** (neither source has Scala Native programs; the archive's Scala entries were removed). k-nucleotide counts into `scala.collection.mutable.HashMap[Long, Int]` |

## Tuned columns

The rules (README) allow any documented technique and any runtime setting an expert would use, under the
same algorithm. For phase 1 the six rivals' tuned columns are **the plain source built and run the way an
expert would**; no source was changed for them yet (each tuned directory holds a copy of the plain source):

| column | build | runtime |
|---|---|---|
| swift-tuned | `swiftc -Ounchecked -wmo` (no overflow or bounds checks, whole-module optimisation) | — |
| go-tuned | `go build -gcflags=-B` (no bounds checks) | `GOGC=400` |
| ocaml-tuned | flambda switch (OCaml 5.4.1 + `ocaml-option-flambda`), `ocamlopt -O3 -unsafe -inline 200` | `OCAMLRUNPARAM=s=4M,o=200` (32 MB minor heap, less eager major GC) |
| crystal-tuned | `crystal build --release --no-debug --mcpu native` | `GC_INITIAL_HEAP_SIZE=268435456` (Boehm GC) |
| kotlin-native-tuned | `kotlinc-native -opt -Xbinary=gc=cms` | — |
| scala-native-tuned | `--native-mode release-full --native-lto thin --native-gc immix` | `GC_INITIAL_HEAP_SIZE=256m` |

## The two avin forms

`avin-idiomatic` is ordinary avin as its documentation teaches it: `data` types and clauses, `Vector`
literals for small fixed tables, `Array` for numeric buffers, the mutable `Map` for counting, `f"…"`
for formatted numbers, `println` of strings. `avin-tuned` may use any documented avin feature that
is faster, with the same algorithm:

| program | avin-tuned differs by |
|---|---|
| binarytrees, fannkuchredux, spectralnorm | nothing: no documented feature beats the idiomatic program (a copy) |
| nbody | the five bodies in an `Array[Body]` instead of a `Vector[Body]` |
| fasta | each block of 1024 lines filled into an `Array[byte]` and printed as one string, instead of a `StringWriter` and `writeChar` per symbol and a `println` per line |
| knucleotide | the sequence held as an `Array[byte]` of 2-bit codes (built through a `Buffer[byte]`) instead of a string read with `charAt` |
