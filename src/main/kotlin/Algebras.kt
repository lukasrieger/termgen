import com.lukas.Operator
import kotlin.math.sqrt
import kotlin.random.Random


val evalAlgebra = Algebra<Exp<Int>, Int> {
    when (it) {
        is Exp.BinOp -> when (it.operator) {
            Operator.Plus -> it.left + it.right
            Operator.Minus -> it.left - it.right
            Operator.Times -> it.left * it.right
            Operator.Div -> it.left / it.right
        }

        is Exp.Const -> it.value
    }
}

val showAlgebra = Algebra<Exp<String>, String> {
    when (it) {
        is Exp.BinOp -> when (it.operator) {
            Operator.Plus -> "(${it.left} + ${it.right})"
            Operator.Minus -> "(${it.left} - ${it.right})"
            Operator.Times -> "(${it.left} * ${it.right})"
            Operator.Div -> "(${it.left} / ${it.right})"
        }

        is Exp.Const -> "${it.value}"
    }
}

fun expCoAlgebra(
    selectOperator: () -> Operator
) = CoAlgebra<Exp<DeferredExp>, DeferredExp> { (value, depth) ->
    when {
        depth < 1 -> Exp.Const(value)
        else -> {
            val op = selectOperator()
            val (left, right) = decompose(result = value, operator = op)

            Exp.BinOp(
                left = DeferredExp(left, depth - 1),
                operator = op,
                right = DeferredExp(right, depth - 1)
            )
        }
    }
}


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