package com.mindarray;
import static com.mindarray.Constant.*;
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
    public void start(Promise<Void> startPromise) {
      var eventBus = vertx.eventBus();
        final Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
         router.route().method(HttpMethod.POST).path(Constant.DISCOVERY_ENDPOINT).handler(handler ->{
             HttpServerResponse response = handler.response();
             try{
                 var credentials = handler.getBodyAsJson();
                 eventBus.<JsonObject>request(Constant.API_SERVER_DISCOVERY_DISCOVERY_ADDRESS, credentials, context -> {
                     if (context.succeeded()) {
                         if (context.result().body().getString(STATUS).equals(SUCCESSFULL)) {
                             response.setStatusCode(200).putHeader("content-type", Constant.HEADER_TYPE).end(
                                     new JsonObject().put("message", SUCCESSFULL).encodePrettily()
                             );
                         } else {
                             response.setStatusCode(200).putHeader("content-type", Constant.HEADER_TYPE);
                             var result = new JsonObject().put("message", UNSUCCESSFULL).put(ERROR, context.result().body().getValue(ERROR));
                             response.end(result.encodePrettily());

                         }
                     }
                 });
             }catch (Exception e){
                 response.setStatusCode(200).putHeader("content-type", Constant.HEADER_TYPE).end(
                         new JsonObject().put("message", "Wrong Json Format").put(ERROR,e.getMessage()).encodePrettily()
                 );

             }


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
