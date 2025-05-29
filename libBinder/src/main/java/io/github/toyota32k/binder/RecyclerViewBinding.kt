@file:Suppress("unused")

package io.github.toyota32k.binder

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.*
import io.github.toyota32k.binder.list.IRecyclerViewInsertEventSource
import io.github.toyota32k.binder.list.ObservableList
import io.github.toyota32k.binder.list.RecyclerViewAdapter
import io.github.toyota32k.utils.ConstantLiveData
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.disposableObserve
import kotlinx.coroutines.flow.Flow

class RecyclerViewBinding<T> private constructor(
    val list: ObservableList<T>,
    val view: RecyclerView
) : IBinding {

    override val mode: BindingMode = BindingMode.OneWay

    private var gestureDisposable: IDisposable? = null
    private var dragAndDropDisposable: IDisposable? = null
    private var autoScrollDisposable: IDisposable? = null

    override fun dispose() {
        itemTouchHelper?.attachToRecyclerView(null)
        gestureDisposable?.dispose()
        dragAndDropDisposable?.dispose()
        autoScrollDisposable?.dispose()
//        list.dispose()    adapter の リスナーは、adapter.dispose()でクリアされるので、これは不要。むしろ、Binder以外でaddされたリスナーも解除されてしまうのでダメぜったい
        (view.adapter as? IDisposable)?.dispose()
    }

    private var itemTouchHelper: ItemTouchHelper? = null

    interface IDeletion {
        /**
         * アイテムの削除確定 --> 実体の削除などを行う。
         */
        fun commit()
    }
    interface IPendingDeletion : IDeletion {
        //fun deleting(deletingItem:T):String
        val itemLabel:String
        val undoButtonLabel:String?

        /**
         * 削除中止 --> リストは自動的に更新されるので、通常はなにもしなくてよい。
         * リストの選択を元に戻すなら、このタイミングで。
         */
        fun rollback()
    }

    data class GestureParams<T>(
        val dragToMove:Boolean,                 // D&D によるアイテムの移動をサポートするか？
        val swipeToDelete:Boolean,              // スワイプによるアイテム削除をサポートするか？
        val deletionHandler:((T)-> IDeletion)? = null
    )
    fun enableGesture(params: GestureParams<T>?) {
        if(params==null) {
            itemTouchHelper?.attachToRecyclerView(null)
            itemTouchHelper = null
            return
        }
        enableGesture(params.dragToMove, params.swipeToDelete, params.deletionHandler)
    }
    fun enableGesture(dragToMove:Boolean, swipeToDelete:Boolean, deletionHandler:((T)-> IDeletion)?) {
        itemTouchHelper?.attachToRecyclerView(null)
        itemTouchHelper = null
        if(dragToMove||swipeToDelete) {
            val dragDirs = if(dragToMove) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0
            val swipeDirs = if(swipeToDelete) ItemTouchHelper.RIGHT else 0
            itemTouchHelper = ItemTouchHelper(object:ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {
                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    if(!dragToMove) return false
                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition
                    if(from==to) return false
                    list.move(from, to)
                    return true
                }
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    if(!swipeToDelete) return
                    val pos = viewHolder.bindingAdapterPosition
                    val item = list[pos]
                    val deletion = deletionHandler?.invoke(item)    // itemを削除する前（まだitemが存在するタイミングで）呼ぶので、リストの選択を変更するなら、このタイミングでやる。
                    list.removeAt(pos)
                    if(deletion !is IPendingDeletion) {
                        // Undo無効 --> 即通知
                        deletion?.commit()
                    } else {
                        val label = deletion.itemLabel
                        // Undo有効
                        var undo = false
                        // below line is to display our snackbar with action.
                        Snackbar.make(view, label, Snackbar.LENGTH_LONG).setAction(deletion.undoButtonLabel?:"Undo") {
                            undo = true
                            list.add(pos, item)
                            deletion.rollback()     // リストは自動的に更新されるから、通常はなにもしなくてok、リスト選択を戻すならこのタイミングでやる。
                        }.addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                if(!undo) {
                                    // Undo用 Snackbarが消えた時点で undo されていなければ呼びだし元に通知
                                    deletion.commit()
                                }
                            }
                        }).apply {
                          view.setOnClickListener { dismiss() }
                        }.show()
                    }
                }
            }).apply { attachToRecyclerView(view) }
        }
    }
    fun enableGesture(owner: LifecycleOwner, params:LiveData<GestureParams<T>?>) {
        gestureDisposable?.dispose()
        gestureDisposable = params.disposableObserve(owner) { enableGesture(it) }
    }

