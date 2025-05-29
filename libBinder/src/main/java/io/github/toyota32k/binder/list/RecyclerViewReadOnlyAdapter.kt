package io.github.toyota32k.binder.list

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import io.github.toyota32k.binder.Binder
import io.github.toyota32k.utils.IDisposable

@SuppressLint("NotifyDataSetChanged")
class RecyclerViewReadOnlyAdapter<T>(
    owner: LifecycleOwner,
    private val listSource: LiveData<Collection<T>>,
    @LayoutRes private val itemViewLayoutId:Int,
    private val bindView: (binder: Binder, view: View, item:T)->Unit
) : IDisposable, Observer<Collection<T>>, RecyclerView.Adapter<RecyclerViewReadOnlyAdapter.ViewHolder>() {
    var list:List<T> = emptyList()
    init {
        listSource.observe(owner, this)
    }
    override fun dispose() {
        listSource.removeObserver(this)
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val binder = Binder()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(itemViewLayoutId, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binder.reset()
        bindView(holder.binder, holder.itemView, list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onChanged(value: Collection<T>) {
        list = value.toList()
        notifyDataSetChanged()
    }
}

@SuppressLint("NotifyDataSetChanged")
class RecyclerViewViewBindingReadOnlyAdapter<T, B:androidx.viewbinding.ViewBinding>(
    owner: LifecycleOwner,
    private val listSource: LiveData<Collection<T>>,
    val inflate: (parent:ViewGroup)->B,
    val bindView: (controls:B, binder: Binder, view: View, item:T)->Unit
) : IDisposable, Observer<Collection<T>>, RecyclerView.Adapter<RecyclerViewViewBindingReadOnlyAdapter.ViewHolder<B>>() {
    var list:List<T> = emptyList()
    init {
        listSource.observe(owner, this)
    }
    override fun dispose() {
        listSource.removeObserver(this)
    }

    class ViewHolder<B:androidx.viewbinding.ViewBinding>(val controls: B): RecyclerView.ViewHolder(controls.root) {
        val binder = Binder()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<B> {
        val controls = inflate(parent)
        return ViewHolder(controls)
    }

    override fun onBindViewHolder(holder: ViewHolder<B>, position: Int) {
        holder.binder.reset()
        bindView(holder.controls, holder.binder, holder.itemView, list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onChanged(value: Collection<T>) {
        list = value.toList()
        notifyDataSetChanged()
    }
}