@file:Suppress("unused")

package io.github.toyota32k.binder

import android.widget.RadioGroup
import androidx.annotation.IdRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import io.github.toyota32k.utils.lifecycle.asMutableLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface IIDValueResolver<T> {
    fun id2value(@IdRes id:Int) : T?
    fun value2id(v:T): Int
}

/**
 * コンストラクタ渡した value → id のマップを使って解決してくれる IIDValueResolver
 *
 * ```
 * val resolver = SimpleIdValueResolver<HogeFuga>(mapOf(
 *   HogeFuga.HOGE to R.id.dialog_hoge,
 *   HogeFuga.FUGA to R.id.dialog_fuga,
 * ))
 * ```
 * 
 * 当然のことながら、異なる value が同じ id を返すことがあるようなマップを渡してしまうと、
 * 期待通りには動きません。
 */
class SimpleIdValueResolver<T>(private val valueIdMap: Map<T, Int>) : IIDValueResolver<T> {
    private val idValueMap: Map<Int, T> = valueIdMap.entries.associate { (key, value) -> value to key }
    override fun id2value(id: Int): T? = idValueMap[id]
    override fun value2id(v: T): Int = valueIdMap[v] ?: 0
}

/**
 * enum classの値をidに変換する関数を使って、SimpleIdValueResolverを構築します。
 * 
 * ```
 * val resolver = SimpleIdValueResolver<HogeFuga> {
 *   when (it) {
 *     HogeFuga.HOGE -> R.id.dialog_hoge
 *     HogeFuga.FUGA -> R.id.dialog_fuga
 *   }
 * }
 * ```
 *
 * mapを渡す代わりにこっちを使うことで、 when の網羅性チェックの恩恵を
 * 受けられます（後になってenumにメンバを追加したときにここがエラーになってくれるので
 * 修正漏れを防げます）。
 * 
 * 当然のことながら、異なる値に対して同じ id を返すことがあるような関数を渡してしまうと、
 * 期待通りには動きません。
 */
@Suppress("FunctionName")
inline fun <reified T: Enum<T>> SimpleIdValueResolver(valueToId: (value: T) -> Int): SimpleIdValueResolver<T>
    = SimpleIdValueResolver(enumValues<T>().associateWith(valueToId))

open class RadioGroupBinding<T> (
    override val data: LiveData<T>,
    mode: BindingMode
) : BaseBinding<T>(mode), RadioGroup.OnCheckedChangeListener {
    constructor(data:LiveData<T>):this(data, BindingMode.OneWay)

    private lateinit var idResolver: IIDValueResolver<T>
    private val radioGroup:RadioGroup?
        get() = view as? RadioGroup

    fun connect(owner: LifecycleOwner, view:RadioGroup, idResolver: IIDValueResolver<T>) {
        this.idResolver = idResolver
        super.connect(owner,view)
        if(mode!= BindingMode.OneWay) {
            view.setOnCheckedChangeListener(this)
            if(mode== BindingMode.OneWayToSource||data.value==null) {
                radioGroup?.apply {
                    onCheckedChanged(this, checkedRadioButtonId)
                }
            }
        }
    }

    override fun dispose() {
        radioGroup?.setOnCheckedChangeListener(null)
        super.dispose()
    }

    override fun onDataChanged(v: T?) {
        val view = radioGroup ?: return
        if(v!=null) {
            val id = idResolver.value2id(v)
            if(view.checkedRadioButtonId!=id) {
                view.check(id)
            }
        }
    }

    override fun onCheckedChanged(@Suppress("UNUSED_PARAMETER") group: RadioGroup, @IdRes checkedId: Int) {
        val v = idResolver.id2value(checkedId)
        mutableData?.apply {
            if (value != v) {
                value = v
            }
        }
    }

    companion object {
        fun <T> create(owner: LifecycleOwner, view: RadioGroup, data: LiveData<T>, idResolver: IIDValueResolver<T>): RadioGroupBinding<T> {
            return RadioGroupBinding(data).apply { connect(owner, view, idResolver) }
        }
        fun <T> create(owner: LifecycleOwner, view: RadioGroup, data: MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): RadioGroupBinding<T> {
            return RadioGroupBinding(data, mode).apply { connect(owner, view, idResolver) }
        }
        // for StateFlow
        fun <T> create(owner: LifecycleOwner, view: RadioGroup, data: Flow<T>, idResolver: IIDValueResolver<T>): RadioGroupBinding<T> {
            return create(owner, view, data.asLiveData(), idResolver)
        }
        fun <T> create(owner: LifecycleOwner, view: RadioGroup, data: MutableStateFlow<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): RadioGroupBinding<T> {
            return create(owner, view, data.asMutableLiveData(owner), idResolver, mode)
        }
    }
}

fun <T> Binder.radioGroupBinding(owner: LifecycleOwner, view: RadioGroup, data: LiveData<T>, idResolver: IIDValueResolver<T>): Binder
        = add(RadioGroupBinding.create(owner, view, data, idResolver))
fun <T> Binder.radioGroupBinding(owner: LifecycleOwner, view: RadioGroup, data: MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(RadioGroupBinding.create(owner, view, data, idResolver, mode))
fun <T> Binder.radioGroupBinding(owner: LifecycleOwner, view: RadioGroup, data: Flow<T>, idResolver: IIDValueResolver<T>): Binder
        = add(RadioGroupBinding.create(owner, view, data, idResolver))
fun <T> Binder.radioGroupBinding(owner: LifecycleOwner, view: RadioGroup, data: MutableStateFlow<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(RadioGroupBinding.create(owner, view, data, idResolver, mode))
fun <T> Binder.radioGroupBinding(view: RadioGroup, data: LiveData<T>, idResolver: IIDValueResolver<T>): Binder
        = add(RadioGroupBinding.create(requireOwner, view, data, idResolver))
fun <T> Binder.radioGroupBinding(view: RadioGroup, data: MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(RadioGroupBinding.create(requireOwner, view, data, idResolver, mode))
fun <T> Binder.radioGroupBinding(view: RadioGroup, data: Flow<T>, idResolver: IIDValueResolver<T>): Binder
        = add(RadioGroupBinding.create(requireOwner, view, data, idResolver))
fun <T> Binder.radioGroupBinding(view: RadioGroup, data: MutableStateFlow<T>, idResolver: IIDValueResolver<T>, mode: BindingMode = BindingMode.TwoWay): Binder
        = add(RadioGroupBinding.create(requireOwner, view, data, idResolver, mode))
