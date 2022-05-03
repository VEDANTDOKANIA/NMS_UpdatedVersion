package com.mindarray;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
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
         router.route().method(HttpMethod.POST).path(Constant.DISCOVERY_ENDPOINT).handler(handler ->{
             HttpServerResponse response = handler.response();
             var credentials = handler.getBodyAsJson();
             eventBus.<JsonObject>request(Constant.DISCOVERY_ADDRESS,credentials, context->{
                  if(context.succeeded()){
                      if(context.result().body().getString("Status").equals("Successful")){
                          response.setStatusCode(200).putHeader("content-type",Constant.HEADER_TYPE).end(
                                  new JsonObject().put("message","Successful").encodePrettily()
                          );
                      }else{
                          response.setStatusCode(200).putHeader("content-type",Constant.HEADER_TYPE);
                          var result = new JsonObject().put("message","Unsuccessful").put("Error",context.result().body().getValue("Error"));
                          response.end(result.encodePrettily());

                      }
                  }
             });
         });

        vertx.createHttpServer().requestHandler(router).exceptionHandler(exception -> {
            LOGGER.error("Exception Occurred" + ":" + exception.getCause().getMessage());
        }).listen(Constant.HTTP_PORT, http -> {
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
