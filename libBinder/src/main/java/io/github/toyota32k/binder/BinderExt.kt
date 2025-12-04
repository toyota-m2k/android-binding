package io.github.toyota32k.binder

import android.view.View
import android.view.ViewTreeObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import io.github.toyota32k.utils.GenericDisposable
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.lifecycle.disposableObserve
import kotlinx.coroutines.flow.Flow
import java.lang.ref.WeakReference

fun <T> Binder.observe(owner: LifecycleOwner, data: LiveData<T>, fn:(value:T)->Unit):Binder
        = add(data.disposableObserve(owner,fn))
fun <T> Binder.observe(data:LiveData<T>,fn:(value:T)->Unit):Binder
        = observe(requireOwner, data, fn)

fun <T> Binder.observe(owner:LifecycleOwner, data: Flow<T>, callback:(value:T)->Unit):Binder
    = add(data.disposableObserve(owner,callback))

fun <T> Binder.observe(data:Flow<T>, callback:(value:T)->Unit):Binder
        = add(data.disposableObserve(requireOwner,callback))

/**
 * addOnGlobalLayoutListener / removeOnGlobalLayoutListener を IDisposable 化するクラス。
 *
 * @param view addOnGlobalLayoutListener / removeOnGlobalLayoutListener を適用するView
 * @param callback addOnGlobalLayoutListenerで呼ばれるコールバック
 */
class DisposableGlobalLayoutListener(
    val view: View,
    callback: ()->Boolean) : IDisposable, ViewTreeObserver.OnGlobalLayoutListener {
    /**
     * @param oneTime true: 1回だけ実行するコールバックを登録 / false: dispose()するまでコールバックし続ける
     * @param callback addOnGlobalLayoutListenerで呼ばれるコールバック
     */
    constructor(view:View, oneTime:Boolean, callback: ()->Unit) : this(view, {
        callback()
        !oneTime
    })

    private var mCallback: (() -> Boolean)? = callback
    init {
        view.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun dispose() {
        val callback = mCallback
        if (callback!=null) {
            mCallback = null
            view.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }

    override fun onGlobalLayout() {
        val callback = mCallback
        if (callback!=null) {
            if (!callback()) {
                dispose()
            }
        }
    }

    companion object {
        /**
         * DisposableGlobalLayoutListenerインスタンスを作ることなく１回こっきりのリスナーを登録する
         */
        fun onInitialLayout(view: View, callback: ()->Unit) {
            val listener: ViewTreeObserver.OnGlobalLayoutListener
                    = object: ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    callback()
                }
            }
            view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        }
    }
}

/**
 * addOnGlobalLayoutListener / removeOnGlobalLayoutListener をBinderのスコープを使って自動化
 * @param view addOnGlobalLayoutListener / removeOnGlobalLayoutListener を適用するView
 * @param callback addOnGlobalLayoutListenerで呼ばれるコールバック。false を返すと、dispose()される。
 */
fun Binder.onGlobalLayout(view: View, callback: ()->Boolean)
        = add(DisposableGlobalLayoutListener(view, callback))
/**
 * onGlobalLayoutの特殊形
 * onCreate()で初回のレイアウト更新を１回だけ取得したい場合に利用。
 */
fun Binder.onInitialLayout(view: View, callback: ()->Unit)
    = add(DisposableGlobalLayoutListener(view, oneTime=true, callback))

/**
 * addOnLayoutChangeListener / removeOnLayoutChangeListener をBinderのスコープを使って自動化
 */
fun Binder.onLayoutChanged(view: View,
                           callback: View.OnLayoutChangeListener):Binder
        = add(GenericDisposable.create {
    view.addOnLayoutChangeListener(callback)
    return@create { view.removeOnLayoutChangeListener(callback) }
})

/**
 * addOnLayoutChangeListener / removeOnLayoutChangeListener を IDisposable 化するクラス。
 * onLayoutChangedが、素の View.OnLayoutChangeListener とバインドするのに対して、
 * ViewSizeChangeListenerは、width/height を引数にとるリスナーを呼び出す。
 * また、onGoodSize = true にセットすると、有効な width/height が得られるまでコールバックしない。
 */
class ViewSizeChangeListener(view: View, private val onGoodSize:Boolean, private val onSizeChanged:(width:Int,height:Int)->Unit): View.OnLayoutChangeListener, IDisposable {
    constructor(view: View, onSizeChanged:(width:Int,height:Int)->Unit):this(view, true, onSizeChanged)

    private val viewRef = WeakReference(view)
    init {
        view.addOnLayoutChangeListener(this)
    }

    override fun dispose() {
        viewRef.get()?.removeOnLayoutChangeListener(this)
    }

    private var prevWidth:Int = 0
    private var prevHeight:Int = 0
    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        val newWidth = right - left
        val newHeight = bottom - top
        if (prevWidth != newWidth || prevHeight != newHeight) {
            if (!onGoodSize|| (newWidth > 0 && newHeight > 0)) {
                prevWidth = newWidth
                prevHeight = newHeight
                onSizeChanged(newWidth, newHeight)
            }
        }
    }
}

/**
 * ViewSizeChangeListener(onGoodSize=true) を作成してBinder に追加する拡張関数
 */
fun Binder.onViewSizeChanged(view: View, onSizeChanged:(width:Int,height:Int)->Unit):Binder
        = add(ViewSizeChangeListener(view, onSizeChanged))