package tech.kavi.vs.core

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.EventBusOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.metrics.MetricsOptions
import io.vertx.core.spi.cluster.ClusterManager
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import java.io.*
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.util.Collections.list
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import java.util.function.Consumer
import java.util.concurrent.TimeUnit.MINUTES

/**
 * 基础配置参数
 * @author sili | 2019-02-01
 */
open class VertxBeansBase {

    open val environment: Environment? = null

    open val CONFIG_PATH = "config.json"

    protected var config: JsonObject = config(CONFIG_PATH)

    private val defaultAddress: String = try {
         list<NetworkInterface>(NetworkInterface.getNetworkInterfaces()).stream()
                .flatMap{list<InetAddress>(it.inetAddresses).stream() }
                .filter{!it.isAnyLocalAddress}
                .filter{!it.isMulticastAddress}
                .filter{!it.isLoopbackAddress}
                .filter{it !is Inet6Address}
                .map(InetAddress::getHostAddress)
                .findFirst().orElse("0.0.0.0")
    } catch (e: SocketException) {
        log.warn("Unable to determine network interfaces. Using \"localhost\" as host address.", e)
        "0.0.0.0"
    }

    @Bean
    protected open fun vertxOptions(): VertxOptions {
        val vertxOptionsJson = config.value<JsonObject>("VertxOptions")
        val options = vertxOptionsJson.let {
            if (it != null) log.info(it)
            when(it) {
                null -> VertxOptions()
                else -> VertxOptions(it)
            }
        }
        environment?.also { env ->
            env.getProperty("vertx.warning-exception-time", Long::class.java)?.let{options.warningExceptionTime = it}
            env.getProperty("vertx.event-loop-pool-size", Int::class.java)?.let{ options.eventLoopPoolSize = it }
            env.getProperty("vertx.max-event-loop-execution-time", Long::class.java)?.let{ options.maxEventLoopExecuteTime = it }
            env.getProperty("vertx.worker-pool-size", Int::class.java)?.let{options.workerPoolSize = it }
            env.getProperty("vertx.max-worker-execution-time", Long::class.java)?.let{ options.maxWorkerExecuteTime = it }
            env.getProperty("vertx.blocked-thread-check-interval", Long::class.java)?.let{ options.blockedThreadCheckInterval = it }
            env.getProperty("vertx.internal-blocking-pool-size", Int::class.java)?.let{ options.internalBlockingPoolSize = it }
            env.getProperty("vertx.ha-group", "").let{ options.haGroup = it }
            env.getProperty("vertx.quorum-size", Int::class.java)?.let{ options.quorumSize = it }
            env.getProperty("vertx.cluster-port", Int::class.java)?.let{ options.eventBusOptions.port = it }
            env.getProperty("vertx.cluster-ping-interval", Long::class.java)?.let{ options.eventBusOptions.clusterPingInterval = it }
            env.getProperty("vertx.cluster-ping-reply-interval", Long::class.java)?.let{ options.eventBusOptions.clusterPingReplyInterval = it }
            env.getProperty("vertx.cluster-public-host", String::class.java)?.let{ options.eventBusOptions.clusterPublicHost = it }
            env.getProperty("vertx.cluster-public-port", Int::class.java)?.let{ options.eventBusOptions.clusterPublicPort = it }

            options.eventBusOptions.isClustered = env.getProperty("vertx.clustered", Boolean::class.java, options.eventBusOptions.isClustered )
            options.eventBusOptions.host = env.getProperty("vertx.cluster-host", if (vertxOptionsJson?.containsKey("clusterHost") == true) options.eventBusOptions.host else defaultAddress)
            options.isHAEnabled = env.getProperty("vertx.ha-enabled", Boolean::class.java, options.isHAEnabled )
        }
        if (options.eventBusOptions.isClustered) {
            clusterManager(config)?.also { options.clusterManager = it }
        }
        eventBusOptions(config)?.also { options.eventBusOptions = it }
        metricsOptions(config)?.also { options.metricsOptions = it }

        return vertxOptions(options, vertxOptionsJson)
    }

    /**
     * custom vertxOption
     * */
    open fun vertxOptions(options: VertxOptions, vertxOptionsJson: JsonObject?): VertxOptions {
        return options
    }

    open fun clusterManager(config: JsonObject): ClusterManager ?{
        return null
    }

    open fun eventBusOptions(config: JsonObject): EventBusOptions ?{
        return null
    }

    open fun metricsOptions(config: JsonObject): MetricsOptions ?{
        return null
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    protected fun <T> clusteredVertx(consumer: Consumer<Handler<AsyncResult<T>>>): T {
        val future = CompletableFuture<T>()
        clusteredVertx(consumer, Handler{
            if (it.succeeded()) {
                future.complete(it.result())
            } else {
                future.completeExceptionally(it.cause())
            }
        })
        return future.get(2, MINUTES)
    }

    private fun <T> clusteredVertx(consumer: Consumer<Handler<AsyncResult<T>>>, handler: Handler<AsyncResult<T>>) {
        consumer.accept(handler)
    }

    companion object {
        private val log = LoggerFactory.getLogger(VertxBeansBase::class.java)

        fun config(configPath: String): JsonObject {
            val localConfig = loadLocalConfig(configPath)
            val externalConfig = loadExternalConfig(localConfig.value<String>("projectName")?.let { "config.$it.json" }
                    ?: "config.json")
            return localConfig.mergeIn(externalConfig, true)
        }

        /**
         * 加载resources目录配置文件
         */
        private fun loadLocalConfig(configPath: String): JsonObject {
            return try {
                val stream = ClassLoader.getSystemResourceAsStream(configPath)
                if (stream === null) {
                    throw FileNotFoundException(configPath)
                }
                loadJsonFile(stream)
            } catch (e: Exception) {
                log.error(e)
                JsonObject()
            }
        }

        inline fun <reified T> JsonObject?.value(key: String): T? {
            if (this == null) return null
            if (!this.containsKey(key)) return null
            val value = this.getValue(key)
            return when(value){
                is T -> value
                else -> null
            }
        }

        inline fun <reified T> JsonObject?.value(key: String, default: T): T {
            return this.value<T>(key) ?: default
        }

        /**
         * 读取json文件并解析成json格式
         */
        @Throws(Exception::class)
        private fun loadJsonFile(stream: InputStream): JsonObject {
            return JsonObject(InputStreamReader(stream, StandardCharsets.UTF_8).useLines {
                it.joinToString("")
            })
        }

        /**
         * 加载外部扩展JSON文件
         */
        private fun loadExternalConfig(configPath: String) : JsonObject{
            return try {
                val stream = FileInputStream(System.getProperty("user.dir") + "/" + configPath)
                loadJsonFile(stream)
            } catch (e: Exception) {
                log.error(e)
                JsonObject()
            }
        }
    }
}
