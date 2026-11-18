#!/bin/zsh
# Builds every program in every language into build/<program>/<language>, then generates the
# standard input for k-nucleotide. Each failure is reported and the build exits non-zero at the end.
#
#   scripts/build.zsh [<language>...]      the languages to build (default: all)
#
# Run it through marshal (README, Running it).

source ${0:A:h}/lib.zsh
cd $ROOT

(( $# )) && LANGS=($@)

# OCaml writes its .cmx and .o files beside the source, so each program is compiled from a copy.
# The tuned column uses the flambda switch; the plain one the default switch.
ocaml_build() {  # <program> <language> <source dir> <out> <flags...>
  local prog=$1 lang=$2 src=$3 out=$4; shift 4
  local work=$BUILD/.ocaml/$prog-$lang
  rm -rf $work; mkdir -p $work
  cp $src/$prog.ml $work/
  (cd $work && "$@" -o $out $prog.ml)
}

failed=0
for prog in $PROGRAMS; do
  mkdir -p $BUILD/$prog
  for lang in $LANGS; do
    dir=programs/$prog/$lang
    out=$BUILD/$prog/$lang
    rm -f $out
    case $lang in
      avin-*) avin build -O2 $dir/$prog.avin -o $out >$out.log 2>&1 ;;
      c)      clang -std=gnu17 -O3 -march=native -w -I$dir $dir/$prog.c -o $out -lm >$out.log 2>&1 ;;
      rust)   rustc --crate-name bench --edition 2021 -C opt-level=3 -C target-cpu=native $dir/$prog.rs -o $out >$out.log 2>&1 ;;
      swift)  swiftc -O $dir/$prog.swift -o $out >$out.log 2>&1 ;;
      swift-tuned)
              swiftc -Ounchecked -wmo $dir/$prog.swift -o $out >$out.log 2>&1 ;;
      go)     go build -o $out $dir/$prog.go >$out.log 2>&1 ;;
      go-tuned)
              go build -gcflags=-B -o $out $dir/$prog.go >$out.log 2>&1 ;;
      ocaml)  ocaml_build $prog $lang $dir $out ocamlopt >$out.log 2>&1 ;;
      ocaml-tuned)
              ocaml_build $prog $lang $dir $out opam exec --switch=flambda -- ocamlopt -O3 -unsafe -inline 200 >$out.log 2>&1 ;;
      crystal) crystal build --release --no-debug -o $out $dir/$prog.cr >$out.log 2>&1 ;;
      crystal-tuned)
              crystal build --release --no-debug --mcpu native -o $out $dir/$prog.cr >$out.log 2>&1 ;;
      kotlin-native)
              kotlinc-native -opt -o $out $dir/$prog.kt >$out.log 2>&1 && mv $out.kexe $out ;;
      kotlin-native-tuned)
              kotlinc-native -opt -Xbinary=gc=cms -o $out $dir/$prog.kt >$out.log 2>&1 && mv $out.kexe $out ;;
      scala-native)
              scala-cli --power package --server=false --native --native-mode release-fast -f -o $out $dir/$prog.scala >$out.log 2>&1 ;;
      scala-native-tuned)
              scala-cli --power package --server=false --native --native-mode release-full --native-lto thin --native-gc immix -f -o $out $dir/$prog.scala >$out.log 2>&1 ;;
    esac
    if [[ -x $out && -s $out ]]; then
      print "built  $prog/$lang"
    else
      print "FAILED $prog/$lang (see build/$prog/$lang.log)"
      failed=1
    fi
  done
done

mkdir -p $BUILD/inputs
if [[ ! -s $INPUT && -x $(exe fasta c) ]]; then
  $(exe fasta c) $INPUT_N > $INPUT
fi
[[ -s $INPUT ]] && print "input  $INPUT ($(wc -c < $INPUT) bytes)"

exit $failed
