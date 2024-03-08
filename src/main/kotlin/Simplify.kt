
val simplifyAlgebra: Algebra<ForExpr, Fix<ForExpr>> = { i->
    i.fix().run {
        when(this) {
            is ExprF.Add -> {
                val leftU = left.unfix.fix()
                val rightU = right.unfix.fix()

                when {
                    leftU is ExprF.Const && rightU is ExprF.Const -> const(leftU.value + rightU.value)
                    leftU is ExprF.Const && leftU.value == 0 -> right
                    rightU is ExprF.Const && rightU.value == 0 -> left
                    leftU is ExprF.Const && rightU is ExprF.Add -> {
                        val l = rightU.left.fix().unfix.fix()
                        val r = rightU.right.fix().unfix.fix()

                        when {
                            l is ExprF.Const && r is ExprF.Const -> const(leftU.value + l.value + r.value)
                            l !is ExprF.Const && r is ExprF.Const -> add(Fix(l), const(r.value + leftU.value))
                            l is ExprF.Const && r !is ExprF.Const -> add(Fix(r), const(l.value + leftU.value))
                            else -> Fix(i)
                        }
                    }

                    rightU is ExprF.Const && leftU is ExprF.Add -> {
                        val l = leftU.left.fix().unfix.fix()
                        val r = leftU.right.fix().unfix.fix()

                        when {
                            l is ExprF.Const && r is ExprF.Const -> const(rightU.value + l.value + r.value)
                            l !is ExprF.Const && r is ExprF.Const -> add(Fix(l), const(r.value + rightU.value))
                            l is ExprF.Const && r !is ExprF.Const -> add(Fix(r), const(l.value + rightU.value))
                            else -> Fix(i)
                        }
                    }

                    leftU is ExprF.Const && rightU is ExprF.Sub -> {
                        val l = rightU.left.fix().unfix.fix()
                        val r = rightU.right.fix().unfix.fix()

                        when {
                            l is ExprF.Const && r is ExprF.Const -> const(leftU.value + (l.value - r.value))
                            else -> Fix(i)
                        }
                    }

                    rightU is ExprF.Const && leftU is ExprF.Sub -> {
                        val l = leftU.left.fix().unfix.fix()
                        val r = leftU.right.fix().unfix.fix()

                        when {
                            l is ExprF.Const && r is ExprF.Const -> const(rightU.value + (l.value - r.value))
                            l !is ExprF.Const && r is ExprF.Const -> sub(Fix(l), const(-r.value + rightU.value))
                            l is ExprF.Const && r !is ExprF.Const -> sub(const(l.value + rightU.value), Fix(r))
                            else -> Fix(i)
                        }
                    }
                    else -> Fix(i)
                }
            }
            is ExprF.Const -> Fix(i)
            is ExprF.Div -> {
                val leftU = left.unfix.fix()
                val rightU = right.unfix.fix()

                when {
                    leftU is ExprF.Const && rightU is ExprF.Const ->
                        const(leftU.value / rightU.value)
                    rightU is ExprF.Const && leftU is ExprF.Mul -> {
                        val l = leftU.left.fix().unfix.fix()
                        val r = leftU.right.fix().unfix.fix()

                        when {
                            l is ExprF.Const && r is ExprF.Const -> const((l.value * r.value) / rightU.value)
                            l is ExprF.Const && l.value == rightU.value -> Fix(r)
                            l !is ExprF.Const && r is ExprF.Const && r.value == rightU.value -> Fix(l)
                            else -> Fix(i)
                        }
                    }

                    rightU is ExprF.Const && rightU.value == 1 -> Fix(leftU)
                    else -> Fix(i)
                }
            }
            is ExprF.Mul -> {
                val leftU = left.unfix.fix()
                val rightU = right.unfix.fix()

                when {
                    leftU is ExprF.Const && rightU is ExprF.Const -> const(leftU.value * rightU.value)
                    leftU is ExprF.Const && leftU.value == 1 -> right
                    rightU is ExprF.Const && rightU.value == 1 -> left
                    leftU is ExprF.Const && leftU.value == 0 -> const(0)
                    rightU is ExprF.Const && rightU.value == 0 -> const(0)
                    leftU is ExprF.Mul && rightU is ExprF.Const -> {
                        val l = leftU.left.fix().unfix.fix()
                        val r = leftU.right.fix().unfix.fix()

                        when {
                            l is ExprF.Const && r is ExprF.Const -> const(rightU.value * l.value * r.value)
                            l is ExprF.Const && r !is ExprF.Const -> mul (const(rightU.value * l.value), Fix(r))
                            l !is ExprF.Const && r is ExprF.Const -> mul (const(rightU.value * r.value), Fix(l))
                            else -> Fix(i)
                        }
                    }
                    leftU is ExprF.Const && rightU is ExprF.Mul -> {
                        val l = rightU.left.fix().unfix.fix()
                        val r = rightU.right.fix().unfix.fix()

                        when {
                            l is ExprF.Const && r is ExprF.Const -> const(leftU.value * l.value * r.value)
                            l is ExprF.Const && r !is ExprF.Const -> mul (const(leftU.value * l.value), Fix(r))
                            l !is ExprF.Const && r is ExprF.Const -> mul (const(leftU.value * r.value), Fix(l))
                            else -> Fix(i)
                        }
                    }
                    leftU !is ExprF.Const && rightU is ExprF.Mul -> {
                        val l = rightU.left.fix().unfix.fix()
                        val r = rightU.right.fix().unfix.fix()

                        when {
                            l is ExprF.Const && r is ExprF.Const -> mul (Fix(leftU), const(l.value * r.value))
                            else -> Fix(i)
                        }
                    }

                    leftU is ExprF.Const && rightU is ExprF.Add -> {
                        val l = rightU.left.fix().unfix.fix()
                        val r = rightU.right.fix().unfix.fix()

                        add(
                            mul(Fix(leftU), Fix(l)),
                            mul(Fix(leftU), Fix(r))
                        )
                    }

                    rightU is ExprF.Const && leftU is ExprF.Add -> {
                        val l = leftU.left.fix().unfix.fix()
                        val r = leftU.right.fix().unfix.fix()

                        add(
                            mul(Fix(rightU), Fix(l)),
                            mul(Fix(rightU), Fix(r))
                        )
                    }

                    else -> Fix(i)
                }
            }
            is ExprF.Pow -> Fix(i)
            is ExprF.Sub -> {
                val leftU = left.unfix.fix()
                val rightU = right.unfix.fix()

                when {
                    leftU is ExprF.Const && rightU is ExprF.Const ->
                        const(leftU.value - rightU.value)
                    leftU is ExprF.Const && rightU is ExprF.Add -> {
                        val l = rightU.left.fix().unfix.fix()
                        val r = rightU.right.fix().unfix.fix()

                        when {
                            l is ExprF.Const && r is ExprF.Const -> const(leftU.value - (l.value + r.value))
                            else -> Fix(i)
                        }
                    }
                    rightU is ExprF.Const && leftU is ExprF.Add ->{
                        val l = leftU.left.fix().unfix.fix()
                        val r = leftU.right.fix().unfix.fix()

                        when {
                            l is ExprF.Const && r is ExprF.Const -> const((l.value + r.value) - rightU.value)
                            l !is ExprF.Const && r is ExprF.Const -> add(Fix(l), const(r.value - rightU.value))
                            l is ExprF.Const && r !is ExprF.Const -> add(const(l.value - rightU.value), Fix(r))
                            else -> Fix(i)
                        }
                    }
                    else -> Fix(i)
                }
            }
            ExprF.Var -> Fix(i)
        }
    }
}