import kotlin.math.pow

enum class Operator(val symbol: String, val f: (Int, Int) -> Int) {
    Plus("+", { a, b -> a + b }),
    Minus("-", { a, b -> a - b }),
    Times("*", { a, b -> a * b }),
    Div("/", { a, b -> a / b }),
    Sqrt("log", { a, b ->
        println("Sqrting: $a, $b")
        a.toDouble().pow(1 / b.toDouble()).toInt().also { println(it) }
    }),
    NoOp("noop", { _, _ -> -1 });


    operator fun invoke(a: Int, b: Int): Int = f(a, b)
}