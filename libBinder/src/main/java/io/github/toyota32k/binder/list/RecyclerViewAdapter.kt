package io.github.toyota32k.binder.list

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import io.github.toyota32k.binder.Binder
import io.github.toyota32k.utils.IDisposable

interface IRecyclerViewInsertEventSource {
    var insertedEventListener:((position:Int, range:Int, isLast:Boolean)->Unit)?
}

/**
 * ObservableList を使った RecyclerView.Adapter のベースクラス
 * 実装クラス：
 *  - RecyclerViewAdapter.SimpleAdapter   item の layoutId を指定して、アイテムビューを生成するタイプ
 *  - RecyclerViewAdapter.ViewBindingAdapter   ViewBinding を利用して、アイテムビューを生成するタイプ
 */
class RecyclerViewAdapter {
    abstract class Base<T, VH>(
            owner: LifecycleOwner,
            val list: ObservableList<T>
        ) : IDisposable, IRecyclerViewInsertEventSource, RecyclerView.Adapter<VH>() where VH : RecyclerView.ViewHolder {
        /**
         * 単に、this::onListChanged を渡したいだけなのだが、コンストラクタでこれをやると、
         * >> Leaking 'this' in constructor of non-final class Base
         * というワーニングが出るので、一枚ラッパをはさむ。
         */
        private inner class ListMutationListener {
            fun onListChanged(t: ObservableList.MutationEventData<T>?) {
                this@Base.onListChanged(t)
            }
        }

        private val listMutationListener = ListMutationListener()
        private var listenerKey: IDisposable? = list.addListener(owner, listMutationListener::onListChanged)

        // region IRecyclerViewInsertEventSource

        override var insertedEventListener: ((position: Int, range: Int, isLast: Boolean) -> Unit)? = null

        // endregion

        // region Disposable i/f
        @MainThread
        override fun dispose() {
            listenerKey?.let {
                listenerKey = null
                it.dispose()
            }
        }

        // endregion

        // region Observer i/f

        @SuppressLint("NotifyDataSetChanged")
        protected open fun onListChanged(t: ObservableList.MutationEventData<T>?) {
            if (t == null) return
            when (t) {
                is ObservableList.ChangedEventData -> notifyItemRangeChanged(t.position, t.range)
                is ObservableList.MoveEventData -> notifyItemMoved(t.from, t.to)
                is ObservableList.RemoveEventData -> notifyItemRangeRemoved(t.position, t.range)
                is ObservableList.InsertEventData -> {
                    notifyItemRangeInserted(t.position, t.range)
                    insertedEventListener?.invoke(t.position, t.range, t.position + t.range == list.size)
                }

                else -> notifyDataSetChanged()
            }
        }

        // endregion

        // region RecyclerView.Adapter

        override fun getItemCount(): Int {
            return list.size
        }

        abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH

        abstract override fun onBindViewHolder(holder: VH, position: Int)

        // endregion
    }

    // region Implementation

    /**
     * ViewのレイアウトIDを渡しておけば自動的にビューが生成される最もシンプルなアダプター実装クラス
     */
    class Simple<T>(
        owner:LifecycleOwner,
        list: ObservableList<T>,
        @LayoutRes private val itemViewLayoutId:Int,
        val bindView: (binder: Binder, view: View, item:T)->Unit
    ) : Base<T, Simple.SimpleViewHolder>(owner,list) {
        class SimpleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            val binder = Binder()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val itemView = inflater.inflate(itemViewLayoutId, parent, false)
            return SimpleViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
            holder.binder.reset()
            bindView(holder.binder, holder.itemView, list[position])
        }
    }

    /**
     * ViewBinding を利用するアダプター実装クラス
     */
    class ViewBindingAdapter<T,B:androidx.viewbinding.ViewBinding>(
        owner:LifecycleOwner,
        list: ObservableList<T>,
        val inflate: (parent:ViewGroup)->B,
        val bindView: (controls:B, binder: Binder, view: View, item:T)->Unit
    ) : Base<T, ViewBindingAdapter.SimpleViewHolder<B>>(owner,list) {
        class SimpleViewHolder<VB:androidx.viewbinding.ViewBinding>(val controls: VB): RecyclerView.ViewHolder(controls.root) {
            val binder = Binder()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder<B> {
            val controls = inflate(parent)
            return SimpleViewHolder(controls)
        }

        override fun onBindViewHolder(holder: SimpleViewHolder<B>, position: Int) {
            holder.binder.reset()
            bindView(holder.controls, holder.binder, holder.itemView, list[position])
        }
    }

    // endregion
}