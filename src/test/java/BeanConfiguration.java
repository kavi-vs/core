import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.springframework.context.annotation.Bean;
import tech.kavi.vs.core.VertxBeans;


public class BeanConfiguration extends VertxBeans {
    @Bean
    public JsonObject router(Vertx vertx){

        System.out.println("--------------");
        System.out.println(vertx.isClustered());
        return  new JsonObject();
    }
}
