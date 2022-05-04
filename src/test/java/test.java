import io.vertx.core.json.JsonObject;

import static com.mindarray.Constant.*;

public class test {
    public static void main(String[] args) {
        JsonObject j1 = new JsonObject();
       var j2 = new JsonObject(j1.toString());
        j1.put(USERNAME,"       vedant");
        j1.put(PASSWORD,"hi  ");
        var fieldname =j1.fieldNames();
        j1.forEach(field ->{
            j1.put(String.valueOf(field),j1.getString(String.valueOf(field)).trim());
        });
    }
}
