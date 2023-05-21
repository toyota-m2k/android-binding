package io.github.toyota32k.binder

// 試行錯誤したが、
// UnitTestでは、ValueAnimator が正しく動かないっぽい。
// runBlockingなどによって、UIスレッド（＝テスト実行スレッド）が止まるため、Animationの実行も止まってしまうのではないかと推測。
// Animationをサブスレッドで実行できればよいのだが、ValueAnimator.start で MainThreadでなければ例外を投げて終了してしまうので不可。
// テスト実行スレッド以外をメインスレッドと偽る方法がわかればなんとかなるかも。。。

//import android.animation.Animator
//import android.animation.ValueAnimator
//import androidx.arch.core.executor.testing.InstantTaskExecutorRule
//import io.github.toyota32k.binder.anim.IReversibleAnimation
//import kotlinx.coroutines.*
//import kotlinx.coroutines.test.TestCoroutineDispatcher
//import kotlinx.coroutines.test.resetMain
//import kotlinx.coroutines.test.setMain
//import org.junit.After
//import org.junit.Before
//import org.junit.Rule
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.robolectric.RobolectricTestRunner
//import java.util.concurrent.Executors
//import kotlin.coroutines.Continuation
//import kotlin.coroutines.resume
//import kotlin.coroutines.suspendCoroutine
//
//
//@RunWith(RobolectricTestRunner::class)
//class AnimationTest {
//    val logger = IReversibleAnimation.logger
//
//    @Rule
//    @JvmField
//    val instantExecutorRule : InstantTaskExecutorRule = InstantTaskExecutorRule()
//    private val mainThreadSurrogate = // TestCoroutineDispatcher()
//        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
//
//    @ExperimentalCoroutinesApi
//    @Before
//    fun setup() {
//        Dispatchers.setMain(mainThreadSurrogate)
//    }
//
//    @ExperimentalCoroutinesApi
//    @After
//    fun tearDown() {
//        Dispatchers.resetMain()
//    }
//
//    class RefAnim : Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
//        var continuation:Continuation<Boolean>? = null
//        val logger = IReversibleAnimation.logger
//        val animator = ValueAnimator.ofFloat(0f, 100f).also {
//            it.duration = 10000L
//            it.addUpdateListener(this )
//            it.addListener(this)
//        }
//
//        suspend fun start() : Boolean {
//            return suspendCoroutine<Boolean> {
//                continuation = it
//                animator.start()
//            }
//        }
//
//        override fun onAnimationUpdate(animation: ValueAnimator?) {
//            logger.debug("${animation?.animatedValue as? Float}")
//        }
//
//        override fun onAnimationStart(animation: Animator?) {
//            logger.debug()
//        }
//
//        override fun onAnimationEnd(animation: Animator?) {
//            logger.debug()
//            continuation?.resume(true)
//        }
//
//        override fun onAnimationCancel(animation: Animator?) {
//            logger.debug()
//        }
//
//        override fun onAnimationRepeat(animation: Animator?) {
//            logger.debug()
//        }
//
//
//
//    }
//
//    @Test
//    fun valueAnimationSimpleTest() {
//        runBlocking {
//            withContext(Dispatchers.Main) {
//                val ani = RefAnim()
//                ani.start()
//                logger.debug("animation end")
//            }
//            logger.debug("withContext end")
//        }
//        logger.debug("done")
//
//
////        runBlocking {
////            delay(1000)
////        }
////
////        var value:Float = -100f
////        val anim = ReversibleValueAnimation(1000).onUpdate {
////            value=it*200 - 100
////            IReversibleAnimation.logger.debug("value=$value")
////        }
////
////        runBlocking {
////            anim.run(false)
////            Assert.assertEquals(100f,value)
////            anim.run(true)
////            Assert.assertEquals(-100f,value)
////        }
//    }
//}