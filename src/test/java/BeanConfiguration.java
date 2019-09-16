import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.consul.ConsulClusterManager;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import tech.kavi.vs.core.VertxBeans;


public class BeanConfiguration extends VertxBeans {

    ClusterManager clusterManager = new ConsulClusterManager(new JsonObject());

    @Nullable
    @Override
    public ClusterManager getClusterManager() {
        System.out.println("ffffffffff");
        return clusterManager;
    }



    @Bean
    public JsonObject router(Vertx vertx){

        System.out.println("--------------");
        System.out.println(vertx.isClustered());
        return  new JsonObject();
    }
}
