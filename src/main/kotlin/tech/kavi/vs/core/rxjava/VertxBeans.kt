package tech.kavi.vs.core.rxjava


import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.rxjava.core.Vertx
import tech.kavi.vs.core.VertxBeansBase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.function.Consumer

@Configuration
open class VertxBeans : VertxBeansBase() {

    @Bean
    @Throws(Throwable::class)
    open fun vertx(options: VertxOptions) = options.let {
        when {
            it.eventBusOptions.isClustered -> clusteredVertx(it)
            else -> Vertx.vertx(it)
        }
    }

    @Bean(name = ["config"])
    fun config(): JsonObject = config

    @Bean
    open fun eventBus(vertx: Vertx) = vertx.eventBus()

    @Bean
    open fun fileSystem(vertx: Vertx) = vertx.fileSystem()

    @Bean
    open fun sharedData(vertx: Vertx) = vertx.sharedData()

    @Bean
    open fun contextRunner(vertx: Vertx): ContextRunner {
        return ContextRunnerImpl(tech.kavi.vs.core.ContextRunnerImpl(vertx.delegate as io.vertx.core.Vertx))
    }

    @Throws(Throwable::class)
    private fun clusteredVertx(options: VertxOptions): Vertx {
        return clusteredVertx(Consumer{ handler -> Vertx.clusteredVertx(options, handler) })
    }

}