// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

// This is just a test to verify, that we can compile @BuilderInference lambdas with JVM_IR

import kotlin.coroutines.*

interface CoroutineScope

interface Flow<out T>  {
    suspend fun collect(collector: FlowCollector<T>)
}

interface SendChannel<in T> {
    suspend fun send(t: T)
}

interface ProducerScope<in T>: CoroutineScope, SendChannel<T> {
    val channel: SendChannel<T>
}

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

interface ReceiveChannel<out E>

suspend inline fun <T> Flow<T>.collect(crossinline action: suspend (value: T) -> Unit): Unit =
    collect(object : FlowCollector<T> {
        override suspend fun emit(value: T) = action(value)
    })

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
fun <E> CoroutineScope.produce(
    context: CoroutineContext = EmptyCoroutineContext,
    capacity: Int = 0,
    @BuilderInference block: suspend ProducerScope<E>.() -> Unit
): ReceiveChannel<E> = object : ReceiveChannel<E> {}

private fun CoroutineScope.asChannel(flow: Flow<*>): ReceiveChannel<Any> = produce {
    flow.collect { value ->
        return@collect channel.send(value ?: "NULL")
    }
}

fun box(): String {
    (object: CoroutineScope {}).asChannel(object: Flow<String> {
        override suspend fun collect(whatever: FlowCollector<String>): Unit = TODO()
    })
    return "OK"
}