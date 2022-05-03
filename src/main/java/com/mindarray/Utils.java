package com.mindarray;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    //validation
   public static JsonObject verifyCredential(JsonObject credentials) {
       var error = new ArrayList<String>();
       if(!credentials.containsKey("IP_Address")){
           error.add("IP Address Not Found");
       }
        if(!credentials.containsKey("Metric_Type")){
            error.add("Metric_Type not found");
        } else{
            if(credentials.getString("Metric_Type").equals("linux")){
                if(!credentials.containsKey("username"))
                {
                   error.add("username not found");
                }else if(!credentials.containsKey("password")){
                    error.add("password column not found");
                }
            }
            else if(Objects.equals(credentials.getString("Metric_Type"), "windows")){
                if(!credentials.containsKey("username"))
                {
                    error.add("Username not found");
                }
                if(!credentials.containsKey("password")){
                    error.add("Password not found");
                }
            }
            else if(Objects.equals(credentials.getString("Metric_Type"), "network")){
                if(!credentials.containsKey("version"))
                {
                   error.add("Version not found");

                }
                if(!credentials.containsKey("community")){
                    error.add("Community not found");
                }
            }
        }
       LOGGER.info(String.valueOf(error));
        if(error.isEmpty()){
            credentials.put("Status","Successful");
        }else{
            credentials.put("Status","Unsuccessful");
            credentials.put("Error",error);
        }

       return credentials;
    }
    public static JsonObject checkAvailability(String ipAddress)  {
       var result = new JsonObject();
       var error = new ArrayList<String>();
        List<String> command = new ArrayList<>();
        command.add(0,"fping");
        command.add(1,"-c");
        command.add(2,"3");
        command.add(3,"-t");
        command.add(4,"1000");
        command.add(5,"-q");
        command.add(ipAddress);

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command);
        builder.redirectErrorStream(true);
        Process process= null;
        try {
            process = builder.start();
        } catch (IOException e) {
            error.add(e.getMessage());
            LOGGER.info("Unable to start builder for discovery");
        }
        assert process != null;
        var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line=null ;
        while (true){
            try {
                if ((line = reader.readLine()) == null)
                    line = reader.readLine();
                break;
            } catch (IOException e) {
                error.add(e.getMessage());
                LOGGER.info("Unable to read line");
            }finally {
               process.destroy();
               try {
                   reader.close();
               } catch (Exception e) {
                   LOGGER.error(e.getMessage());
               }
            }
        }
        if(line == null){
           result.put("Status","Unsucessful");
           result.put("Error",error);
        }
        LOGGER.info(line);
        var pattern = Pattern.compile("%[a-zA-Z]+ = [0-9]\\/[0-9]\\/[0-9]%");
        var matcher = pattern.matcher(line);
        //System.out.println(matcher.group());
        if(matcher.find()){
            String match = matcher.group(0);
            var pattern1= Pattern.compile("\\/[0-9]%");
            var matcher1 = pattern1.matcher(match);
            if(matcher1.find()){
                if((matcher1.group(0).split("/")[1].split("%")[0]).equals("0")){
                    result.put("Status","Successful");
                };
            }else{
               result.put("Status","Unsuccessful");
            }
        }else{
            LOGGER.info("Discovery Match not found");
            result.put("Status","Unsuccessful");
        }
       return result;
    }
   public static JsonObject spawnProcess(JsonObject credential) {
       var error = new ArrayList<String>();
       //call plugin.exe
       String encoder = (Base64.getEncoder().encodeToString((credential).toString().getBytes(StandardCharsets.UTF_8)));
       BufferedReader reader = null;
       Process process = null;
       try {
            process = new ProcessBuilder("Plugin.exe", encoder).start();
           reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
           String line;
           while ((line = reader.readLine()) != null) {
               var jsonObject = new JsonObject(line);
               credential.put("Result", jsonObject);
           }
       } catch (Exception e) {
            error.add(e.getMessage());
       } finally {
           if (reader != null) {
               try{
                   reader.close();
               }catch (Exception e){
                   error.add(e.getMessage());
               }
               process.destroy();
           }
       }
       if(error.isEmpty()){
           credential.put("Status","Successful");
       }else{
           credential.put("Status","Unsuccessful");
       }
       return credential;
   }
   public static JsonObject checkPort(JsonObject credential){
       var error = new ArrayList<String>();
       if(!(credential.containsKey("Port"))){
           switch (credential.getString("Metric_Type")){
               case "linux" :credential.put("Port",Constant.SSH_PORT);
                             break;
               case "windows" : credential.put("Port",Constant.WINRM_PORT);
                               break;
               case "network" : credential.put("Port",Constant.SNMP_PORT);
                                break;
               default:  error.add("Wrong Metric Group given");
           }
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
