# Findings

One entry per program where `avin-idiomatic` is more than 1.3× the best rival's plain column in the [latest results](RESULTS.md) (2026-11-18, avin 0.0.32): all six. Each finding is something to fix in the avin compiler, library or runtime, never a rewrite of the benchmark.

**Evidence** comes from three places, all gathered through marshal:
- `sample <pid> 2 1` on each avin executable at its timed size: 1 ms samples for 2 s. Counts below are "top of stack" samples out of roughly 1,600.
- `avin emit-llvm` of each idiomatic program, read for the code the hot functions get.
- A rebuild of spectralnorm, nbody and fannkuchredux with `AVIN_INCREMENTAL=off`. It tests whether incremental codegen units block inlining. They do not: times were identical, and spectralnorm's `a` is still a separate function.

Two costs show in every program:
- **G1. Every call site stores its trap location, and every frame links a shadow stack.** Each function stores `@"avin.trap:…"` into `ctx+24` before each call. It also pushes a frame of GC roots (`alloca { ptr, i64, [n x ptr] }`, linked through `ctx`), so object pointers live in memory rather than registers. Candidates: stack maps, or eliding the frame in functions that cannot collect.
- **G2. Small functions are not inlined.** Examples are spectralnorm's `a`, knucleotide's `code`, and the library's `Vector1.apply` and `mutable.Map.find`. These `tailcc` internal functions stay out of line at `-O2`, whether built as one module or as units. The rivals inline all of these.

## binarytrees: 2.09× OCaml (0.617 s against 0.295 s)

**Cause: the collector and the allocation path.**
- About 60% of samples are in the runtime: `avin_rt_trace` 141, `avin.rt$collect_stopped` 119, `avin.rt$allocate_sized` 71, and `memset`/`bzero` 45 (with a `DYLD-STUB$$bzero` per call). The program itself has `check` 200 and `make` 66.
- Each allocation goes through an out-of-line `allocate_sized` that clears the object with a libc call.
- There is no nursery: the collector traces the whole live heap on each collection.

OCaml (0.30 s; 0.20 s tuned) and Scala Native tuned (0.27 s) bump-allocate in a young generation. avin already beats C's malloc/free here (0.94×).

**Fix candidates:** an inline bump-pointer fast path with inlined zeroing (a two-field node needs two stores); a young generation.

## fannkuchredux: 2.21× Kotlin/Native (3.566 s against 1.611 s)

**Cause: `max(maxFlips, flips)` allocates a vector on every permutation.**
- The prelude's `max[T: Ord](first: T, rest: T*)` is the only `max`, so the call builds a `Vector[int]` for `rest` 40 million times at n = 11.
- Vector building: `VectorAssembler.addOne` 24, `.result` 33, `vectorLeaf` 11, plus `avin_rt_new_array`, `array_slice` and `array_copy`.
- The garbage it makes: `allocate_sized` 141, `collect_stopped` 142.
- Generic `Ord` dispatch on `int`: `avin_rt_conforms` 127, `tuple_conforms` 22.

That is about 45% of samples; `main` itself (the flips) has 853.

**Fix candidates:** lower a repeated-parameter call whose arguments are all written out to a form that does not allocate, or add fixed two-argument `max`/`min` that a monomorphic `int` call turns into compare-and-select; resolve `Ord` statically at a known type.

## nbody: 2.02× Crystal (3.068 s against 1.516 s); avin-tuned 1.29×

**Cause 1: reading a `Vector[Body]`.**
- Each `bodies(i)` checks the vector's descriptor in an `icmp` chain against `Vector0`, `Vector1`, and so on.
- It then calls the out-of-line `Vector1.apply` (219) and checks the element with `avin_rt_conforms` (151) and `tuple_conforms` (33).
- `for b in bodies` allocates a `VectorIterator` every step: `Vector.iterator` 67, `VectorIterator.next` 67, `allocate_sized` 31, `collect_stopped` 34.

The same code over an `Array[Body]` (avin-tuned) runs in 1.96 s.

