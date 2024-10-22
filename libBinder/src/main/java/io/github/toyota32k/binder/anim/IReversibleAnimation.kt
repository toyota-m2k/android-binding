package io.github.toyota32k.binder.anim

import io.github.toyota32k.utils.UtLog
import kotlinx.coroutines.delay

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

    // region 単体利用のための API

    /**
     * value 0 --> 1
     */
    suspend fun advance() = run(false)

    /**
     * value 1 --> 0
     */
    suspend fun back() = run(true)

    /**
     * 行って戻る
     * アイコンを一定時間表示して、非表示に戻す、などの用途。
     * @param duration 行ってから、戻るまでの時間
     */
    suspend fun advanceAndBack(duration:Long) {
        advance()
        delay(duration)
        back()
    }

    /**
     * 行って戻る
     * @param reverseFirst  trueなら、戻って行く
     */
    suspend fun advanceAndBack(reverseFirst:Boolean, duration:Long) {
        if(reverseFirst) {
            back()
            delay(duration)
            advance()
        } else {
            advanceAndBack(duration)
        }
    }

    // endregion

    companion object {
        val logger = UtLog("Anim", null, "io.github.toyota32k.")
    }
}
