
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;



@Import({BeanConfiguration.class})
@ComponentScan("service")
@Component
public class Launch implements ApplicationListener {
    public static void main( String[] args ) { new AnnotationConfigApplicationContext(Launch.class); }

    @Autowired
    private Vertx vertx;


    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {

        System.out.println("------------------");
        System.out.println(vertx.isClustered());


    }

}
