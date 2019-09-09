package tech.kavi.vs.core.rxjava

import io.vertx.core.Future
import io.vertx.core.Handler
import rx.Observable
import rx.Subscriber
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Consumer
import java.util.function.Supplier


class ContextRunnerImpl(private val delegate: tech.kavi.vs.core.ContextRunner) : ContextRunner {

    override fun <T> execute(instances: Int, supplier: Supplier<Observable<T>>): Observable<List<T>> {
        return Observable.create { subscriber -> doExecute(subscriber, instances, supplier) }
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun <T> executeBlocking(instances: Int, supplier: Supplier<Observable<T>>, timeout: Long, unit: TimeUnit): List<T> {
        return execute(instances, supplier).toBlocking().toFuture().get(timeout, unit)
    }

    private fun <T> doExecute(subscriber: Subscriber<in List<T>>, instances: Int, supplier: Supplier<Observable<T>>) {
        delegate.execute<T>(instances,
                Consumer{ resultHandler ->
                    supplier.get().subscribe(
                            { result -> resultHandler.handle(Future.succeededFuture(result)) },
                            { throwable -> resultHandler.handle(Future.failedFuture<T>(throwable)) })
                },
                Handler{ result ->
                    if (result.succeeded()) {
                        subscriber.onNext(result.result())
                        subscriber.onCompleted()
                    } else {
                        subscriber.onError(result.cause())
                    }
                })
    }
}
