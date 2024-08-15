package io.github.toyota32k.binder

import androidx.annotation.IdRes

enum class TestEnum(@IdRes val id:Int) {
    E0(1000),
    E1(1001),
    E2(1002),
    E3(1003);

    companion object {
        fun valueOf(@IdRes id: Int): TestEnum? {
            return entries.find { it.id == id }
        }
    }
    object IDResolver:IIDValueResolver<TestEnum> {
        override fun id2value(id: Int): TestEnum? = valueOf(id)
        override fun value2id(v: TestEnum): Int = v.id
    }

}

