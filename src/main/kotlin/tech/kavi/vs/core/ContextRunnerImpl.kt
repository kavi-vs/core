package tech.kavi.vs.core

import io.vertx.core.*
import io.vertx.core.logging.LoggerFactory

import java.util.ArrayList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

class ContextRunnerImpl(private val vertx: Vertx) : ContextRunner {
    override fun <T> execute(instances: Int, consumer: Consumer<Handler<AsyncResult<T>>>, resultHandler: Handler<AsyncResult<List<T>>>) {
        if (Vertx.currentContext() != null) {
            throw IllegalStateException("Already on a Vert.x thread!")
        }
        val collector = ResultCollector(instances, resultHandler)
        for (i in 0 until instances) {
            wrap(consumer).accept(Handler{ result ->
                if (result.succeeded()) {
                    collector.pushResult(result.result())
                } else {
                    resultHandler.handle(Future.failedFuture<List<T>>(result.cause()))
                }
            })
        }
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun <T> executeBlocking(instances: Int, consumer: Consumer<Handler<AsyncResult<T>>>, timeout: Long, unit: TimeUnit): List<T> {
        val future = CompletableFuture<List<T>>()
        execute(instances, consumer, Handler{ result ->
            if (result.succeeded()) {
                future.complete(result.result())
            } else {
                future.completeExceptionally(result.cause())
            }
        })
        return future.get(timeout, unit)
    }


    private fun <T> wrap(consumer: Consumer<Handler<AsyncResult<T>>>): Consumer<Handler<AsyncResult<T>>> {
        val context = vertx.orCreateContext
        return Consumer { resultHandler ->
            context.runOnContext {
                consumer.accept(Handler{ result ->
                    context.runOnContext { _ -> resultHandler.handle(result) }
                })
            }
        }
    }

    private class ResultCollector<T> constructor(private val count: Int, private val resultHandler: Handler<AsyncResult<List<T>>>) {
        private val results = ArrayList<T>()

        @Synchronized
        fun pushResult(result: T) {
            if (results.size == count) {
                log.warn("Your callback must supply one result, and only one result, to ContextRunner. (Trying to add {0}.)", result)
            } else {
                results.add(result)
                if (results.size == count) {
                    resultHandler.handle(Future.succeededFuture(results))
                }
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ContextRunnerImpl::class.java)
    }
}
