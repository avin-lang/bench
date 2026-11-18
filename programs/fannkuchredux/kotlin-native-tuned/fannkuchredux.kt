// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
//
// fannkuch-redux for Kotlin/Native: a port of the C program (fannkuchredux.gcc-3), written for
// this suite. Every permutation in the counting order, flipped until a 1 leads.

fun fannkuch(n: Int): Pair<Int, Int> {
    val perm1 = IntArray(n) { it }
    val perm = IntArray(n)
    val count = IntArray(n)
    var maxFlips = 0
    var checksum = 0
    var permCount = 0
    var r = n
    while (true) {
        while (r != 1) {
            count[r - 1] = r
            r--
        }
        perm1.copyInto(perm)

        var flips = 0
        var k = perm[0]
        while (k != 0) {
            var i = 0
            var j = k
            while (i < j) {
                val t = perm[i]
                perm[i] = perm[j]
                perm[j] = t
                i++
                j--
            }
            k = perm[0]
            flips++
        }
        if (flips > maxFlips) maxFlips = flips
        checksum += if (permCount % 2 == 0) flips else -flips

        while (true) {
            if (r == n) return Pair(checksum, maxFlips)
            val first = perm1[0]
            for (i in 0 until r) perm1[i] = perm1[i + 1]
            perm1[r] = first
            count[r]--
            if (count[r] > 0) break
            r++
        }
        permCount++
    }
}

fun main(args: Array<String>) {
    val n = args[0].toInt()
    val (checksum, flips) = fannkuch(n)
    println(checksum)
    println("Pfannkuchen($n) = $flips")
}
