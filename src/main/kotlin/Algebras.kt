import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random


private val <A> Expr<A>.isDistributable: Boolean
    get() = when (this) {
        is ExprF.Mul, is ExprF.Add -> true
        else -> false
    }


private fun applyAddDistributive(add: ExprF.Const, exp: Fix<ForExpr>): Fix<ForExpr>? =
    when (val expr = exp.unfix.fix()) {
        is ExprF.Add -> {
            val left = applyAddDistributive(add, expr.left.fix()) ?: expr.left.fix()
            val right = applyAddDistributive(add, expr.right.fix()) ?: expr.right.fix()

            val leftF = left.unfix.fix()
            val rightF = right.unfix.fix()
            when {
                leftF is ExprF.Const && rightF is ExprF.Const ->
                    const(leftF.value + rightF.value)

                else -> add(left, right)
            }
        }

        else -> null
    }

private fun applySubDistributive(sub: ExprF.Const, exp: Fix<ForExpr>): Fix<ForExpr>? =
    when(val expr = exp.unfix.fix()) {
        is ExprF.Add -> {
            val left = applySubDistributive(sub, expr.left.fix()) ?: expr.left.fix()
            val right = applySubDistributive(sub, expr.right.fix()) ?: expr.right.fix()

            val leftF = left.unfix.fix()
            val rightF = right.unfix.fix()

            when {
                leftF is ExprF.Const && rightF !is ExprF.Const ->
                    add(const(leftF.value - sub.value), right)
                leftF !is ExprF.Const && rightF is ExprF.Const ->
                    add(left, const(rightF.value - sub.value))

                else -> add(left, right)
            }
        }
        else -> null
    }

private fun applyDivDistributive(div: ExprF.Const, exp: Fix<ForExpr>): Fix<ForExpr>? =
    when (val expr = exp.unfix.fix()) {
        is ExprF.Mul -> {
            val left = applyDivDistributive(div, expr.left.fix()) ?: expr.left.fix()
            val right = applyDivDistributive(div, expr.right.fix()) ?: expr.right.fix()

            val leftF = left.unfix.fix()
            val rightF = right.unfix.fix()
            when {
                leftF is ExprF.Const && rightF !is ExprF.Const ->
                    mul(const(leftF.value / div.value), right)
                leftF !is ExprF.Const && rightF is ExprF.Const ->
                    mul(left, const(rightF.value / div.value))

                else -> mul(left, right)
            }
        }
        else -> null
    }


private fun applyMulDistributive(factor: ExprF.Const, exp: Fix<ForExpr>): Fix<ForExpr>? =
    when (val expr = exp.unfix.fix()) {
        is ExprF.Var -> mul(const(factor.value), variable())
        is ExprF.Add -> {
            val left = applyMulDistributive(factor, expr.left.fix()) ?: expr.left.fix()
            val right = applyMulDistributive(factor, expr.right.fix()) ?: expr.right.fix()

            val leftF = left.unfix.fix()
            val rightF = right.unfix.fix()
            when {
                leftF is ExprF.Const && rightF is ExprF.Const ->
                    const(leftF.value + rightF.value)

                else -> add(left, right)
            }
        }
        is ExprF.Sub -> {
            val left = applyMulDistributive(factor, expr.left.fix()) ?: expr.left.fix()
            val right = applyMulDistributive(factor, expr.right.fix()) ?: expr.right.fix()

            val leftF = left.unfix.fix()
            val rightF = right.unfix.fix()

            when {
                leftF is ExprF.Const && rightF is ExprF.Const ->
                    const(leftF.value - rightF.value)

                else -> add(left, right)
            }
        }
        is ExprF.Mul -> {
            val left = applyMulDistributive(factor, expr.left.fix()) ?: expr.left.fix()
            val right = applyMulDistributive(factor, expr.right.fix()) ?: expr.right.fix()

            val leftF = left.unfix.fix()
            val rightF = right.unfix.fix()
            when {
                leftF is ExprF.Const && rightF is ExprF.Const ->
                    const(leftF.value * rightF.value)

                else -> mul(left, right)
            }
        }

        is ExprF.Const -> const(factor.value * expr.value)

        else -> null // Can't apply Mul distributively over anything other than Add and Mul
    }


val evalAlgebra: Algebra<ForPure, Int> = {
    it.fix().run {
        when (this) {
            is ExprF.Const -> value
            is ExprF.Add -> left + right
            is ExprF.Div -> left / right
            is ExprF.Mul -> left * right
            is ExprF.Pow -> base.toDouble().pow(exponent).toInt()
            is ExprF.Sub -> left - right
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
        }
    }
}

typealias Path = List<Pair<Operator, Fix<ForExpr>>>

