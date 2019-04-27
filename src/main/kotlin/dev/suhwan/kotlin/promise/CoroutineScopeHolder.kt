package dev.suhwan.kotlin.promise

import kotlinx.coroutines.CoroutineScope

object CoroutineScopeHolder {
    private var scope: CoroutineScope? = null

    fun getCoroutineScope(): CoroutineScope? {
        return this.scope
    }

    fun setCoroutineScope(coroutineScope: CoroutineScope?) {
        this.scope = coroutineScope
    }
}
