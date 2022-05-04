package com.mindarray;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;

import static com.mindarray.Constant.*;

public class DatabaseEngine extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseEngine.class);
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var eventBus = vertx.eventBus();
       vertx.<JsonObject>executeBlocking( handler ->{
           var data = verifyDatabase();
           data.onComplete( databseHandler ->{
               LOGGER.debug(String.valueOf(data.result()));
               handler.complete( data.result());
           });
       }).onComplete( completeHandler ->{
           if(completeHandler.result().getString(STATUS).equals(SUCCESSFULL)){
               LOGGER.info("Initial Check Done for Database");
               eventBus.<JsonObject>localConsumer(DISCOVERY_DATABASE_CHECK_PING,checkHandler->{
                   vertx.executeBlocking(blockingHandler ->{
                             blockingHandler.complete(checkIP(checkHandler.body()));
                   }).onComplete(checkIPContext ->{
                       checkHandler.reply(checkIPContext.result());
                   });
               });
               eventBus.<JsonObject>localConsumer(DISCOVERY_DATABASE_ADD_DATA, addHandler ->{
                   vertx.<JsonObject>executeBlocking(blockingHandler ->{
                       var result = insertCredentials(addHandler.body());
                       blockingHandler.complete(result);
                   }).onComplete( dataCompleteHandler ->{
                       addHandler.reply(dataCompleteHandler.result());
                   });
               });

           }else{
               LOGGER.info("Initial Check fail for the Database");
           }
       });

        startPromise.complete();
    }
    private  Connection connect() {
        Connection connection = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "vedant.dokania", "Mind@123");
            LOGGER.info("Database Connection Successful");
        } catch (Exception e) {
            LOGGER.error("Exception Occured :" + e.getMessage());
        }
        return connection;
    }
    private JsonObject getCredentials(JsonObject credential){
        var connection = connect(); //To connect with database
        var credentials = new JsonObject();
        try {
            var statement = connection.createStatement();
            statement.execute("use nms");
            String query = "select poller.Credential_id , poller.IP_address ,poller.Metric_type ,poller.Metric_group,poller.Scheduled_time, poller.Group_status, monitor.username,monitor.password,monitor.port,monitor.community,monitor.version from monitor, poller where monitor.IP_address = poller.Ip_address and monitor.IP_address=\""+credential.getString(IP_ADDRESS)+"\";";
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()){
                var data = new JsonObject();
                data.put(IP_ADDRESS,resultSet.getString(2));
                data.put(METRIC_TYPE,resultSet.getString(3));
                data.put(METRIC_GROUP,resultSet.getString(4));
                data.put("Scheduled_Time", resultSet.getInt(5));
                data.put("Group_status",resultSet.getString(6));
                if(data.getString(METRIC_TYPE).equals("linux") || data.getString(METRIC_TYPE).equals("windows")){
                    data.put(USERNAME,resultSet.getString(7));
                    data.put(PASSWORD,resultSet.getString(8));
                    data.put(PORT, resultSet.getInt(9));
                }else{
                    data.put(PORT, resultSet.getInt(9));
                    data.put(COMMUNITY,resultSet.getString(10));
                    data.put(VERSION, resultSet.getString(11));
                }
                credentials.put(resultSet.getString(1),data );
            }
        } catch (SQLException e) {
            LOGGER.error(e.getCause().getMessage());
        }finally {
            try {
                connection.close();
            }catch (Exception e){
                LOGGER.info(e.getMessage());
            }
        }
        return credentials;
    }
    private String generate() {
        SecureRandom random = new SecureRandom();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        byte[] buffer = new byte[20];
        random.nextBytes(buffer);
        return encoder.encodeToString(buffer);
    }
    private JsonObject insertCredentials(JsonObject credential){
        var error = new ArrayList<String>();
        try(var con = connect();){
            var statement = con.createStatement();
            statement.execute("use nms;");
            String query ;
            if(!(credential.getString(METRIC_TYPE).equals("network"))){
                 query = INSERT.replace("table","monitor").
                        replace("columns","(IP_address,username,Password,Metric_type,Port) values (").
                        replace("data","\""+credential.getString(IP_ADDRESS)+"\",\""+credential.getString(USERNAME)
                                +"\",\""+credential.getString(PASSWORD)+"\",\""+credential.getString(METRIC_TYPE)+"\","+credential.getString(PORT)+")");
            }else{
                query = INSERT.replace("table","monitor").
                        replace("columns","(IP_address,version,community,Metric_type,Port) values (").
                        replace("data","\""+credential.getString(IP_ADDRESS)+"\",\""+credential.getString(VERSION)
                                +"\",\""+credential.getString(COMMUNITY)+"\",\""+credential.getString(METRIC_TYPE)+"\","+credential.getString(PORT)+")");
            }
            statement.execute(query);
            ResultSet rs = statement.executeQuery("Select Metric_group ,time from metric where Metric_type="+"\"" +credential.getString(METRIC_TYPE)+"\";");
            ArrayList<String> queries= new ArrayList<>();
            while(rs.next()){
                String value =("insert into poller values(\""+generate()+"\",\""+credential.getString(IP_ADDRESS)+"\",\""+
                        credential.getString(METRIC_TYPE)+"\",\"" +rs.getString(1)+"\","+rs.getInt(2)+",\"enable\");" );
                queries.add(value);
            }
            queries.forEach( msg ->{
                try {
                    statement.execute(msg);
                } catch (SQLException e) {
                    error.add(e.getMessage());
                    LOGGER.debug("Error in insertion in poller table");
                }
            });

        }catch (Exception e){
            error.add(e.getMessage());
            LOGGER.info("Exception Occurred :"+e.getMessage());
        }
        if(!(error.isEmpty())){
            credential.put(STATUS, UNSUCCESSFULL);
            credential.put(ERROR,error);
        }else{
            credential.put(STATUS, SUCCESSFULL);
        }

   return credential;
    }
    private Future<JsonObject> verifyDatabase(){
       Promise<JsonObject> promise = Promise.promise();
        var result = new JsonObject();
        var conn = connect();
        var error = new ArrayList<String>();
        String databaseCheck = "show databases like" + " \"nms\";" ;
        try {
            var stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(databaseCheck);
            String value = null;
            while(resultSet.next()){
                value = resultSet.getString(1);
            }
            if(value.equals("nms")){
                LOGGER.info("Database already exists");
            }
            else{
                String createDatabase = "create database nms;" ;
                String createMonitor ="create table monitor (IP_address varchar(50), Metric_type varchar(90), username varchar(90), password varchar(90), port int , community varchar(90) , version varchar(90),Primary key (IP_address) );";
                String createMetric ="create table metric (Metric_type varchar(90), Metric_group varchar(90), time int);";
                String createPoll ="create table poller( Credential_id varchar(100),IP_address varchar(50),Metric_type varchar(90),Metric_group varchar(90),Scheduled_time int, Group_status varchar(20), primary key(Credential_id));";
                var flag= stmt.execute(createDatabase);
                if(flag){
                    LOGGER.info("Database created successfully");
                    stmt.executeQuery("use nms");
                    stmt.executeQuery(createMonitor);
                    stmt.executeQuery(createMetric);
                    stmt.executeQuery(createPoll);
                    stmt.executeQuery("insert into metric values(\"linux\",\"CPU\",5000);");
                    stmt.executeQuery("insert into metric values(\"linux\",\"Disk\",6000);");
                    stmt.executeQuery("insert into metric values(\"linux\",\"Memory\",8000);");
                    stmt.executeQuery("insert into metric values(\"linux\",\"Process\",5000);");
                    stmt.executeQuery("insert into metric values(\"linux\",\"System\",9000);");
                    stmt.executeQuery("insert into metric values(\"windows\",\"CPU\",5000);");
                    stmt.executeQuery("insert into metric values(\"windows\",\"Disk\",6000);");
                    stmt.executeQuery("insert into metric values(\"windows\",\"Memory\",8000);");
                    stmt.executeQuery("insert into metric values(\"windows\",\"Process\",5000);");
                    stmt.executeQuery("insert into metric values(\"windows\",\"System\",9000);");
                }else{
                    LOGGER.info("Unable to create database");
                }
            }
        }catch (Exception e)
        {
            error.add(e.getMessage());
            LOGGER.error("Error:" + e.getCause().getMessage());
        }finally {
            try {
                conn.close();
            }catch (Exception e){
                error.add(e.getMessage());
            }
        }
        if(error.isEmpty()){
            result.put(STATUS, SUCCESSFULL);
        }else{
            result.put(STATUS, UNSUCCESSFULL);
            result.put(ERROR,error);
        }
        promise.complete(result);
        return promise.future();
    }
    private JsonObject checkIP(JsonObject credential){
        var error = new ArrayList<String>();
        try ( var connect = connect()){
            var statement = connect.createStatement();
            String available ="Select exists(Select * from monitor where IP_address= "+"\""+credential.getString(IP_ADDRESS)+"\");";
                statement.execute("use nms;");
                ResultSet resultSet = statement.executeQuery(available);
                int value = 0;
                while(resultSet.next()){
                    value = resultSet.getInt(1);
                }
                if(value==1){
                    error.add("IP already Discovered");
                    LOGGER.info("IP already discovered");
                }else{
                    LOGGER.info("IP checked not available in database");
                }
        }catch (Exception e){
            error.add(e.getMessage());
        }
        if(error.isEmpty()){
            credential.put(STATUS, SUCCESSFULL);
        }else{
            credential.put(STATUS, UNSUCCESSFULL);
            credential.put(ERROR,error);
        }
        return credential;
    }

}
