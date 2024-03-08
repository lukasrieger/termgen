object ForFix

typealias FixOf<A> = Kind<ForFix, A>

data class Fix<out A>(val unfix: Kind<A, FixOf<A>>) : FixOf<A> {
    override fun toString(): String = unfix.toString()
}

fun <A> FixOf<A>.fix(): Fix<A> = this as Fix<A>
