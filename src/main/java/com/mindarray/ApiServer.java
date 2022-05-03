package com.mindarray;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiServer extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiServer.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
      var eventBus = vertx.eventBus();
        final Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
         router.route().method(HttpMethod.POST).path(Constant.DISCOVERYENDPOINT).handler(handler ->{
             var credentials = handler.getBodyAsJson();
             eventBus.request(Constant.DISCOVERYADDRESS,credentials,context->{

             });
         });

        vertx.createHttpServer().requestHandler(router).exceptionHandler(exception -> {
            LOGGER.error("Exception Occurred" + ":" + exception.getCause().getMessage());
        }).listen(Constant.HTTPPORT, http -> {
            if (http.succeeded()) {
                startPromise.complete();
                LOGGER.info("HTTP server started");
            } else {
                startPromise.fail(http.cause());
                LOGGER.error("HTTP server not started :" + http.cause());
            }

        });
    }
}
