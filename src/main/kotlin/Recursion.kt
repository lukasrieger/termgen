import arrow.core.andThen


interface Kind<out F, out A>


typealias Algebra<F, A> = (Kind<F, A>) -> A

typealias CoAlgebra<F, A> = (A) -> Kind<F, A>

typealias RAlgebra<F, T, A> = (Kind<F, Pair<T, A>>) -> A


context(Functor<F>)
fun <F, A> ana(
    coAlgebra: CoAlgebra<F, A>
): (A) -> Fix<F> = { Fix(coAlgebra(it).map(ana(coAlgebra))) }

context(Functor<F>)
fun <F, A> cata(
    algebra: Algebra<F, A>
): (Fix<F>) -> A = { fix ->
    algebra(fix.unfix.map { cata(algebra)(it.fix()) })
}

context(Functor<F>)
fun <F, A, B> composedHylo(
    algebra: Algebra<F, B>,
    coAlgebra: CoAlgebra<F, A>
): (A) -> B = ana(coAlgebra) andThen cata(algebra)

context(Functor<F>)
fun <F, A, B> hylo(
    algebra: Algebra<F, B>,
    coAlgebra: CoAlgebra<F, A>
): (A) -> B = { algebra(coAlgebra(it).map(hylo(algebra, coAlgebra))) }

context(Functor<F>)
fun <F, A> para(
    algebra: RAlgebra<F, Fix<F>, A>
): (Fix<F>) -> A = { fix ->
    algebra(fix.unfix.map { it.fix() to para(algebra)(it.fix()) })
}