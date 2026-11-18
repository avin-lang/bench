# Shared definitions for build.zsh and run.zsh. Sourced, never run.

ROOT=${0:A:h:h}
export PATH=/Users/work/.cargo/bin:/opt/homebrew/opt/openjdk/bin:/opt/homebrew/bin:$HOME/.opam/default/bin:$PATH

PROGRAMS=(binarytrees fannkuchredux nbody spectralnorm fasta knucleotide)

# avin first, then its rivals (each plain, then tuned), then C and Rust as ceiling references.
AVIN=(avin-idiomatic avin-tuned)
RIVALS=(scala-native scala-native-tuned kotlin-native kotlin-native-tuned swift swift-tuned
        ocaml ocaml-tuned go go-tuned crystal crystal-tuned)
CEILING=(c rust)
LANGS=($AVIN $RIVALS $CEILING)

# The runtime settings a tuned column runs under, as an expert would set them (SOURCES.md says why
# each). Every other column runs with the runtime's defaults.
typeset -A TUNED_ENV
TUNED_ENV=(go-tuned "GOGC=400"
           ocaml-tuned "OCAMLRUNPARAM=s=4M,o=200"
           crystal-tuned "GC_INITIAL_HEAP_SIZE=268435456"
           scala-native-tuned "GC_INITIAL_HEAP_SIZE=256m")

# Size handed to each program as its one argument. A program whose size is - (knucleotide) takes
# no argument and reads the generated fasta file on standard input.
typeset -A BIG SMALL
BIG=(nbody 50000000 spectralnorm 5500 fannkuchredux 11 binarytrees 18 fasta 25000000 knucleotide -)
SMALL=(nbody 1000 spectralnorm 100 fannkuchredux 7 binarytrees 10 fasta 1000 knucleotide -)

# BENCH_PROGRAMS names a subset to build or verify (default: every program).
(( ${+BENCH_PROGRAMS} )) && PROGRAMS=(${=BENCH_PROGRAMS})

# The fasta size whose output is k-nucleotide's standard input.
INPUT_N=10000000
BUILD=$ROOT/build
INPUT=$BUILD/inputs/fasta-$INPUT_N.txt

exe() { print -r -- $BUILD/$1/$2; }

# The command that runs a column's executable: its tuned runtime settings, then the executable.
runner() {  # <program> <language>
  if [[ -n ${TUNED_ENV[$2]} ]]; then
    print -r -- "env ${TUNED_ENV[$2]} $(exe $1 $2)"
  else
    print -r -- "$(exe $1 $2)"
  fi
}
