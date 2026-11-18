// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
//
// n-body for Scala Native: a port of the C program (nbody.gcc-6), written for this suite. Each
// body is an object with mutable fields, held in an array.

final class Body(var x: Double, var y: Double, var z: Double,
                 var vx: Double, var vy: Double, var vz: Double, val mass: Double)

object nbody:
  val SolarMass = 4.0 * math.Pi * math.Pi
  val DaysPerYear = 365.24

  def planet(x: Double, y: Double, z: Double, vx: Double, vy: Double, vz: Double, mass: Double) =
    Body(x, y, z, vx * DaysPerYear, vy * DaysPerYear, vz * DaysPerYear, mass * SolarMass)

  def offsetMomentum(bodies: Array[Body]): Unit =
    var px = 0.0
    var py = 0.0
    var pz = 0.0
    for b <- bodies do
      px += b.vx * b.mass
      py += b.vy * b.mass
      pz += b.vz * b.mass
    bodies(0).vx = -px / SolarMass
    bodies(0).vy = -py / SolarMass
    bodies(0).vz = -pz / SolarMass

  def energy(bodies: Array[Body]): Double =
    var e = 0.0
    for i <- bodies.indices do
      val a = bodies(i)
      e += 0.5 * a.mass * (a.vx * a.vx + a.vy * a.vy + a.vz * a.vz)
      for j <- i + 1 until bodies.length do
        val b = bodies(j)
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        e -= a.mass * b.mass / math.sqrt(dx * dx + dy * dy + dz * dz)
    e

  def advance(bodies: Array[Body], dt: Double): Unit =
    for i <- bodies.indices do
      val a = bodies(i)
      for j <- i + 1 until bodies.length do
        val b = bodies(j)
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        val d2 = dx * dx + dy * dy + dz * dz
        val mag = dt / (d2 * math.sqrt(d2))
        a.vx -= dx * b.mass * mag
        a.vy -= dy * b.mass * mag
        a.vz -= dz * b.mass * mag
        b.vx += dx * a.mass * mag
        b.vy += dy * a.mass * mag
        b.vz += dz * a.mass * mag
    for b <- bodies do
      b.x += dt * b.vx
      b.y += dt * b.vy
      b.z += dt * b.vz

  def main(args: Array[String]): Unit =
    val n = args(0).toInt
    val bodies = Array(
      Body(0, 0, 0, 0, 0, 0, SolarMass),
      planet(4.84143144246472090e+00, -1.16032004402742839e+00, -1.03622044471123109e-01,
        1.66007664274403694e-03, 7.69901118419740425e-03, -6.90460016972063023e-05,
        9.54791938424326609e-04),
      planet(8.34336671824457987e+00, 4.12479856412430479e+00, -4.03523417114321381e-01,
        -2.76742510726862411e-03, 4.99852801234917238e-03, 2.30417297573763929e-05,
        2.85885980666130812e-04),
      planet(1.28943695621391310e+01, -1.51111514016986312e+01, -2.23307578892655734e-01,
        2.96460137564761618e-03, 2.37847173959480950e-03, -2.96589568540237556e-05,
        4.36624404335156298e-05),
      planet(1.53796971148509165e+01, -2.59193146099879641e+01, 1.79258772950371181e-01,
        2.68067772490389322e-03, 1.62824170038242295e-03, -9.51592254519715870e-05,
        5.15138902046611451e-05))
    offsetMomentum(bodies)
    println(f"${energy(bodies)}%.9f")
    for _ <- 0 until n do advance(bodies, 0.01)
    println(f"${energy(bodies)}%.9f")
