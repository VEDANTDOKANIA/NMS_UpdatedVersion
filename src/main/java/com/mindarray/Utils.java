package com.mindarray;

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
import static com.mindarray.Constant.*;

public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    //validation
   public static JsonObject verifyCredential(JsonObject credentials) {
       var error = new ArrayList<String>();
       if(!credentials.containsKey(IP_ADDRESS)){
           error.add("IP Address Not Found");
       }
        if(!credentials.containsKey(METRIC_TYPE)){
            error.add("Metric_Type not found");
        } else{
            if(credentials.getString(METRIC_TYPE).equals("linux")){
                if(!credentials.containsKey(USERNAME))
                {
                   error.add("username not found");
                }else if(!credentials.containsKey(PASSWORD)){
                    error.add("password column not found");
                }
            }
            else if(Objects.equals(credentials.getString(METRIC_TYPE), "windows")){
                if(!credentials.containsKey(USERNAME))
                {
                    error.add("Username not found");
                }
                if(!credentials.containsKey(PASSWORD)){
                    error.add("Password not found");
                }
            }
            else if(Objects.equals(credentials.getString(METRIC_TYPE), "network")){
                if(!credentials.containsKey(VERSION))
                {
                   error.add("Version not found");

                }
                if(!credentials.containsKey(COMMUNITY)){
                    error.add("Community not found");
                }
            }
        }

        if(error.isEmpty()){
            credentials.put(STATUS, SUCCESSFULL);
        }else{
            credentials.put(STATUS, UNSUCCESSFULL);
            credentials.put(ERROR,error);
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
           error.add("Unable to ping the device");
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
                    result.put(STATUS, UNSUCCESSFULL);
                };
            }else{
               error.add("High Latency. Unable to capture all 3 packages");
            }
        }else{
            LOGGER.info("Unable to ping the device");
            error.add("Unable to ping the device");
        }
        if(error.isEmpty()){
            result.put(STATUS, SUCCESSFULL);
        }else{
            result.put(STATUS, UNSUCCESSFULL);
            result.put(ERROR,error);
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
            process = new ProcessBuilder("src/main/java/com/mindarray/plugin.exe", encoder).start();
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
           credential.put(STATUS, SUCCESSFULL);
       }else{
           credential.put(STATUS, UNSUCCESSFULL);
       }
       return credential;
   }
   public static JsonObject checkPort(JsonObject credential){
       var error = new ArrayList<String>();
       if(!(credential.containsKey(PORT))){
           switch (credential.getString(METRIC_TYPE)){
               case "linux" :credential.put(PORT,Constant.SSH_PORT);
                             break;
               case "windows" : credential.put(PORT,Constant.WINRM_PORT);
                               break;
               case "network" : credential.put(PORT,Constant.SNMP_PORT);
                                break;
               default:  error.add("Wrong Metric Group given");
           }
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
