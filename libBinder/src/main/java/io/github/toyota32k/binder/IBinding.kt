package io.github.toyota32k.binder

import io.github.toyota32k.utils.IDisposable

enum class BindingMode {
    OneWay,
    OneWayToSource,
    TwoWay,
}

enum class BoolConvert {
    Straight,   // true --> true
    Inverse,    // true --> false
    ;
    fun conv(value:Boolean):Boolean {
        return if(this== Straight) value else !value
    }
}

interface IBinding : IDisposable {
    val mode: BindingMode
}