//    private var dragAndDropHelper: ItemTouchHelper? = null
    fun enableDragAndDrop(sw:Boolean) {
        enableGesture(dragToMove = sw, swipeToDelete = false, deletionHandler = null)
//        if(itemTouchHelper!=null) {
//            throw IllegalStateException("gesture enabled already.")
//        }
//        if(sw) {
//            if(dragAndDropHelper==null) {
//                dragAndDropHelper = ItemTouchHelper(object:ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
//                    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
//                        val from = viewHolder.bindingAdapterPosition
//                        val to = target.bindingAdapterPosition
//                        list.move(from, to)
//                        return true
//                    }
//
//                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//                    }
//                }).apply { attachToRecyclerView(view) }
//            }
//        } else {
//            dragAndDropHelper?.attachToRecyclerView(null)
//            dragAndDropHelper = null
//        }
    }
    fun enableDragAndDrop(owner: LifecycleOwner, sw:LiveData<Boolean>) {
        dragAndDropDisposable?.dispose()
        dragAndDropDisposable = sw.disposableObserve(owner) { enableDragAndDrop(it) }
    }
    /**
     * 自動スクロールのモード
     */
    enum class AutoScrollMode {
        NONE,       // 自動スクロールしない
        ALL,        // アイテムが挿入されたら常にスクロール
        ONLY_TAIL,  // リストの末尾に挿入された場合にのみスクロールする
    }
    /**
     * アイテムが挿入されたときに、そのアイテムが画面内に表示されるようにスクロールするかどうかの指定
     */
    fun enableAutoScroll(mode:AutoScrollMode) {
        val src = view.adapter as? IRecyclerViewInsertEventSource ?: return
        if (mode!= AutoScrollMode.NONE) {
            src.insertedEventListener = { position, range, isLast ->
                if (mode== AutoScrollMode.ALL||isLast) {
                    view.smoothScrollToPosition(position + range - 1)
                }
            }
        } else {
            src.insertedEventListener = null
        }
    }
    fun enableAutoScroll(owner: LifecycleOwner, sw:LiveData<AutoScrollMode>) {
        autoScrollDisposable?.dispose()
        autoScrollDisposable = sw.disposableObserve(owner) { enableAutoScroll(it) }
    }

