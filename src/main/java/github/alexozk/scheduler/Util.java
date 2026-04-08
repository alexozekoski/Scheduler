/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozk.scheduler;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author alexo
 */
public class Util {

    public static JsonElement toJson(Date date) {
        if (date == null) {
            return JsonNull.INSTANCE;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        return new JsonPrimitive(sdf.format(date));
    }
}
