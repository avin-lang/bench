// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
//
// fasta for Kotlin/Native: a port of the C program (fasta.gcc-2), written for this suite. Each
// symbol is chosen by scanning the cumulative probabilities; lines of 60 go out through a
// StringBuilder flushed to standard output.

const val LINE = 60

const val ALU =
    "GGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCACTTTGG" +
    "GAGGCCGAGGCGGGCGGATCACCTGAGGTCAGGAGTTCGAGA" +
    "CCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAAT" +
    "ACAAAAATTAGCCGGGCGTGGTGGCGCGCGCCTGTAATCCCA" +
    "GCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGG" +
    "AGGCGGAGGTTGCAGTGAGCCGAGATCGCGCCACTGCACTCC" +
    "AGCCTGGGCGACAGAGCGAGACTCCGTCTCAAAAA"

val IUB_SYMBOLS = "acgtBDHKMNRSVWY"
val IUB_PROBS = doubleArrayOf(0.27, 0.12, 0.12, 0.27, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02)
val HOMO_SYMBOLS = "acgt"
val HOMO_PROBS = doubleArrayOf(0.3029549426680, 0.1979883004921, 0.1975473066391, 0.3015094502008)

var seed = 42

fun random(): Double {
    seed = (seed * 3877 + 29573) % 139968
    return seed.toDouble() / 139968.0
}

val out = StringBuilder()

fun flushIfFull() {
    if (out.length >= 1 shl 16) {
        print(out)
        out.clear()
    }
}

fun repeatFasta(header: String, seq: String, n: Int) {
    out.append(header).append('\n')
    val twice = seq + seq
    var start = 0
    var left = n
    while (left > 0) {
        val len = minOf(left, LINE)
        out.append(twice, start, start + len).append('\n')
        start = (start + len) % seq.length
        left -= len
        flushIfFull()
    }
}

fun randomFasta(header: String, symbols: String, probs: DoubleArray, n: Int) {
    out.append(header).append('\n')
    val cumulative = DoubleArray(probs.size)
    var sum = 0.0
    for (i in probs.indices) {
        sum += probs[i]
        cumulative[i] = sum
    }
    val last = probs.size - 1
    var left = n
    while (left > 0) {
        val len = minOf(left, LINE)
        for (k in 0 until len) {
            val v = random()
            var i = 0
            while (i < last && v > cumulative[i]) i++
            out.append(symbols[i])
        }
        out.append('\n')
        left -= len
        flushIfFull()
    }
}

fun main(args: Array<String>) {
    val n = args[0].toInt()
    repeatFasta(">ONE Homo sapiens alu", ALU, n * 2)
    randomFasta(">TWO IUB ambiguity codes", IUB_SYMBOLS, IUB_PROBS, n * 3)
    randomFasta(">THREE Homo sapiens frequency", HOMO_SYMBOLS, HOMO_PROBS, n * 5)
    print(out)
}
