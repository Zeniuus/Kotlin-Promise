package dev.suhwan.kotlin.promise

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class Promise(
    private val block: (suspend ((Any) -> Unit, (Any) -> Unit) -> Unit)?
) {
    private constructor(
        parent: Promise,
        onResolvedBlock: ((Any) -> Any)? = null,
        onRejectedBlock: ((Any) -> Any)? = null
    ) : this(null) {
        this.onResolvedBlock = onResolvedBlock
        this.onRejectedBlock = onRejectedBlock
        parent.onComplete(this)
    }

    enum class Status {
        PENDING,
        RESOLVED,
        REJECTED
    }

    private sealed class Result {
        abstract val value: Any

        data class Success(override val value: Any) : Result()

        data class Failure(override val value: Any) : Result()
    }

    private val children = mutableListOf<Promise>()

    private var onResolvedBlock: ((Any) -> Any)? = null
    private var onRejectedBlock: ((Any) -> Any)? = null

    var status = Status.PENDING

    private lateinit var result: Result

    private val coroutineScope = CoroutineScopeHolder.getCoroutineScope()!!

    init {
        if (block != null) {
            executeAsCoroutine {
                try {
                    block.invoke(::resolve, ::reject)
                } catch (e: Throwable) {
                    reject(e)
                }
            }
        }
    }

    private fun resolve(value: Any) {
        if (!::result.isInitialized) {
            result = Result.Success(value)
            status = Status.RESOLVED

            children.forEach { child ->
                child.onParentResolved(value)
            }
        }
    }

    private fun reject(value: Any) {
        if (!::result.isInitialized) {
            result = Result.Failure(value)
            status = Status.REJECTED

            children.forEach { child ->
                child.onParentRejected(value)
            }
        }
    }

    private fun onParentResolved(value: Any) {
        executeAsCoroutine {
            if (onResolvedBlock == null) {
                resolve(value)
            } else {
                try {
                    val result = onResolvedBlock!!.invoke(value)
                    resolve(result)
                } catch (e: Throwable) {
                    reject(e)
                }
            }
        }
    }

    private fun onParentRejected(value: Any) {
        executeAsCoroutine{
            if (onRejectedBlock == null) {
                reject(value)
            } else {
                try {
                    val result = onRejectedBlock!!.invoke(value)
                    resolve(result)
                } catch (e: Throwable) {
                    reject(e)
                }
            }
        }
    }

    fun then(onResolvedBlock: (Any) -> Any, onRejectedBlock: ((Any) -> Any)? = null): Promise {
        return Promise(this, onResolvedBlock, onRejectedBlock)
    }

    fun catch(onRejectedBlock: (Any) -> Any): Promise {
        return Promise(this, null, onRejectedBlock)
    }

    private fun onComplete(promise: Promise) {
        children.add(promise)
        when (status) {
            Status.PENDING -> {}
            Status.RESOLVED -> {
                promise.onParentResolved(result.value)
            }
            Status.REJECTED -> {
                promise.onParentRejected(result.value)
            }
        }
    }

    private fun executeAsCoroutine(block: suspend (CoroutineScope) -> Any) {
        coroutineScope.launch {
            block(this)
        }
    }

    companion object {
        fun resolve(value: Any): Promise {
            return Promise { resolve, _ ->
                resolve(value)
            }
        }

        fun reject(value: Any): Promise {
            return Promise { _, reject ->
                reject(value)
            }
        }
    }
}
