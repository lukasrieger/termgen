@file:Suppress("UNCHECKED_CAST")

import com.lukas.Operator

sealed interface Exp<A> : Functor<Exp<A>, A> {
    data class BinOp<A>(val left: A, val operator: Operator, val right: A) : Exp<A>

    data class Const<A>(val value: Int) : Exp<A>

    override fun <R> map(f: (A) -> R): Functor<*, R> = when (this) {
        is BinOp -> BinOp(f(left), operator, f(right))
        is Const -> Const(value)
    }
}

typealias ExpF<T> = Fix<Exp<T>, T>

fun <T, R> ExpF<T>.into(): ExpF<R> = this as ExpF<R>

data class DeferredExp(val value: Int, val depth: Int)

fun <T> binOp(left: Fix<*, T>, op: Operator, right: Fix<*, T>): ExpF<T> = Fix(Exp.BinOp(left, op, right) as Exp<T>)

fun <T> const(value: Int): ExpF<T> = Fix(Exp.Const(value))