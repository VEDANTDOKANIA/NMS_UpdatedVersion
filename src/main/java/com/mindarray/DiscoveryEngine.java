package com.mindarray;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

public class DiscoveryEngine extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var  eventBus = vertx.eventBus();
        eventBus.<JsonObject>localConsumer(Constant.DISCOVERY_ADDRESS, handler->{
            var verification = Utils.verifyCredential(handler.body());
            if(verification.getString("Status").equals("Successful")){

            }else{
                handler.reply(verification);
            }
        });
        startPromise.complete();
    }
}
