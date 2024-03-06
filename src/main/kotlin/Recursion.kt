@file:Suppress("UNCHECKED_CAST")

import arrow.core.andThen


interface Functor<F, T> {
    fun <R> map(f: (T) -> R): Functor<*, R>
}

@JvmInline
value class Fix<F, T>(private val unfix: F) where F : Functor<F, T> {

    fun unfix(): Functor<*, Fix<F, T>> = unfix as Functor<*, Fix<F, T>>

    override fun toString(): String = unfix.toString()
}

fun interface Algebra<F, T> : (F) -> T where F : Functor<F, T>

fun interface CoAlgebra<F, T> : (T) -> F where F : Functor<F, T>


/* generalized Fold */
fun <F, T> cata(
    algebra: Algebra<F, T>
): (Fix<F, T>) -> T where F : Functor<F, T> = { fix ->
    algebra(fix.unfix().map(cata(algebra)) as F)
}

/* generalized unfold */
fun <F, T> ana(
    coAlgebra: CoAlgebra<F, T>
): (T) -> Fix<F, T> where F : Functor<F, T> = {
    Fix(coAlgebra(it).map(ana(coAlgebra)) as F)
}

/** efficient composition of [ana] and [cata] */
fun <F1, A, F2, B> hylo(
    algebra: Algebra<F2, B>,
    coAlgebra: CoAlgebra<F1, A>
): (A) -> B where F1 : Functor<F1, A>, F2 : Functor<F2, B> = {
    algebra(coAlgebra(it).map(hylo(algebra, coAlgebra)) as F2)
}

/** simple composition of [ana] and [cata] */
fun <F1, A, F2, B> simpleHylo(
    algebra: Algebra<F2, B>,
    coAlgebra: CoAlgebra<F1, A>
): (A) -> B where F1 : Functor<F1, A>, F2 : Functor<F2, B> =
    ana(coAlgebra) as (A) -> Fix<F2, B> andThen cata(algebra)