//    override fun isDisposed(): Boolean {
//        return (view.adapter as? IDisposable)?.isDisposed() ?: false
//    }

    companion object {
        fun <T> create(owner: LifecycleOwner, view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
                       fixedSize:Boolean = true,
                       layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
                       bindView:(Binder, View, T)->Unit) : RecyclerViewBinding<T> {
            return RecyclerViewBinding(list,view).apply {
                view.setHasFixedSize(fixedSize)
                view.layoutManager = layoutManager
                view.adapter = RecyclerViewAdapter.SimpleAdapter(owner,list,itemViewLayoutId,bindView)
            }
        }
        fun <T,B: ViewBinding> create(owner: LifecycleOwner, view: RecyclerView, list: ObservableList<T>,
                                      fixedSize:Boolean = true,
                                      layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
                                      inflate: (parent:ViewGroup)->B,
                                      bindView:(B, Binder, View, T)->Unit) : RecyclerViewBinding<T> {
            return RecyclerViewBinding(list,view).apply {
                view.setHasFixedSize(fixedSize)
                view.layoutManager = layoutManager
                view.adapter = RecyclerViewAdapter.ViewBindingAdapter<T,B>(owner,list,inflate,bindView)
            }
        }
        // RecyclerView で、layout_height = wrap_content を指定しても、ビューの高さがコンテントの増減に追従しないので、
        // stackoverflow の記事を参考に、adapter を差し替える荒業で乗り切ったつもりでいたが、単に、setHasFixedSize(true) にしていたから、サイズが変更しなかっただけだったことが判明。
        // setHasFixedSize(false)にすれば、RecyclerViewAdapter.Simple で期待通りに動作することを確認した。
//        fun <T> createHeightWrapContent(owner: LifecycleOwner, view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int, bindView:(Binder, View, T)->Unit) : RecycleViewBinding<T> {
//            return RecycleViewBinding(list,view).apply {
//                view.adapter = RecyclerViewAdapter.HeightWrapContent(owner,list,itemViewLayoutId,view,bindView)
//            }
//        }
//        fun <T,B> create(owner: LifecycleOwner, view: RecyclerView, list:ObservableList<T>, createView:(parent: ViewGroup, viewType:Int)->B, bind: (binding: B, item:T)->Unit) : RecycleViewBinding<T>
//        where B: ViewDataBinding {
//            return RecycleViewBinding(list,view).apply {
//                view.adapter = RecyclerViewAdapter.SimpleWithDataBinding<T,B>(owner,list,createView,bind)
//            }
//        }
    }
    abstract class BuilderBase<T>(val owner: LifecycleOwner, val view:RecyclerView) {
        protected var mObservableList: ObservableList<T>? = null
        protected var mReadOnlyListLiveData: LiveData<Collection<T>>? = null
        protected var mFixedSize:Boolean = true
        protected var mLayoutManager: RecyclerView.LayoutManager? = null// = LinearLayoutManager(view.context),
        protected var mDragAndDrop: Boolean = false
        protected var mDragAndDropLiveData: LiveData<Boolean>? = null
        protected var mGestureParams: GestureParams<T>? = null
        protected var mGestureParamsLiveData: LiveData<GestureParams<T>?>? = null
        protected var mAutoScroll: AutoScrollMode = AutoScrollMode.NONE
        fun list(l:ObservableList<T>) = apply { mObservableList = l }
        fun list(l:LiveData<Collection<T>>) = apply { mReadOnlyListLiveData = l }
        fun list(l:Flow<Collection<T>>) = apply { mReadOnlyListLiveData = l.asLiveData() }
        fun list(l:Collection<T>) = apply { mReadOnlyListLiveData = ConstantLiveData(l) }
        fun fixedSize(f:Boolean) = apply { mFixedSize = f }
        fun layoutManager(l:RecyclerView.LayoutManager) = apply { mLayoutManager = l }
        fun dragAndDrop(d:Boolean) = apply { mDragAndDrop = d }
        fun dragAndDrop(d:LiveData<Boolean>) = apply { mDragAndDropLiveData = d }
        fun dragAndDrop(d:Flow<Boolean>) = apply { mDragAndDropLiveData = d.asLiveData() }
        fun gestureParams(p:GestureParams<T>) = apply { mGestureParams = p }
        fun gestureParams(d:LiveData<GestureParams<T>?>) = apply { mGestureParamsLiveData = d }
        fun gestureParams(d:Flow<GestureParams<T>?>) = apply { mGestureParamsLiveData = d.asLiveData() }
        fun autoScroll(mode: AutoScrollMode) = apply { mAutoScroll = mode }

        protected fun applyExtensions(bindings:RecyclerViewBinding<T>) {
            val gestureParams = mGestureParams
            val gestureParamsLiveData = mGestureParamsLiveData
            val dragAndDropLiveData = mDragAndDropLiveData
            if(gestureParamsLiveData!=null) {
                bindings.enableGesture(owner, gestureParamsLiveData)
            } else if(gestureParams!=null) {
                bindings.enableGesture(gestureParams)
            } else if(dragAndDropLiveData!=null) {
                bindings.enableDragAndDrop(owner, dragAndDropLiveData)
            } else if(mDragAndDrop) {
                bindings.enableDragAndDrop(true)
            }
            bindings.enableAutoScroll(mAutoScroll)
        }

        abstract fun build(binder:Binder)
    }
    class SimpleBuilder<T>(owner: LifecycleOwner, view:RecyclerView):BuilderBase<T>(owner,view) {
        private var mItemLayoutId:Int = -1
        private var mBindView:((Binder, View, T)->Unit)? = null

        fun itemLayoutId(@LayoutRes id:Int) = apply { mItemLayoutId = id }
        fun bindView(b:(Binder, View, T)->Unit) = apply { mBindView = b }

        override fun build(binder:Binder) {
            val observableList = mObservableList
            val readonlyList = mReadOnlyListLiveData
            val bindView = mBindView ?: throw IllegalStateException("bindView is not set.")
            binder + if(observableList!=null) {
                if (readonlyList!=null) {
                    throw IllegalStateException("list and readonlyList are both set.")
                }
                @Suppress("RemoveRedundantQualifierName")
                RecyclerViewBinding.create(
                    owner,
                    view,
                    observableList,
                    mItemLayoutId,
                    mFixedSize,
                    mLayoutManager ?: LinearLayoutManager(view.context),
                    bindView
                ).apply {
                    applyExtensions(this)
                }
            } else if(readonlyList!=null) {
                RecyclerViewReadOnlyBinding.create(
                    owner,
                    view,
                    readonlyList,
                    mItemLayoutId,
                    bindView
                )
            } else {
                throw IllegalStateException("list is not set.")
            }
        }
    }
    class ViewBindingBuilder<T,B:ViewBinding>(owner: LifecycleOwner, view:RecyclerView): BuilderBase<T>(owner,view) {
        var mBindView:((B, Binder, View, T)->Unit)? = null
        var mInflate:((parent:ViewGroup)->B)? = null
        fun bindView(b:(B, Binder, View, T)->Unit) = apply { mBindView = b }
        fun inflate(inflate:(parent:ViewGroup)->B) = apply { mInflate = inflate }

        override fun build(binder:Binder) {
            val observableList = mObservableList
            val readonlyList = mReadOnlyListLiveData
            val inflate = mInflate ?: throw IllegalStateException("inflate is not set.")
            val bindView = mBindView ?: throw IllegalStateException("bindView is not set.")
            if(observableList!=null) {
                @Suppress("RemoveRedundantQualifierName")
                RecyclerViewBinding.create(
                    owner,
                    view,
                    observableList,
                    mFixedSize,
                    mLayoutManager ?: LinearLayoutManager(view.context),
                    inflate,
                    bindView
                ).apply {
                    applyExtensions(this)
                }
            } else if(readonlyList!=null) {
                RecyclerViewReadOnlyBinding.create(
                    owner,
                    view,
                    readonlyList,
                    inflate,
                    bindView
                )
            } else {
                throw IllegalStateException("list is not set.")
            }
        }
    }
}

