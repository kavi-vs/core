package tech.kavi.vs.core.rxjava

import rx.Observable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Supplier

/**
 * 当使用VertxBeans而不是Verticles时，没有实例的概念。
 * 这些实用程序通过执行用户提供的回调提供了一种简单的复制实例的方法。
 * 在新的事件循环上。
 * 这些异步调用的结果将被整理并提供给客户机。
 * @author sili | 2019-02-01
 */
interface ContextRunner {
    /**
     * 在新的事件循环上执行用户提供的代码，并异步提供整理后的结果。
     *
     * @param instances 执行代码的次数（以及要使用的事件循环数）
     * @param supplier 在执行代码后提供“observable”；例如，“observable<httpserver>”`
     * @param <T> 正在创建的对象类型；例如'httpserver'`
     * @return 发出经过排序的“list”项，或者返回一个错误。
    </T> */
    fun <T> execute(instances: Int, supplier: Supplier<Observable<T>>): Observable<List<T>>

    /**
     * 在新的事件循环上执行用户提供的代码，并异步提供整理后的结果。
     *
     * @param instances 执行代码的次数（以及要使用的事件循环数）
     * @param supplier 在执行代码后提供“observable”；例如，“observable<httpserver>”`
     * @param timeout 超时时间
     * @param unit 超时单位
     * @param <T> 正在创建的对象类型；例如'httpserver'`
     * @return 排序的项目列表
     * @throws InterruptedException
     * @throws ExecutionException 代码引发的异常
     * @throws TimeoutException 所有实例都没有在超时内提供结果的异常
    </T> */
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun <T> executeBlocking(instances: Int, supplier: Supplier<Observable<T>>, timeout: Long, unit: TimeUnit): List<T>
}
