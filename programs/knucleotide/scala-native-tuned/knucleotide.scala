// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
//
// k-nucleotide for Scala Native: a port of the C program (knucleotide.gcc), written for this
// suite. Every k-mer is counted into the standard mutable HashMap, keyed by its 2k-bit code.

import scala.collection.mutable
import scala.io.StdIn

object knucleotide:
  def code(c: Char): Long = c match
    case 'a' | 'A' => 0
    case 'c' | 'C' => 1
    case 'g' | 'G' => 2
    case _ => 3

  def decode(key: Long, k: Int): String =
    (k - 1 to 0 by -1).map(i => "ACGT"(((key >> (2 * i)) & 3).toInt)).mkString

  def countAll(dna: Array[Byte], k: Int): mutable.HashMap[Long, Int] =
    val counts = mutable.HashMap.empty[Long, Int]
    val mask = (1L << (2 * k)) - 1
    var key = 0L
    for i <- dna.indices do
      key = ((key << 2) & mask) | dna(i)
      if i >= k - 1 then counts(key) = counts.getOrElse(key, 0) + 1
    counts

  def frequencies(dna: Array[Byte], k: Int, out: StringBuilder): Unit =
    val total = (dna.length - k + 1).toDouble
    for (key, n) <- countAll(dna, k).toVector.sortBy((key, n) => (-n, key)) do
      out ++= f"${decode(key, k)} ${100.0 * n / total}%.3f\n"
    out += '\n'

  def count(dna: Array[Byte], fragment: String, out: StringBuilder): Unit =
    val key = fragment.foldLeft(0L)((acc, c) => (acc << 2) | code(c))
    out ++= s"${countAll(dna, fragment.length).getOrElse(key, 0)}\t$fragment\n"

  def main(args: Array[String]): Unit =
    var line = StdIn.readLine()
    while line != null && !line.startsWith(">THREE") do line = StdIn.readLine()
    val dna = mutable.ArrayBuilder.make[Byte]
    line = StdIn.readLine()
    while line != null && !line.startsWith(">") do
      for c <- line do dna += code(c).toByte
      line = StdIn.readLine()
    val bytes = dna.result()

    val out = StringBuilder()
    frequencies(bytes, 1, out)
    frequencies(bytes, 2, out)
    for fragment <- Seq("GGT", "GGTA", "GGTATT", "GGTATTTTAATT", "GGTATTTTAATTTATAGT") do
      count(bytes, fragment, out)
    print(out)
