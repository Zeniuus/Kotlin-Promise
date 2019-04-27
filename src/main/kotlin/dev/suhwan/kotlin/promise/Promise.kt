package dev.suhwan.kotlin.promise

class Promise(
    block: ((Any) -> Unit, (Any) -> Unit) -> Unit
) {
    private sealed class Result {
        abstract val value: Any

        data class Success(override val value: Any) : Result()

        data class Failure(override val value: Any) : Result()
    }

    private lateinit var result: Result

    init {
        try {
            block(::resolve, ::reject)
        } catch (e: Throwable) {
            result = Result.Failure(e)
        }
    }

    private fun resolve(value: Any) {
        if (!::result.isInitialized) {
            result = Result.Success(value)
        }
    }

    private fun reject(value: Any) {
        if (!::result.isInitialized) {
            result = Result.Failure(value)
        }
    }

    fun then(successBlock: (Any) -> Any, failureBlock: ((Any) -> Any)? = null): Promise {
        return when (result) {
            is Result.Success -> Promise.generatePromise(successBlock, result.value)
            is Result.Failure -> {
                if (failureBlock == null) {
                    Promise.reject(result.value)
                } else {
                    Promise.generatePromise(failureBlock, result.value)
                }
            }
        }
    }

    fun catch(block: (Any) -> Any): Promise {
        return when (result) {
            is Result.Success -> Promise.resolve(result.value)
            is Result.Failure -> Promise.generatePromise(block, result.value)
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

        private fun generatePromise(block: (Any) -> Any, value: Any): Promise {
            return try {
                val resultValue = block(value)
                Promise.resolve(resultValue)
            } catch (e: Throwable) {
                Promise.reject(e)
            }
        }
    }
}
