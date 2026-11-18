// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
//
// k-nucleotide for Kotlin/Native: a port of the C program (knucleotide.gcc), written for this
// suite. Every k-mer is counted into the standard HashMap, keyed by its 2k-bit code.

import kotlin.math.abs
import kotlin.math.roundToLong

fun fixed(x: Double, decimals: Int): String {
    var scale = 1L
    repeat(decimals) { scale *= 10 }
    val r = (abs(x) * scale).roundToLong()
    val sign = if (x < 0 && r != 0L) "-" else ""
    return sign + (r / scale) + "." + (r % scale).toString().padStart(decimals, '0')
}

fun code(c: Char): Long = when (c) {
    'a', 'A' -> 0L
    'c', 'C' -> 1L
    'g', 'G' -> 2L
    else -> 3L
}

fun decode(key: Long, k: Int): String {
    val sb = StringBuilder()
    for (i in k - 1 downTo 0) sb.append("ACGT"[((key shr (2 * i)) and 3L).toInt()])
    return sb.toString()
}

fun countAll(dna: ByteArray, k: Int): HashMap<Long, Int> {
    val counts = HashMap<Long, Int>()
    val mask = (1L shl (2 * k)) - 1
    var key = 0L
    for (i in dna.indices) {
        key = ((key shl 2) and mask) or dna[i].toLong()
        if (i >= k - 1) counts[key] = (counts[key] ?: 0) + 1
    }
    return counts
}

fun frequencies(dna: ByteArray, k: Int, out: StringBuilder) {
    val total = (dna.size - k + 1).toDouble()
    val sorted = countAll(dna, k).entries.sortedWith(compareBy({ -it.value }, { it.key }))
    for (e in sorted) out.append(decode(e.key, k)).append(' ').append(fixed(100.0 * e.value / total, 3)).append('\n')
    out.append('\n')
}

fun count(dna: ByteArray, fragment: String, out: StringBuilder) {
    var key = 0L
    for (c in fragment) key = (key shl 2) or code(c)
    out.append(countAll(dna, fragment.length)[key] ?: 0).append('\t').append(fragment).append('\n')
}

fun main() {
    var line = readlnOrNull()
    while (line != null && !line.startsWith(">THREE")) line = readlnOrNull()
    val buffer = ByteArrayBuilder()
    line = readlnOrNull()
    while (line != null && !line.startsWith(">")) {
        for (c in line) buffer.add(code(c).toByte())
        line = readlnOrNull()
    }
    val dna = buffer.toByteArray()

    val out = StringBuilder()
    frequencies(dna, 1, out)
    frequencies(dna, 2, out)
    for (fragment in listOf("GGT", "GGTA", "GGTATT", "GGTATTTTAATT", "GGTATTTTAATTTATAGT")) count(dna, fragment, out)
    print(out)
}

// A growable array of bytes.
class ByteArrayBuilder {
    private var data = ByteArray(1 shl 20)
    private var size = 0

    fun add(b: Byte) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = b
    }

    fun toByteArray(): ByteArray = data.copyOf(size)
}
