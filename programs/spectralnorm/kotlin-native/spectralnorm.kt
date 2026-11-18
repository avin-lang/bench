// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
//
// spectral-norm for Kotlin/Native: a port of the C program (spectralnorm.gcc-8), written for
// this suite. Kotlin/Native has no printf, so `fixed` writes the result to nine decimals.

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sqrt

fun fixed(x: Double, decimals: Int): String {
    var scale = 1L
    repeat(decimals) { scale *= 10 }
    val r = (abs(x) * scale).roundToLong()
    val sign = if (x < 0 && r != 0L) "-" else ""
    return sign + (r / scale) + "." + (r % scale).toString().padStart(decimals, '0')
}

fun a(i: Int, j: Int): Double = 1.0 / ((i + j) * (i + j + 1) / 2 + i + 1)

fun timesA(v: DoubleArray, out: DoubleArray) {
    for (i in v.indices) {
        var sum = 0.0
        for (j in v.indices) sum += a(i, j) * v[j]
        out[i] = sum
    }
}

fun timesAt(v: DoubleArray, out: DoubleArray) {
    for (i in v.indices) {
        var sum = 0.0
        for (j in v.indices) sum += a(j, i) * v[j]
        out[i] = sum
    }
}

fun timesAtA(v: DoubleArray, out: DoubleArray, tmp: DoubleArray) {
    timesA(v, tmp)
    timesAt(tmp, out)
}

fun main(args: Array<String>) {
    val n = args[0].toInt()
    val u = DoubleArray(n) { 1.0 }
    val v = DoubleArray(n)
    val tmp = DoubleArray(n)
    repeat(10) {
        timesAtA(u, v, tmp)
        timesAtA(v, u, tmp)
    }
    var vBv = 0.0
    var vv = 0.0
    for (i in 0 until n) {
        vBv += u[i] * v[i]
        vv += v[i] * v[i]
    }
    println(fixed(sqrt(vBv / vv), 9))
}
