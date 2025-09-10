package io.github.toyota32k.binder.list

import android.database.DataSetObserver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.LayoutRes
import io.github.toyota32k.binder.Binder
import io.github.toyota32k.utils.IDisposable

open class ListViewAdapter<T>(
    private val list: List<T>,
    @param:LayoutRes private val  itemViewLayoutId: Int,
    private val bindView:(Binder, View, T)->Unit
) : BaseAdapter(), IDisposable {

    val binderMap = mutableMapOf<View, Binder>()

    interface IIdHolder {
        val id:Long
    }

    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(position: Int): Any {
        return list[position]!!
    }

    override fun getItemId(position: Int): Long {
        val v = getItem(position)
        return if(v is IIdHolder) {
            v.id
        } else {
            position.toLong()
        }
    }

    /**
     * アイテムビューを作る
     */
    open fun createItemView(parent: ViewGroup):View {
        val inflater = LayoutInflater.from(parent.context)
        return inflater.inflate(itemViewLayoutId, parent, false)
    }

    /**
     * アイテムビューにデータをセットする
     */
    open fun updateItemView(itemView:View, position:Int) {
        val binder = binderMap[itemView] ?: Binder().apply { binderMap[itemView] = this }
        binder.reset()
        bindView(binder, itemView, list[position])
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: createItemView(parent)
        updateItemView(itemView, position)
        return itemView
    }

    override fun dispose() {
    }
}

class MutableListViewAdapter<T>(
    private val observableList: ObservableList<T>,
    @LayoutRes itemViewLayoutId: Int,
    bindView:(Binder, View, T)->Unit
    ) : ListViewAdapter<T>(observableList, itemViewLayoutId, bindView) {
    private var disposable: IDisposable? = null

    override fun registerDataSetObserver(observer: DataSetObserver?) {
        disposable?.dispose()
        disposable = observableList.addListenerForever {
            notifyDataSetChanged()
        }
        super.registerDataSetObserver(observer)
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver?) {
        disposable?.dispose()
        disposable = null
        super.unregisterDataSetObserver(observer)
    }

    override fun dispose() {
        super.dispose()
        disposable?.let {
            notifyDataSetInvalidated()
            it.dispose()
            disposable = null
        }
    }
}