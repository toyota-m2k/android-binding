@file:Suppress("unused")

package io.github.toyota32k.binder.anim

import android.view.View


class VisibilityAnimation private constructor(private val valueAnimation: ReversibleValueAnimation) : IReversibleAnimation by valueAnimation {
    constructor(duration: Long) : this(ReversibleValueAnimation(duration))

    init {
        valueAnimation
            .onStart(this::onStart)
            .onEnd(this::onEnd)
            .onUpdate(this::onUpdate)
    }

    private val views = mutableListOf<View>()

    fun addView(vararg v:View): VisibilityAnimation {
        views.addAll(v)
        return this
    }
    
    operator fun plus(v:View): VisibilityAnimation {
        views.add(v)
        return this
    }

    private fun onStart(reverse:Boolean){
        if(!reverse) {
            // invisible --> visible
            views.forEach { view->
                view.alpha = 0f
                view.visibility = View.VISIBLE
            }
        }
    }
    private fun onEnd(reverse:Boolean){
        if(reverse) {
            // visible-->invisible
            views.forEach { view->
                view.alpha = 0f
                view.visibility = View.INVISIBLE
            }
        } else {
            // invisible --> visible
            views.forEach { view->
                view.alpha = 1f
                view.visibility = View.VISIBLE
            }
        }
    }

    private fun onUpdate(value:Float){
        views.forEach { view->
            view.alpha = value
        }
    }
}