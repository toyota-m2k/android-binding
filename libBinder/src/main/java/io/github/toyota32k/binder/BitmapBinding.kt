package io.github.toyota32k.binder

import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.utils.android.RefBitmap
import io.github.toyota32k.utils.android.RefBitmapFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BitmapBinding(refBitmapFlow: Flow<RefBitmap?>) : BaseFlowBinding<RefBitmap?>(BindingMode.OneWay) {
    override val data: Flow<RefBitmap?> = refBitmapFlow

    private val imageView: ImageView?
        get() = view as ImageView?

    fun connect(owner: LifecycleOwner, view: ImageView) {
        super.connect(owner,view)
    }

    override fun onDataChanged(v: RefBitmap?) {
        val bitmap = v?.takeIf { it.hasBitmap }?.bitmap
        imageView?.setImageBitmap(bitmap)
    }
}

fun Binder.bitmapBinding(view:ImageView, refBitmapFlow: Flow<RefBitmap?>) = apply {
    add(BitmapBinding(refBitmapFlow).apply {
        connect(requireOwner, view)
    })
}