fun <T> Binder.recyclerViewBinding(owner:LifecycleOwner, view:RecyclerView, fn:RecyclerViewBinding.SimpleBuilder<T>.()->Unit) : Binder =
    apply {
        val builder = RecyclerViewBinding.SimpleBuilder<T>(owner,view)
        builder.fn()
        builder.build(this)
    }
fun <T> Binder.recyclerViewBinding(view:RecyclerView, fn:RecyclerViewBinding.SimpleBuilder<T>.()->Unit) : Binder =
    recyclerViewBinding(requireOwner, view, fn)

fun <T, B:ViewBinding> Binder.recyclerViewBindingEx(owner:LifecycleOwner, view:RecyclerView, fn:RecyclerViewBinding.ViewBindingBuilder<T,B>.()->Unit) : Binder =
    apply {
        val builder = RecyclerViewBinding.ViewBindingBuilder<T,B>(owner,view)
        builder.fn()
        builder.build(this)
    }
fun <T, B:ViewBinding> Binder.recyclerViewBindingEx(view:RecyclerView, fn:RecyclerViewBinding.ViewBindingBuilder<T,B>.()->Unit) : Binder =
    recyclerViewBindingEx(requireOwner, view, fn)

//// region Owner 引数ありコーナー
//
///**
// * D&D / Gesture サポートなし
// */
//fun <T> Binder.recyclerViewBinding(
//    owner: LifecycleOwner, view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    bindView:(Binder, View, T)->Unit): Binder
//        = add(
//    RecyclerViewBinding.create(
//        owner,
//        view,
//        list,
//        itemViewLayoutId,
//        fixedSize,
//        layoutManager,
//        bindView
//    )
//)
//
///**
// * D&D サポート指定あり
// */
//fun <T> Binder.recyclerViewBinding(
//    owner: LifecycleOwner, view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    dragAndDrop:Boolean,
//    bindView:(Binder, View, T)->Unit): Binder
//        = add(
//    RecyclerViewBinding.create(
//        owner,
//        view,
//        list,
//        itemViewLayoutId,
//        fixedSize,
//        layoutManager,
//        bindView
//    ).apply { enableDragAndDrop(dragAndDrop) })
//
//
///**
// * D&D の動的サポート(LiveData版）
// */
//fun <T> Binder.recyclerViewBinding(
//    owner: LifecycleOwner, view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    dragAndDrop: LiveData<Boolean>,
//    bindView:(Binder, View, T)->Unit): Binder {
//    val b = RecyclerViewBinding.create(
//        owner,
//        view,
//        list,
//        itemViewLayoutId,
//        fixedSize,
//        layoutManager,
//        bindView
//    )
//    return genericBoolBinding(owner, view, dragAndDrop) {_,dd-> b.enableDragAndDrop(dd) }
//         .recyclerViewBinding(owner,view,list,itemViewLayoutId,fixedSize,layoutManager,bindView)
//}
//
///**
// * D&D の動的サポート(Flow版）
// */
//fun <T> Binder.recyclerViewBinding(
//    owner: LifecycleOwner, view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    dragAndDrop: Flow<Boolean>,
//    bindView:(Binder, View, T)->Unit): Binder {
//    val b = RecyclerViewBinding.create(
//        owner,
//        view,
//        list,
//        itemViewLayoutId,
//        fixedSize,
//        layoutManager,
//        bindView
//    )
//    return genericBoolBinding(owner, view, dragAndDrop) { _,dd-> b.enableDragAndDrop(dd) }
//        .recyclerViewBinding(owner,view,list,itemViewLayoutId,fixedSize,layoutManager,bindView)
//}
//
///**
// * Gesture の静的サポート（ばらばらに指定する版）
// *
// * recyclerViewBinding --> recyclerViewGestureBinding ...
// * JVMのやつが、dragAndDrop:LiveData<Boolean> と gestureParams: LiveData<RecyclerViewBinding.GestureParams<T>?> を区別できないから名前を変えねばならなかった。
// */
//fun <T> Binder.recyclerViewGestureBinding(
//    owner: LifecycleOwner, view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    dragToMove:Boolean, swipeToDelete:Boolean, deletionHandler:((T)-> RecyclerViewBinding.IDeletion)?,
//    bindView:(Binder, View, T)->Unit): Binder
//        = add(
//    RecyclerViewBinding.create(
//        owner,
//        view,
//        list,
//        itemViewLayoutId,
//        fixedSize,
//        layoutManager,
//        bindView
//    ).apply { enableGesture(dragToMove,swipeToDelete,deletionHandler) })
//
///**
// * Gesture の静的サポート (GestureParamsで指定する版）
// */
//fun <T> Binder.recyclerViewGestureBinding(
//    owner: LifecycleOwner, view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    gestureParams: RecyclerViewBinding.GestureParams<T>?,
//    bindView:(Binder, View, T)->Unit): Binder
//        = add(
//    RecyclerViewBinding.create(
//        owner,
//        view,
//        list,
//        itemViewLayoutId,
//        fixedSize,
//        layoutManager,
//        bindView
//    ).apply { enableGesture(gestureParams) })
///**
// * Gesture の動的サポート(LiveData版）
// */
//fun <T> Binder.recyclerViewGestureBinding(
//    owner: LifecycleOwner, view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    gestureParams: LiveData<RecyclerViewBinding.GestureParams<T>?>,
//    bindView:(Binder, View, T)->Unit): Binder {
//    val b = RecyclerViewBinding.create(
//        owner,
//        view,
//        list,
//        itemViewLayoutId,
//        fixedSize,
//        layoutManager,
//        bindView
//    )
//    return genericBinding(owner, view, gestureParams) {_,p-> b.enableGesture(p) }
//        .recyclerViewBinding(owner,view,list,itemViewLayoutId,fixedSize,layoutManager,bindView)
//}
///**
// * Gesture の動的サポート(Flow版）
// */
//fun <T> Binder.recyclerViewGestureBinding(
//    owner: LifecycleOwner, view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    gestureParams: Flow<RecyclerViewBinding.GestureParams<T>?>,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    bindView:(Binder, View, T)->Unit): Binder {
//    val b = RecyclerViewBinding.create(
//        owner,
//        view,
//        list,
//        itemViewLayoutId,
//        fixedSize,
//        layoutManager,
//        bindView
//    )
//    return genericBinding(owner, view, gestureParams) {_,p-> b.enableGesture(p) }
//        .recyclerViewBinding(owner,view,list,itemViewLayoutId,fixedSize,layoutManager,bindView)
//}
//
//// endregion
//
//// region Owner 引数なしコーナー
//
///**
// * D&Dサポートなし
// */
//fun <T> Binder.recyclerViewBinding(
//    view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    bindView:(Binder, View, T)->Unit): Binder
//        = recyclerViewBinding(requireOwner,view,list,itemViewLayoutId,fixedSize,layoutManager,bindView)
///**
// * D&D サポート指定あり
// */
//fun <T> Binder.recyclerViewBinding(
//    view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    dragAndDrop:Boolean,
//    bindView:(Binder, View, T)->Unit): Binder
//        = recyclerViewBinding(requireOwner,view,list,itemViewLayoutId,fixedSize,layoutManager,dragAndDrop,bindView)
//
///**
// * D&D の動的サポート(LiveData版）
// */
//fun <T> Binder.recyclerViewBinding(
//    view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    dragAndDrop: LiveData<Boolean>,
//    bindView:(Binder, View, T)->Unit): Binder
//        = recyclerViewBinding(requireOwner,view,list,itemViewLayoutId,fixedSize,layoutManager,dragAndDrop,bindView)
//
///**
// * D&D の動的サポート(Flow版）
// */
//fun <T> Binder.recyclerViewBinding(
//    view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    dragAndDrop: Flow<Boolean>,
//    bindView:(Binder, View, T)->Unit): Binder
//        = recyclerViewBinding(requireOwner,view,list,itemViewLayoutId,fixedSize,layoutManager,dragAndDrop, bindView)
//
///**
// * Gesture の静的サポート（ばらばらに指定する版）
// */
//fun <T> Binder.recyclerViewGestureBinding(
//    view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    dragToMove:Boolean, swipeToDelete:Boolean, deletionHandler:((T)-> RecyclerViewBinding.IDeletion)?,
//    bindView:(Binder, View, T)->Unit): Binder
//        = recyclerViewGestureBinding(requireOwner,view,list,itemViewLayoutId,fixedSize,layoutManager,dragToMove, swipeToDelete, deletionHandler,bindView)
//
///**
// * Gesture の静的サポート
// */
//fun <T> Binder.recyclerViewGestureBinding(
//    view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    gestureParams: RecyclerViewBinding.GestureParams<T>?,
//    bindView:(Binder, View, T)->Unit): Binder
//        = recyclerViewGestureBinding(requireOwner,view,list,itemViewLayoutId,fixedSize,layoutManager,gestureParams,bindView)
///**
// * Gesture の動的サポート(LiveData版）
// */
//fun <T> Binder.recyclerViewGestureBinding(
//    view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    gestureParams: LiveData<RecyclerViewBinding.GestureParams<T>?>,
//    bindView:(Binder, View, T)->Unit): Binder
//        = recyclerViewGestureBinding(requireOwner,view,list,itemViewLayoutId,fixedSize,layoutManager,gestureParams,bindView)
///**
// * Gesture の動的サポート(Flow版）
// */
//fun <T> Binder.recyclerViewGestureBinding(
//    view: RecyclerView, list: ObservableList<T>, itemViewLayoutId:Int,
//    fixedSize:Boolean = true,
//    gestureParams: Flow<RecyclerViewBinding.GestureParams<T>?>,
//    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(view.context),
//    bindView:(Binder, View, T)->Unit): Binder
//        = recyclerViewGestureBinding(requireOwner,view,list,itemViewLayoutId,fixedSize,gestureParams,layoutManager,bindView)
//
//// endregion