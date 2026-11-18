# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
#
# k-nucleotide for Crystal: a port of the C program (knucleotide.gcc), written for this suite.
# Every k-mer is counted into the standard Hash, keyed by its 2k-bit code.

def code(c : Char) : UInt64
  case c
  when 'a', 'A' then 0_u64
  when 'c', 'C' then 1_u64
  when 'g', 'G' then 2_u64
  else               3_u64
  end
end

def decode(key : UInt64, k : Int32) : String
  String.build do |s|
    (k - 1).downto(0) { |i| s << "ACGT"[((key >> (2 * i)) & 3).to_i] }
  end
end

def count_all(dna : Bytes, k : Int32) : Hash(UInt64, Int32)
  counts = Hash(UInt64, Int32).new(0)
  mask = (1_u64 << (2 * k)) - 1
  key = 0_u64
  dna.each_with_index do |b, i|
    key = ((key << 2) & mask) | b
    counts[key] += 1 if i >= k - 1
  end
  counts
end

def frequencies(dna : Bytes, k : Int32)
  total = (dna.size - k + 1).to_f
  sorted = count_all(dna, k).to_a.sort_by { |key, n| {-n, key} }
  sorted.each { |key, n| printf("%s %.3f\n", decode(key, k), 100.0 * n / total) }
  puts
end

def count(dna : Bytes, fragment : String)
  key = fragment.each_char.reduce(0_u64) { |acc, c| (acc << 2) | code(c) }
  puts "#{count_all(dna, fragment.size).fetch(key, 0)}\t#{fragment}"
end

while (line = STDIN.gets) && !line.starts_with?(">THREE")
end
buffer = IO::Memory.new
while (line = STDIN.gets) && !line.starts_with?('>')
  line.each_char { |c| buffer.write_byte(code(c).to_u8) }
end
dna = buffer.to_slice

frequencies(dna, 1)
frequencies(dna, 2)
["GGT", "GGTA", "GGTATT", "GGTATTTTAATT", "GGTATTTTAATTTATAGT"].each { |f| count(dna, f) }
