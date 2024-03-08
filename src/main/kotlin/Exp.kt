@file:Suppress("UNCHECKED_CAST")


typealias ExprOf<A> = Kind<ForExpr, A>

data object ForPure : ForExpr()

typealias PureOf<A> = Kind<ForPure, A>


open class ForExpr

fun <A> ExprOf<A>.fix(): Expr<A> = this as Expr<A>

fun <A> PureOf<A>.fix(): ExprF.PureF<A> = this as ExprF.PureF<A>

typealias Expr<A> = ExprF<A, ForExpr>

typealias Pure<A> = ExprF<A, ForPure>

sealed interface ExprF<out A, out K : ForExpr> : Kind<K, A> {

    sealed interface PureF<out A> : ExprF<A, ForPure>

    data class Add<A>(val left: A, val right: A) : PureF<A>

    data class Sub<A>(val left: A, val right: A) : PureF<A>

    data class Mul<A>(val left: A, val right: A) : PureF<A>

    data class Div<A>(val left: A, val right: A) : PureF<A>

    data class Pow<A>(val base: A, val exponent: Int) : PureF<A>

    data class Sqrt<A>(val base: A, val nth: A) : PureF<A>

    data class Const(val value: Int) : PureF<Nothing>

    data object Var : ExprF<Nothing, ForExpr>

    companion object {
        fun <K : ForExpr> functor() = object : Functor<K> {
            override fun <T, R> Kind<K, T>.map(f: (T) -> R): Kind<K, R> =
                fix().run {
                    when (this) {
                        is Add -> Add(f(left), f(right))
                        is Const -> Const(value)
                        is Div -> Div(f(left), f(right))
                        is Mul -> Mul(f(left), f(right))
                        is Pow -> Pow(f(base), exponent)
                        is Sub -> Sub(f(left), f(right))
                        is Sqrt -> Sqrt(f(base), f(nth))
                        Var -> Var
                    }
                } as Kind<K, R>
        }
    }
}

data class DeferredExpr(val value: Int, val depth: Int)


fun variable() = Fix(ExprF.Var)

fun add(left: Fix<ForExpr>, right: Fix<ForExpr>) = Fix(ExprF.Add(left, right))

fun sub(left: Fix<ForExpr>, right: Fix<ForExpr>) = Fix(ExprF.Sub(left, right))

fun mul(left: Fix<ForExpr>, right: Fix<ForExpr>) = Fix(ExprF.Mul(left, right))

fun div(left: Fix<ForExpr>, right: Fix<ForExpr>) = Fix(ExprF.Div(left, right))

fun pow(base: Fix<ForExpr>, exponent: Int) = Fix(ExprF.Pow(base, exponent))

fun sqrt(base: Fix<ForExpr>, nth: Fix<ForExpr>) = Fix(ExprF.Sqrt(base, nth))

fun const(value: Int) = Fix(ExprF.Const(value))
