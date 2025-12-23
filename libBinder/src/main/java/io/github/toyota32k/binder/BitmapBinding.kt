package io.github.toyota32k.binder

import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.utils.android.RefBitmap
import io.github.toyota32k.utils.android.RefBitmapFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BitmapBinding(val refBitmapFlow: RefBitmapFlow) : BaseFlowBinding<RefBitmap?>(BindingMode.OneWay) {
    override val mutableData: MutableStateFlow<RefBitmap?> get() = refBitmapFlow
    override val data: StateFlow<RefBitmap?> get() = refBitmapFlow

    private val imageView: ImageView?
        get() = view as ImageView?

    fun connect(owner: LifecycleOwner, view: ImageView) {
        super.connect(owner,view)
    }

    override fun onDataChanged(v: RefBitmap?) {
        imageView?.setImageBitmap(v?.bitmap)
    }
}

fun Binder.bitmapBinding(view:ImageView, refBitmapFlow: RefBitmapFlow) = apply {
    add(BitmapBinding(refBitmapFlow).apply {
        connect(requireOwner, view)
    })
}