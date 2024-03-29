import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int

enum class Complexity(
    val valueRange: IntRange,
    val expressionDepth: Int,
    val allowedOperators: Set<Operator>,
    val symbolsAllowed: Boolean = false
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
    ),

    Symbolic(
        valueRange = 1..100,
        expressionDepth = 4,
        allowedOperators = setOf(Operator.Plus, Operator.Minus, Operator.Times),
        symbolsAllowed = true
    )

}


data class GeneratorContext(val complexity: Complexity)

data object Generator {

    context(GeneratorContext)
    fun expCoAlgebra(rs: RandomSource = RandomSource.default()) = expCoAlgebra { operator().sample(rs).value }

    context(GeneratorContext)
    fun <T> expression(
        targetValue: Int = complexity.valueRange.random(),
        depth: Int = complexity.expressionDepth,
        symbolsAllowed: Boolean = false
    ): Arb<Fix<ForPure>> = arbitrary {
        val coAlgebra = expCoAlgebra(it)
        val initial = DeferredExpr(targetValue, depth)

        ExprF.functor<ForPure>().run { ana(coAlgebra)(initial) }

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

