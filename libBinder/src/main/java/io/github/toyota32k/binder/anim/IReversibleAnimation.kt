package io.github.toyota32k.binder.anim

import io.github.toyota32k.utils.UtLog

/**
 * 逆戻し可能あアニメーションのi/f
 * - アニメーションの開始～完了まで待機可能(suspend)
 * - アニメーション中 (running==true) に、
 *   - 逆方向アニメーションを開始(run)すると、元のアニメーション動作を停止し、その時点から、逆向きにアニメーションする。
 *   - 順方向アニメーションを開始(run)すると、その要求はキャンセルされたものとして、進行中のアニメーションをそのまま継続する。
 */
interface IReversibleAnimation {
    /**
     * アニメーション方向
     * false: 順方向 / true : 逆方向
     */
    val reverse:Boolean

    /**
     * アニメーション時間：ミリ秒
     */
    val duration:Long

    /**
     * アニメーション中か？
     * true: アニメーション中
     */
    val running:Boolean

    /**
     * アニメーション開始
     * @param reverse   false:順方向 / true:逆方向
     * @return true:アニメーション完了 / false:アニメーションは実行されなかった、または、途中で中止された
     */
    suspend fun run(reverse:Boolean) : Boolean

    /**
     * 初期化のために、アニメーションせずに最終状態にする
     */
    fun invokeLastState(reverse:Boolean)

    companion object {
        val logger = UtLog("Anim", null, "io.github.toyota32k.")
    }
}
