// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
//
// fasta for Scala Native: a port of the C program (fasta.gcc-2), written for this suite. Each
// symbol is chosen by scanning the cumulative probabilities; lines of 60 go to a buffered
// standard output.

import java.io.{BufferedOutputStream, PrintStream}

object fasta:
  val Line = 60

  val Alu =
    "GGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCACTTTGG" +
    "GAGGCCGAGGCGGGCGGATCACCTGAGGTCAGGAGTTCGAGA" +
    "CCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAAT" +
    "ACAAAAATTAGCCGGGCGTGGTGGCGCGCGCCTGTAATCCCA" +
    "GCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGG" +
    "AGGCGGAGGTTGCAGTGAGCCGAGATCGCGCCACTGCACTCC" +
    "AGCCTGGGCGACAGAGCGAGACTCCGTCTCAAAAA"

  val Iub = Array(('a', 0.27), ('c', 0.12), ('g', 0.12), ('t', 0.27),
    ('B', 0.02), ('D', 0.02), ('H', 0.02), ('K', 0.02), ('M', 0.02), ('N', 0.02),
    ('R', 0.02), ('S', 0.02), ('V', 0.02), ('W', 0.02), ('Y', 0.02))

  val HomoSapiens = Array(('a', 0.3029549426680), ('c', 0.1979883004921),
    ('g', 0.1975473066391), ('t', 0.3015094502008))

  private var seed = 42
  def random(): Double =
    seed = (seed * 3877 + 29573) % 139968
    seed.toDouble / 139968.0

  val out = PrintStream(BufferedOutputStream(System.out, 1 << 16), false)

  def repeatFasta(header: String, seq: String, n: Int): Unit =
    out.println(header)
    val twice = seq + seq
    var start = 0
    var left = n
    while left > 0 do
      val len = math.min(left, Line)
      out.println(twice.substring(start, start + len))
      start = (start + len) % seq.length
      left -= len

  def randomFasta(header: String, table: Array[(Char, Double)], n: Int): Unit =
    out.println(header)
    val symbols = table.map(_._1)
    val cumulative = table.map(_._2).scanLeft(0.0)(_ + _).tail
    val last = table.length - 1
    val line = StringBuilder()
    var left = n
    while left > 0 do
      val len = math.min(left, Line)
      line.clear()
      for _ <- 0 until len do
        val v = random()
        var i = 0
        while i < last && v > cumulative(i) do i += 1
        line += symbols(i)
      out.println(line)
      left -= len

  def main(args: Array[String]): Unit =
    val n = args(0).toInt
    repeatFasta(">ONE Homo sapiens alu", Alu, n * 2)
    randomFasta(">TWO IUB ambiguity codes", Iub, n * 3)
    randomFasta(">THREE Homo sapiens frequency", HomoSapiens, n * 5)
    out.flush()
