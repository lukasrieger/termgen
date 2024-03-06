import com.lukas.Operator
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import kotlin.math.sqrt
import kotlin.random.Random

enum class Complexity(
    val valueRange: IntRange,
    val expressionDepth: Int,
    val allowedOperators: Set<Operator>
) {

    Easy(
        valueRange = 1..10,
        expressionDepth = 2,
        allowedOperators = setOf(Operator.Plus, Operator.Minus)
    ),

    Medium(
        valueRange = 1..400,
        expressionDepth = 4,
        allowedOperators = setOf(Operator.Plus, Operator.Minus, Operator.Times)
    ),

    Hard(
        valueRange = -100..100,
        expressionDepth = 5,
        allowedOperators = setOf(Operator.Plus, Operator.Minus, Operator.Times, Operator.Div)
    )

}


data class GeneratorContext(val complexity: Complexity)

fun decompose(result: Int, operator: Operator): Pair<Int, Int> = when (operator) {
    Operator.Plus -> {
        val rand = if (result > 0) (0..<result).random() else (result..0).random()
        rand to result - rand
    }

    Operator.Minus -> {
        val (a, b) = listOf(result, (result + (result * 2))).sorted()
        val rand = (a..b).random()
        rand to (rand - result)
    }

    Operator.Times -> when (result) {
        0 -> 0 to 0
        else -> when {
            result > 0 -> (1..sqrt(result.toDouble()).toInt())
                .filter { result % it == 0 }
                .map { it to (result / it) }
                .random()

            else -> {
                val negated = -result
                (1..sqrt(negated.toDouble()).toInt())
                    .filter { negated % it == 0 }
                    .map { it to (result / it) }
                    .random()
            }
        }
    }

    Operator.Div -> {
        val b = Random.nextInt(from = 1, until = 4)
        val a = result * b
        a to b
    }
}

data object Generator {

    context(GeneratorContext)
    fun expCoAlgebra(rs: RandomSource = RandomSource.default()) = expCoAlgebra { operator().sample(rs).value }

    context(GeneratorContext)
    fun <T> expression(
        targetValue: Int = complexity.valueRange.random(),
        depth: Int = complexity.expressionDepth
    ): Arb<ExpF<T>> = arbitrary {
        val coAlgebra = expCoAlgebra(it)
        val initial = DeferredExp(targetValue, depth)

        ana(coAlgebra)(initial).into()
    }

    context(GeneratorContext)
    private fun operator() = Arb.choose(
        6 to Operator.Plus,
        6 to Operator.Minus,
        1 to Operator.Times,
        1 to Operator.Div
    ).filter { it in complexity.allowedOperators }

    context(GeneratorContext)
    fun constValue() = Arb.int(complexity.valueRange).filter { it != 0 }
}

