enum class Operator(val symbol: String, val f: (Int, Int) -> Int) {
    Plus("+", { a, b -> a + b}) ,
    Minus("-", { a, b -> a - b}),
    Times("*", { a, b -> a * b}),
    Div("/", { a, b -> a / b}),
    Log("log", { a, b -> kotlin.math.log(a.toDouble(), b.toDouble()).toInt() });


    operator fun invoke(a: Int, b: Int): Int = f(a, b)
}