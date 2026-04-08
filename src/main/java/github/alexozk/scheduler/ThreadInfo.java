/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozk.scheduler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

/**
 *
 * @author alexo
 */
public class ThreadInfo implements Comparable<ThreadInfo> {

    protected long id;

    protected String name;

    protected String state;

    protected long timeNs;

    protected long timeMs;

    protected double percent;

    protected String lockName;

    protected String lockOwnerName;

    protected Long lockOwnerId;

    protected String[] lockSynchronizers;

    protected StackTracerCode[] stackTracer;

    @Override
    public int compareTo(ThreadInfo o) {
        int res = name.compareTo(o.name);
        if (res == 0) {
            res = id > o.id ? 1 : 0;
        }
        return res;
    }

    public JsonObject toJson() {
        JsonObject data = new JsonObject();
        data.addProperty("id", id);
        data.addProperty("name", name);
        data.addProperty("state", state);
        data.addProperty("tim_ns", timeNs);
        data.addProperty("time_ms", timeMs);
        data.addProperty("percent", percent);
        data.addProperty("lock_name", lockName);
        data.addProperty("lock_owner_name", lockOwnerName);
        data.addProperty("lock_owner_id", lockOwnerId);
        JsonArray lockSynchronizers = null;
        if (this.lockSynchronizers != null) {
            lockSynchronizers = new JsonArray(this.lockSynchronizers.length);
            for (String value : this.lockSynchronizers) {
                lockSynchronizers.add(value);
            }
        }
        data.add("lock_synchronizers", lockSynchronizers);
        JsonArray stackTracer = null;
        if (this.stackTracer != null) {
            stackTracer = new JsonArray(this.stackTracer.length);
            for (StackTracerCode value : this.stackTracer) {
                stackTracer.add(value.toJson());
            }
        }
        data.add("stack_tracer", stackTracer);
        return data;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public long getTimeNs() {
        return timeNs;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public double getPercent() {
        return percent;
    }

    public String getLockName() {
        return lockName;
    }

    public String getLockOwnerName() {
        return lockOwnerName;
    }

    public Long getLockOwnerId() {
        return lockOwnerId;
    }

    public String[] getLockSynchronizers() {
        return lockSynchronizers;
    }

    public StackTracerCode[] getStackTracer() {
        return stackTracer;
    }
    
    
}
