// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/
//
// binary-trees for Scala Native: a port of the C program (binarytrees.gcc), written for this
// suite. Every node is a separate allocation; the bottom node has two null children.

final class Node(val left: Node, val right: Node)

object binarytrees:
  def make(depth: Int): Node =
    if depth == 0 then Node(null, null) else Node(make(depth - 1), make(depth - 1))

  def check(node: Node): Int =
    if node.left == null then 1 else 1 + check(node.left) + check(node.right)

  def main(args: Array[String]): Unit =
    val maxDepth = math.max(6, args(0).toInt)
    val stretch = maxDepth + 1
    println(s"stretch tree of depth $stretch\t check: ${check(make(stretch))}")

    val longLived = make(maxDepth)
    for depth <- 4 to maxDepth by 2 do
      val iterations = 1 << (maxDepth - depth + 4)
      var sum = 0
      for _ <- 0 until iterations do sum += check(make(depth))
      println(s"$iterations\t trees of depth $depth\t check: $sum")
    println(s"long lived tree of depth $maxDepth\t check: ${check(longLived)}")
