package com.mindarray;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstarp {

    public static final Vertx VERTX = Vertx.vertx();
    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstarp.class);

    public static void main(String[] args) {
        start(ApiServer.class.getName()).
                compose( handler -> start(DatabaseEngine.class.getName())).
                compose(handler -> start(DiscoveryEngine.class.getName())).
                compose(handler -> start(Poller.class.getName())).
                onComplete(
                        handler->{
                            if(handler.succeeded()){
                                LOGGER.info("All verticles Deployed");
                            }else{
                               LOGGER.error(handler.cause().getMessage());
                            }
                        }
                );


    }
    public static Future<Void> start(String verticle){
        Promise<Void> promise = Promise.promise();
        VERTX.deployVerticle(verticle,handler ->{
            if(handler.succeeded()){
                promise.complete();
            }else{
                promise.fail(handler.cause());
            }
        });
        return  promise.future();
    }
}