val pathToUnknownAlgebra: RAlgebra<ForExpr, Fix<ForExpr>, Path> = {
    it.fix().run {
        when(this) {
            is ExprF.Add -> {
                val self = add(left.first, right.first)
                when {
                    left.second.isNotEmpty() -> left.second + Pair(Operator.Minus, right.first)
                    right.second.isNotEmpty() -> right.second + Pair(Operator.Minus, left.first)
                    else -> emptyList()
                }
            }
            is ExprF.Const -> emptyList()
            is ExprF.Div -> {
                val self = add(left.first, right.first)
                when {
                    left.second.isNotEmpty() -> left.second + Pair(Operator.Times, right.first)
                    right.second.isNotEmpty() -> right.second + Pair(Operator.Times, left.first)
                    else -> emptyList()
                }
            }
            is ExprF.Mul -> {
                val self = add(left.first, right.first)
                when {
                    left.second.isNotEmpty() -> left.second + Pair(Operator.Div, right.first)
                    right.second.isNotEmpty() -> right.second + Pair(Operator.Div, left.first)
                    else -> emptyList()
                }
            }
            is ExprF.Pow -> {
                val self = pow(base.first, exponent)
                when {
                    base.second.isNotEmpty() -> base.second + Pair(Operator.Log, const(exponent))
                    else -> emptyList()
                }
            }
            is ExprF.Sub -> {
                val self = add(left.first, right.first)
                when {
                    left.second.isNotEmpty() -> left.second + Pair(Operator.Plus, right.first)
                    right.second.isNotEmpty() -> right.second + Pair(Operator.Plus, left.first)
                    else -> emptyList()
                }
            }
            ExprF.Var -> listOf(Pair(Operator.Plus, variable()))
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
            ExprF.Var -> const(1)
        }
    }
}


val simplifyAlgebra: Algebra<ForExpr, Fix<ForExpr>> = {
    it.fix().run {
        when (this) {
            is ExprF.Add -> {
                val leftU = left.unfix.fix()
                val rightU = right.unfix.fix()

                when {
                    rightU is ExprF.Const && leftU is ExprF.Const ->
                        const(leftU.value + rightU.value)
                    leftU is ExprF.Const && leftU.value == 0 ->
                        right
                    rightU is ExprF.Const && rightU.value == 0 ->
                        left
                    leftU is ExprF.Const && rightU is ExprF.Add ->
                        applyAddDistributive(leftU, right) ?: add(left, right)
                    rightU is ExprF.Const && leftU is ExprF.Add ->
                        applyAddDistributive(rightU, left) ?: add(left, right)
                    else ->
                        add(left, right)
                }
            }
            is ExprF.Const -> const(value)
            is ExprF.Div -> {
                val leftU = left.unfix.fix()
                val rightU = right.unfix.fix()

                when {
                    leftU is ExprF.Const && rightU is ExprF.Const ->
                        const(leftU.value / rightU.value)
                    leftU is ExprF.Const && rightU is ExprF.Mul ->
                        applyDivDistributive(leftU, right) ?: div(left, right)
                    rightU is ExprF.Const && leftU is ExprF.Mul ->
                        applyDivDistributive(rightU, left) ?: div(left, right)
                    else -> div(left, right)
                }
            }
            is ExprF.Mul -> {
                val leftU = left.unfix.fix()
                val rightU = right.unfix.fix()

                when {
                    leftU is ExprF.Const && rightU is ExprF.Const ->
                        const(leftU.value * rightU.value)
                    leftU is ExprF.Const && leftU.value == 1 ->
                        right
                    rightU is ExprF.Const && rightU.value == 1 ->
                        left
                    leftU is ExprF.Const && leftU.value == 0 ->
                        const(0)
                    rightU is ExprF.Const && rightU.value == 0 ->
                        const(0)
                    leftU is ExprF.Const && rightU.isDistributable ->
                        applyMulDistributive(leftU, right) ?: mul(left, right)
                    rightU is ExprF.Const && leftU.isDistributable ->
                        applyMulDistributive(rightU, left) ?: mul(left, right)
                    else ->
                        mul(left, right)
                }
            }

            is ExprF.Pow -> when (exponent) {
                1 -> base
                else -> pow(base, exponent)
            }

            is ExprF.Sub -> {
                val leftU = left.unfix.fix()
                val rightU = right.unfix.fix()

                when {
                    leftU is ExprF.Const && rightU is ExprF.Const ->
                        const(leftU.value - rightU.value)
                    leftU is ExprF.Const && rightU is ExprF.Add ->
                        applySubDistributive(leftU, right) ?: sub(left, right)
                    rightU is ExprF.Const && leftU is ExprF.Add ->
                        applySubDistributive(rightU, left) ?: sub(left, right)
                    else -> sub(left, right)
                }
            }
            ExprF.Var -> variable()
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
                Operator.Log -> error("Unsupported")
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

    Operator.Log -> error("Unsupported")
}