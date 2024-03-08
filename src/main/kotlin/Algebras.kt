import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random


val evalAlgebra: Algebra<ForPure, Int> = {
    it.fix().run {
        when (this) {
            is ExprF.Const -> value
            is ExprF.Add -> left + right
            is ExprF.Div -> left / right
            is ExprF.Mul -> left * right
            is ExprF.Pow -> base.toDouble().pow(exponent).toInt()
            is ExprF.Sub -> left - right
            is ExprF.Sqrt -> base.toDouble().pow(1 / nth).toInt()
        }
    }
}

val countUnknownsAlgebra: Algebra<ForExpr, Int> = {
    it.fix().run {
        when (this) {
            is ExprF.Const -> 0
            is ExprF.Add -> left + right
            is ExprF.Div -> left + right
            is ExprF.Mul -> left + right
            is ExprF.Pow -> base
            is ExprF.Sub -> left + right
            is ExprF.Var -> 1
            is ExprF.Sqrt -> base + nth
        }
    }
}

typealias Path = List<Pair<Operator, Fix<ForExpr>>>

val pathToUnknownAlgebra: RAlgebra<ForExpr, Fix<ForExpr>, Path> = {
    it.fix().run {
        when (this) {
            is ExprF.Add -> when {
                left.second.isNotEmpty() -> left.second + Pair(Operator.Minus, right.first)
                right.second.isNotEmpty() -> right.second + Pair(Operator.Minus, left.first)
                else -> emptyList()
            }

            is ExprF.Const -> emptyList()
            is ExprF.Div -> when {
                left.second.isNotEmpty() -> left.second + Pair(Operator.Times, right.first)
                right.second.isNotEmpty() -> right.second + Pair(Operator.Times, left.first)
                else -> emptyList()
            }

            is ExprF.Mul -> when {
                left.second.isNotEmpty() -> left.second + Pair(Operator.Div, right.first)
                right.second.isNotEmpty() -> right.second + Pair(Operator.Div, left.first)
                else -> emptyList()
            }

            is ExprF.Pow -> when {
                base.second.isNotEmpty() -> base.second + Pair(Operator.Sqrt, const(exponent))
                else -> emptyList()
            }

            is ExprF.Sub -> when {
                left.second.isNotEmpty() -> left.second + Pair(Operator.Plus, right.first)
                right.second.isNotEmpty() -> right.second + Pair(Operator.Plus, left.first)
                else -> emptyList()
            }

            ExprF.Var -> listOf(Pair(Operator.NoOp, variable()))
            is ExprF.Sqrt -> when {
                base.second.isNotEmpty() -> base.second + Pair(Operator.Times, base.first)
                else -> emptyList()
            }
        }
    }
}


fun solveWith(x: Int = 0): Algebra<ForExpr, Int> = {
    it.fix().run {
        when (this) {
            is ExprF.Const -> value
            is ExprF.Add -> left + right
            is ExprF.Div -> left / right
            is ExprF.Mul -> left * right
            is ExprF.Pow -> base.toDouble().pow(exponent).toInt()
            is ExprF.Sub -> left - right
            is ExprF.Sqrt -> base.toDouble().pow(1 / nth).toInt()
            is ExprF.Var -> x
        }
    }
}

val showAlgebra: Algebra<ForExpr, String> = {
    it.fix().run {
        when (this) {
            is ExprF.Const -> "$value"
            is ExprF.Add -> "(${left} + ${right})"
            is ExprF.Div -> "(${left} / ${right})"
            is ExprF.Mul -> "(${left} * ${right})"
            is ExprF.Pow -> "${base}^${exponent}"
            is ExprF.Sub -> "(${left} - ${right})"
            is ExprF.Sqrt -> "sqrt($base, $nth)"
            is ExprF.Var -> "x"
        }
    }

}

val derivationAlgebra: RAlgebra<ForExpr, Fix<ForExpr>, Fix<ForExpr>> = {
    it.fix().run {
        when (this) {
            is ExprF.Add -> add(left.second, right.second)
            is ExprF.Const -> const(0)
            is ExprF.Div -> TODO()
            is ExprF.Mul -> add(
                mul(left.second, right.first),
                mul(left.first, right.second)
            )

            is ExprF.Pow -> mul(
                const(exponent),
                mul(
                    pow(base.first, exponent - 1),
                    base.second
                )
            )

            is ExprF.Sub -> sub(left.second, right.second)
            is ExprF.Sqrt -> TODO()
            ExprF.Var -> const(1)
        }
    }
}


fun expCoAlgebra(
    selectOperator: () -> Operator
): CoAlgebra<ForPure, DeferredExpr> = { (value, depth) ->
    when {
        depth < 1 -> ExprF.Const(value)
        else -> {
            val op = selectOperator()
            val (leftValue, rightValue) = decompose(result = value, operator = op)
            val (leftDef, rightDef) = DeferredExpr(leftValue, depth - 1) to DeferredExpr(rightValue, depth - 1)

            when (op) {
                Operator.Plus -> ExprF.Add(leftDef, rightDef)
                Operator.Minus -> ExprF.Sub(leftDef, rightDef)
                Operator.Times -> ExprF.Mul(leftDef, rightDef)
                Operator.Div -> ExprF.Div(leftDef, rightDef)
                else -> error("Unsupported")
            }
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

    else -> error("Unsupported")
}