**Cause 2: `math.sqrt` is a call.** It goes through the shim `avin_shim_avin_2emath_2esqrt` (94 samples) rather than `llvm.sqrt.f64`. Most of the remaining gap between avin-tuned and Crystal is this plus G1.

**Fix candidates:**
- Emit `sqrt`, `abs`, `floor`, `ceil`, `trunc`, `rint`, `fma`, `copySign`, `min` and `max` as LLVM intrinsics.
- Inline `Vector.apply` for small vectors, and drop the element `conforms` check where the static type proves it.
- Compile a `for` over a vector as a loop over its leaves, with no iterator.

## spectralnorm: 1.52× Kotlin/Native (1.203 s against 0.794 s)

**Cause: `a(i, j)` is called, not inlined.**
- `<program>.a` has 324 of 471 samples (69%). The rest is in `timesAtA`, into which `timesA` and `timesAt` were inlined.
- Its IR is `define internal tailcc double @"<program>.a"`, with five `llvm.sadd/smul.with.overflow.i32` operations, each branching to `avin_rt_trap`.
- The overflow checks are not the main cost: Swift `-O` checks too and runs in 0.82 s.
- The call is the cost: it blocks unrolling and vectorization. For reference, Rust runs in 0.36 s and C in 1.00 s.
- `AVIN_INCREMENTAL=off` still leaves `a` out of line (G2).

**Fix candidates:** let LLVM's inliner take small `tailcc` functions, or mark small leaf functions `alwaysinline`; mark trap blocks `cold`.

## fasta: 9.85× Swift (8.147 s against 0.827 s); avin-tuned 3.32×

**Cause 1 (idiomatic): `StringWriter.writeChar` makes a string per character.**
- `Writer.writeChar` is `write(renderChar(c))`: each symbol renders into a new one-character text (`avin_rt_render_char` 39, `avin.rt$new_text` 19).
- That text is appended (`avin_rt_text_append` 66, `StringWriter.write` 32), then freed.
- All of it goes through the system allocator: `_xzm_malloc_tc` 52, `_xzm_free_tc` 65, `_free` 42, `_malloc_zone_malloc` 31, and `_tlv_get_addr` 77.
- 125 million characters pay that.
- The `Vector` tables also cost out-of-line calls: `Vector1.apply[real]` 40 and `[char]` 12.

avin-tuned fills an `Array[byte]` and prints a block at a time: 2.75 s.

**Cause 2 (both forms): the generator-and-scan loop.**
- In avin-tuned, `randomFasta` has 1,323 of about 1,600 samples; output is under 15%.
- The loop does an `int` multiply-add and `%` (overflow-checked), a `real` division, and a scan of up to 15 `real`s with bounds checks, plus G1's stores around `random.next()` and `out.put`.
- Swift `-O` also checks overflow and bounds and takes 0.83 s, so the checks do not explain 3.3×. The loop's IR needs a closer read.

**Fix candidates:** make `writeChar` on `StringWriter` and `StandardOutput` append bytes directly; allocate small texts on the GC heap instead of through malloc/free; then read the tuned loop's IR.

## knucleotide: 1.49× Go (4.184 s against 2.799 s); avin-tuned 1.22×

**Cause: two out-of-line hash probes per k-mer.**
- `counts(key) = counts.getOrElse(key, 0) + 1` probes twice, once in `getOrElse` and once in `update`. C's `kh_put` probes once.
- `mutable.Map.find[long, int]` is out of line and not specialized for `long` keys: 522 samples (831 in avin-tuned). `getOrElse` has 138.
- The out-of-line clause function `code(c)` has 259 (avin-tuned).
- `charAt` scans UTF-8: `string.charAt` 63, `sysl.text$scan_at` 39.

**Fix candidates:** a single-probe update reached by the idiomatic spelling, or an `m(key) += 1` lowered to one probe as `xs(i) += 1` is for arrays; inline `find` for primitive keys; an ASCII fast path in `charAt`.
