package com.mindarray;

public class Constant {
    //TODO Common eventbus notation requestVerticle.ConsumerVerticle.Purpose
    //Database Constant
    public static final String SELECT = "Select column from table where condition";
    public static final String INSERT = "insert into table columns data ;";

    //Port Constants
    public static final int HTTP_PORT = 8080;
    public static final int SSH_PORT =22;
    public static final  int WINRM_PORT =5985;
    public static final int SNMP_PORT =161;

    //Event Bus Constants
    public static final String DISCOVERY_ENDPOINT = "/Discovery";
    public static final String API_SERVER_DISCOVERY_DISCOVERY_ADDRESS = "Discovery";
    public static  final String DISCOVERY_DATABASE_CHECK_PING = "CheckPing";
    public static  final String DISCOVERY_DATABASE_ADD_DATA = "AddData";
    public static final String HEADER_TYPE = "application/json";

    
    
    // Json constants
    public static final String IP_ADDRESS= "ip.address";
    public static final String METRIC_GROUP ="metric.group";
    public static final String PORT = "port" ;
    public static final String METRIC_TYPE ="metric.type";
    public static  final String COMMUNITY = "community";
    public static final String VERSION ="version";
    public static final String USERNAME ="username" ;
    public static final String PASSWORD ="password";
    public static final String STATUS ="status" ;
    public static final String ERROR ="error";
    
    
    // Constant Messages
    
    public static final String SUCCESSFULL ="successful" ;

    public static final String UNSUCCESSFULL ="unsuccessful" ;


 }
