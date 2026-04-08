/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozk.scheduler;

import com.google.gson.JsonObject;

/**
 *
 * @author alexo
 */
public class StackTracerCode {

    public String file;

    public int line;

    public String method;

    public String className;

    public StackTracerCode() {
    }

    public StackTracerCode(StackTraceElement element) {
        file = element.getFileName();
        line = element.getLineNumber();
        method = element.getMethodName();
        className = element.getClassName();
    }

    public StackTracerCode(JsonObject data) {
        file = data.get("file").getAsString();
        line = data.get("line").getAsNumber().intValue();
        method = data.get("method").getAsString();
        className = data.get("className").getAsString();
    }

    public StackTraceElement getAsStackTracerElement() {
        return new StackTraceElement(className, method, file, line);
    }

    public JsonObject toJson() {
        JsonObject data = new JsonObject();
        data.addProperty("file", file);
        data.addProperty("line", line);
        data.addProperty("method", method);
        data.addProperty("class", className);
        return data;
    }
}
