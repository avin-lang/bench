// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
//
// fannkuch-redux for Scala Native: a port of the C program (fannkuchredux.gcc-3), written for
// this suite. Every permutation in the counting order, flipped until a 1 leads.

object fannkuchredux:
  def fannkuch(n: Int): (Int, Int) =
    val perm1 = Array.tabulate(n)(i => i)
    val perm = new Array[Int](n)
    val count = new Array[Int](n)
    var maxFlips = 0
    var checksum = 0
    var permCount = 0
    var r = n
    while true do
      while r != 1 do
        count(r - 1) = r
        r -= 1
      System.arraycopy(perm1, 0, perm, 0, n)

      var flips = 0
      var k = perm(0)
      while k != 0 do
        var i = 0
        var j = k
        while i < j do
          val t = perm(i)
          perm(i) = perm(j)
          perm(j) = t
          i += 1
          j -= 1
        k = perm(0)
        flips += 1
      if flips > maxFlips then maxFlips = flips
      checksum += (if permCount % 2 == 0 then flips else -flips)

      var more = true
      while more do
        if r == n then return (checksum, maxFlips)
        val first = perm1(0)
        var i = 0
        while i < r do
          perm1(i) = perm1(i + 1)
          i += 1
        perm1(r) = first
        count(r) -= 1
        if count(r) > 0 then more = false else r += 1
      permCount += 1
    (checksum, maxFlips)

  def main(args: Array[String]): Unit =
    val n = args(0).toInt
    val (checksum, flips) = fannkuch(n)
    println(checksum)
    println(s"Pfannkuchen($n) = $flips")
