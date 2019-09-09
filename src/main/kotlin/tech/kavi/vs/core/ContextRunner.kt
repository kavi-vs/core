package tech.kavi.vs.core

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

/**
 * 当使用VertxBeans而不是Verticles时，没有实例的概念。
 * 这些实用程序通过执行用户提供的回调提供了一种简单的复制实例的方法。
 * 在新的事件循环上。
 * 这些异步调用的结果将被整理并提供给客户机。
 * @author sili | 2019-02-01
 */
interface ContextRunner {
    /**
     * 执行用户提供的代码并异步提供整理后的结果。
     *
     * @param instances 执行代码的次数（以及要使用的事件循环数）
     * @param consumer 使用发送调用结果的“handler”
     * @param resultHandler 可接收排序结果的“handler”
     * @param <T> 正在创建的对象类型；例如'httpserver'`
    </T> */
    fun <T> execute(instances: Int, consumer: Consumer<Handler<AsyncResult<T>>>, resultHandler: Handler<AsyncResult<List<T>>>)

    /**
     * 执行用户提供的代码并同步提供整理后的结果。
     *
     * @param instances 执行代码的次数（以及要使用的事件循环数）
     * @param consumer 使用发送调用结果的“handler”
     * @param timeout 超时时间
     * @param unit 超时单位
     * @param <T> 正在创建的对象类型；例如'httpserver'`
     * @return 已排序的项目列表
     * @throws InterruptedException
     * @throws ExecutionException 代码引发的异常
     * @throws TimeoutException 所有实例都没有在超时内提供结果的异常
    </T> */
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun <T> executeBlocking(instances: Int, consumer: Consumer<Handler<AsyncResult<T>>>, timeout: Long, unit: TimeUnit): List<T>
}
