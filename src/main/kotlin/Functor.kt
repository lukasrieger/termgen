interface Functor<F> {
    fun <T, R> Kind<F, T>.map(f: (T) -> R): Kind<F, R>
}