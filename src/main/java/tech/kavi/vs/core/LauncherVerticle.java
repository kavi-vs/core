package tech.kavi.vs.core;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

@Component
public abstract class LauncherVerticle extends AbstractVerticle implements ApplicationListener {

    public static AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @Autowired
    protected Vertx vertx;

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        vertx.deployVerticle(this);
    }

    public static void launcher(Class<? extends AbstractVerticle> verticle){
        context.register(verticle);
        context.refresh();
    }
}
