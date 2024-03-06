import com.lukas.Operator


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