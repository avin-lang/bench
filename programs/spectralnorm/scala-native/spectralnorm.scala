// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
//
// spectral-norm for Scala Native: a port of the C program (spectralnorm.gcc-8), written for this
// suite.

object spectralnorm:
  def a(i: Int, j: Int): Double = 1.0 / ((i + j) * (i + j + 1) / 2 + i + 1)

  def timesA(v: Array[Double], out: Array[Double]): Unit =
    for i <- v.indices do
      var sum = 0.0
      for j <- v.indices do sum += a(i, j) * v(j)
      out(i) = sum

  def timesAt(v: Array[Double], out: Array[Double]): Unit =
    for i <- v.indices do
      var sum = 0.0
      for j <- v.indices do sum += a(j, i) * v(j)
      out(i) = sum

  def timesAtA(v: Array[Double], out: Array[Double], tmp: Array[Double]): Unit =
    timesA(v, tmp)
    timesAt(tmp, out)

  def main(args: Array[String]): Unit =
    val n = args(0).toInt
    val u = Array.fill(n)(1.0)
    val v = new Array[Double](n)
    val tmp = new Array[Double](n)
    for _ <- 0 until 10 do
      timesAtA(u, v, tmp)
      timesAtA(v, u, tmp)
    var vBv = 0.0
    var vv = 0.0
    for i <- 0 until n do
      vBv += u(i) * v(i)
      vv += v(i) * v(i)
    println(f"${math.sqrt(vBv / vv)}%.9f")
