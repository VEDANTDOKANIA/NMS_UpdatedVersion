package com.mindarray;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DatabaseEngine extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseEngine.class);
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
         var result = verifyDatabase();
         result.onSuccess(handler ->{
             LOGGER.info("Initial Check Done for Database");
         }).onComplete( completeHandler ->{

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
    private JsonObject getCredentials(JsonObject Credential){
        var connection = connect(); //To connect with database
        var credentials = new JsonObject();
        try {
            var statement = connection.createStatement();
            statement.execute("use nms");
            String query = "select poller.Credential_id , poller.IP_address ,poller.Metric_type ,poller.Metric_group,poller.Scheduled_time, poller.Group_status, monitor.username,monitor.password,monitor.port,monitor.community,monitor.version from monitor, poller where monitor.IP_address = poller.Ip_address and monitor.IP_address=\""+Credential.getString("IP_Address")+"\";";
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()){
                var data = new JsonObject();
                data.put("IP_Address",resultSet.getString(2));
                data.put("Metric_Type",resultSet.getString(3));
                data.put("Metric_Group",resultSet.getString(4));
                data.put("Scheduled_Time", resultSet.getInt(5));
                data.put("Group_status",resultSet.getString(6));
                if(data.getString("Metric_Type").equals("linux") || data.getString("Metric_Type").equals("windows")){
                    data.put("username",resultSet.getString(7));
                    data.put("password",resultSet.getString(8));
                    data.put("Port", resultSet.getInt(9));
                }else{
                    data.put("Port", resultSet.getInt(9));
                    data.put("community",resultSet.getString(10));
                    data.put("version", resultSet.getString(11));
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
            result.put("Status","Successful");
        }else{
            result.put("Status","Unsuccessful");
            result.put("Error",error);
        }
        promise.complete(result);
        return promise.future();
    }
    private JsonObject checkIP(JsonObject credential){
        var error = new ArrayList<String>();
        try ( var connect = connect()){
            var statement = connect.createStatement();
            String available ="Select exists(Select * from monitor where IP_address= "+"\""+credential.getString("IP_Address")+"\");";
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
                    LOGGER.info("IP not discovered");
                }
        }catch (Exception e){
            error.add(e.getMessage());
        }
        if(error.isEmpty()){
            credential.put("Status","Successful");
        }else{
            credential.put("Status","Unsuccessful");
            credential.put("Error",error);
        }
        return credential;
    }

}
