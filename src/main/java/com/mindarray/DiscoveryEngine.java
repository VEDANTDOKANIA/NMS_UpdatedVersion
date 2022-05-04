package com.mindarray;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import static com.mindarray.Constant.DISCOVERY_DATABASE_ADD_DATA;
import static com.mindarray.Constant.*;

public class DiscoveryEngine extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var  eventBus = vertx.eventBus();
        eventBus.<JsonObject>localConsumer(Constant.API_SERVER_DISCOVERY_DISCOVERY_ADDRESS, handler->{
            var credentials = new JsonObject(handler.body().toString());
            var verification = Utils.verifyCredential(handler.body());
            if(verification.getString(STATUS).equals(SUCCESSFULL)){
                eventBus.<JsonObject>request(Constant.DISCOVERY_DATABASE_CHECK_PING,new JsonObject().put(IP_ADDRESS,handler.body().getString(IP_ADDRESS)), checkHandler ->{
                    if(checkHandler.succeeded()){
                        if(checkHandler.result().body().getString(STATUS).equals(SUCCESSFULL)){
                                    vertx.executeBlocking( blockingHandler ->{
                                        var availability =Utils.checkAvailability(handler.body().getString(IP_ADDRESS));
                                        if(availability.getString(STATUS).equals(SUCCESSFULL)){
                                             var portCheck = Utils.checkPort(handler.body());
                                             if(portCheck.getString(STATUS).equals(SUCCESSFULL)){
                                                  vertx.<JsonObject>executeBlocking( blockingHandler1 ->{
                                                      var result = Utils.spawnProcess(handler.body().put(PORT,portCheck.getInteger(PORT)).put("category","discovery"));
                                                      blockingHandler1.complete(result);
                                                  }).onComplete( completeHandler ->{
                                                      if(completeHandler.result().getString(STATUS).equals(SUCCESSFULL)){
                                                         if(completeHandler.result().getJsonObject("Result").getString(STATUS).equals(SUCCESSFULL)){
                                                             eventBus.<JsonObject>request(DISCOVERY_DATABASE_ADD_DATA,credentials.put(PORT,portCheck.getInteger(PORT)), addHandler ->{
                                                                 if(addHandler.succeeded()){
                                                                     handler.reply(addHandler.result().body());
                                                                 }else{
                                                                     handler.reply(new JsonObject().put(STATUS, SUCCESSFULL).put(ERROR,"Unable to get data from event bus"));
                                                                 }
                                                             });
                                                         }else{
                                                             handler.reply(completeHandler.result().getJsonObject("Result"));
                                                         }


                                                      }else{
                                                          handler.reply(checkHandler.result());
                                                      }
                                                  });
                                             }else{
                                                 handler.reply(portCheck);
                                             }
                                        }else{
                                            handler.reply(availability);
                                        }
                                    });
                        }else{
                            handler.reply(checkHandler.result().body());
                        }
                    }
                });
             }else{
                handler.reply(verification);
            }
        });
        startPromise.complete();
    }
}
