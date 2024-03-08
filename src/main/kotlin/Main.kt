import arrow.core.andThen
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.pair
import io.kotest.property.forAll


fun eval(exp: Fix<ForPure>): Int =
    ExprF.functor<ForPure>().run { cata(evalAlgebra)(exp) }


fun prettyPrint(exp: Fix<ForExpr>): String =
    ExprF.functor<ForExpr>().run { cata(showAlgebra)(exp) }

fun derive(exp: Fix<ForExpr>): Fix<ForExpr> =
    ExprF.functor<ForExpr>().run { para(derivationAlgebra)(exp) }

fun simplify(exp: Fix<ForExpr>): Fix<ForExpr> =
    ExprF.functor<ForExpr>().run { cata(simplifyAlgebra)(exp) }

fun deriveAndSimplify(exp: Fix<ForExpr>): Fix<ForExpr> =
    ExprF.functor<ForExpr>().run {
        (para(derivationAlgebra) andThen cata(simplifyAlgebra))(exp)
    }

fun solveForMaximum(exp: Fix<ForExpr>): Int =
    ExprF.functor<ForExpr>().run {
        val zero =
            para(derivationAlgebra) andThen
                    cata(simplifyAlgebra) andThen
                    cata(solveWith(x = 0))

        cata(solveWith(x = zero(exp)))(exp)
    }

fun pathToUnknown(exp: Fix<ForExpr>): List<Pair<Operator, Fix<ForExpr>>> =
    ExprF.functor<ForExpr>().run {
        val amount = cata(countUnknownsAlgebra)(exp)

        when(amount) {
            1 -> (cata(simplifyAlgebra) andThen para(pathToUnknownAlgebra))(exp).reversed()
            else -> error("Function contains more than one unknown. This is currently unsupported.")
        }
    }

fun solve(exp: Fix<ForExpr>): Pair<Fix<ForExpr>, Fix<ForExpr>> =
    ExprF.functor<ForExpr>().run {
        val path = pathToUnknown(exp)
        val right: Fix<ForExpr> = const(0)

        val (leftSide, rightSide) = path.dropLast(1).fold(Pair(simplify(exp), right)) { (left, right), (op, by) ->
            when(op) {
                Operator.Plus -> simplify(add(left, by)) to simplify(add(right, by))
                Operator.Minus -> simplify(sub(left, by)) to simplify(sub(right, by))
                Operator.Times -> simplify(mul(left, by)) to simplify(mul(right, by))
                Operator.Div -> simplify(div(left, by)) to simplify(div(right, by))
                Operator.Log -> TODO()
            }
        }

        simplify(leftSide) to simplify(rightSide)
    }

fun nthDerivative(n: Int, exp: Fix<ForExpr>): Fix<ForExpr> = when (n) {
    0 -> simplify(exp)
    else -> nthDerivative(n - 1, derive(exp))
}


suspend fun main() {
    with(GeneratorContext(Complexity.Medium)) {
        val evalResult = forAll(iterations = 100, Generator.constValue()) { targetValue ->
            println("Checking target value: $targetValue")

            forAll(iterations = 10, Generator.expression<Int>(targetValue = targetValue)) {
                println("Generated expression: ${prettyPrint(it)}")
                targetValue == eval(it)
            }
            true
        }

        val hyloResult = forAll(100, pairGen) { (targetValue, depth) ->
            val initial = DeferredExpr(targetValue, depth)

            println(expPrettyHylo(initial))
            expEvalHylo(initial) == targetValue
        }

        println("Simple eval total successes ${evalResult.successes()}")
        println("Simple eval failures ${evalResult.failures()}")

        println("Hylo successes ${hyloResult.successes()}")
        println("Hylo failures ${hyloResult.failures()}")


        val derivable = mul(const(3), pow(add(variable(), mul(const(3), const(5))), 6))

        val derivable3 = mul(
            const(3),
            add(variable(), mul(const(3), const(5)))
        )

        println(prettyPrint(derivable))

        println(prettyPrint(derivable3))
        println(prettyPrint(simplify(derivable3)))
        val derivable2 = add(const(2), const(3))

        println(prettyPrint(deriveAndSimplify(derivable)))
        println(prettyPrint(deriveAndSimplify(derivable2)))

        println(prettyPrint(nthDerivative(2, derivable)))


        println("--------------")

        val generated = Generator.expression<Int>(targetValue = 100).sample(RandomSource.default()).value
        println(prettyPrint(generated))
        println(prettyPrint(simplify(generated)))

        println("----------------")

        val derivable4 = mul(
            const(3),
            add(variable(), mul(const(3), const(5)))
        )

        println(pathToUnknown(derivable4))

        println("Given: ${prettyPrint(simplify(derivable4))} , solving for x gives us: ")
        val (leftSide, rightSide) = solve(derivable4)
        println("${prettyPrint(leftSide)} = ${prettyPrint(rightSide)}")

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
    get() = ExprF.functor<ForPure>().run {
        hylo(
            algebra = evalAlgebra,
            coAlgebra = Generator.expCoAlgebra()
        )
    }


context(GeneratorContext)
val expPrettyHylo
    get() = ExprF.functor<ForPure>().run {
        composedHylo(
            algebra = evalAlgebra,
            coAlgebra = Generator.expCoAlgebra()
        )
    }