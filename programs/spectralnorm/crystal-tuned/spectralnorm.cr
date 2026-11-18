# The Computer Language Benchmarks Game
# https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
#
# spectral-norm for Crystal: a port of the C program (spectralnorm.gcc-8), written for this suite.

def a(i, j)
  1.0 / ((i + j) * (i + j + 1) // 2 + i + 1)
end

def times_a(v : Array(Float64), dst : Array(Float64))
  v.size.times do |i|
    sum = 0.0
    v.size.times { |j| sum += a(i, j) * v[j] }
    dst[i] = sum
  end
end

def times_at(v : Array(Float64), dst : Array(Float64))
  v.size.times do |i|
    sum = 0.0
    v.size.times { |j| sum += a(j, i) * v[j] }
    dst[i] = sum
  end
end

def times_ata(v, dst, tmp)
  times_a(v, tmp)
  times_at(tmp, dst)
end

n = ARGV[0].to_i
u = Array.new(n, 1.0)
v = Array.new(n, 0.0)
tmp = Array.new(n, 0.0)
10.times do
  times_ata(u, v, tmp)
  times_ata(v, u, tmp)
end
vbv = 0.0
vv = 0.0
n.times do |i|
  vbv += u[i] * v[i]
  vv += v[i] * v[i]
end
printf("%0.9f\n", Math.sqrt(vbv / vv))
