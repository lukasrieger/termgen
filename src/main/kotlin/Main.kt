import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.pair
import io.kotest.property.forAll


fun <T> eval(exp: ExpF<T>): Int =
    cata(evalAlgebra)(exp.into())

fun <T> prettyPrint(exp: ExpF<T>): String =
    cata(showAlgebra)(exp.into())

suspend fun main() {
    with(GeneratorContext(Complexity.Hard)) {
        val evalResult = forAll(iterations = 100, Generator.constValue()) { targetValue ->
            println("Checking target value: $targetValue")

            forAll(iterations = 10, Generator.expression<Int>(targetValue = targetValue)) {
                println("Generated expression: ${prettyPrint(it)}")
                targetValue == eval(it)
            }
            true
        }

        val hyloResult = forAll(100, pairGen) { (targetValue, depth) ->
            val initial = DeferredExp(targetValue, depth)

            println(expPrettyHylo(initial))
            expEvalHylo(initial) == targetValue
        }

        println("Simple eval total successes ${evalResult.successes()}")
        println("Simple eval failures ${evalResult.failures()}")

        println("Hylo successes ${hyloResult.successes()}")
        println("Hylo failures ${hyloResult.failures()}")

    }
}

context(GeneratorContext)
val pairGen
    get() = Arb.pair(
        Arb.int(complexity.valueRange),
        Arb.constant(complexity.expressionDepth)
    )

context(GeneratorContext)
val expEvalHylo
    get() =
        hylo(
            algebra = evalAlgebra,
            coAlgebra = Generator.expCoAlgebra()
        )


context(GeneratorContext)
val expPrettyHylo
    get() =
        simpleHylo(
            algebra = showAlgebra,
            coAlgebra = Generator.expCoAlgebra()
        )