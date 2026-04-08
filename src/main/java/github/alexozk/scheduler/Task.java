/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozk.scheduler;

import com.google.gson.JsonObject;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author alexo
 */
public class Task implements Comparable<Task> {

    private final Runnable runnable;
    private volatile boolean done = false;
    private final Scheduler scheduler;
    private volatile boolean inExcution = false;
    private volatile long delay = 0;
    private volatile long interval = 0;
    private volatile long start = System.currentTimeMillis();
    private volatile long executions = 0;
    private volatile String name;
    private final long id;
    private volatile Exception error;
    private volatile int priority = 0;

    public Task(long id, String name, Runnable runnable, long delay, long interval, Scheduler scheduler, int priority) {
        this.id = id;
        this.name = name;
        this.runnable = runnable;
        this.scheduler = scheduler;
        this.delay = delay;
        this.interval = interval;
        this.priority = priority;
    }

//    public void setInExcution(boolean inExcution) {;
//        this.inExcution = inExcution;
//    }
    public void execute() {
        synchronized (this) {
            while (inExcution) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            inExcution = true;
            error = null;
        }
        try {
            runnable.run();

        } catch (Exception ex) {
            error = ex;
        }

        synchronized (this) {
            if (!isInterval()) {
                done = true;
            }else{
                start = System.currentTimeMillis();
            }

            executions++;
            inExcution = false;
            notifyAll();
        }
        try {
            scheduler.completeTask(this);

        } catch (Exception ex) {
            error = ex;
        }
        if (error != null) {
            scheduler.onError(this, error);
        }
    }

    public synchronized void cancel() {
        this.scheduler.cancelTask(this);
        notifyAll();
    }

    public synchronized void get(long timeout) {
        if (!done) {
            try {
                wait(timeout);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void get(long timeout, TimeUnit timeUnit) {
        get(TimeUnit.MILLISECONDS.convert(timeout, timeUnit));
    }

    public void get() {
        get(0);
    }

    public boolean isDone() {
        return done;
    }

    public boolean isInExcution() {
        return inExcution;
    }

    public long getDelayInMillis() {
        return delay;
    }

    public long getDelaySystemTime() {
        return getDelaySystemTime(System.currentTimeMillis());
    }

    public long getDelaySystemTime(long start) {
        long pass = start - this.start;
        if (isInterval() && executions > 0) {
//            if ("Check status".equals(name)) {
//                System.out.println(this.start + " " + start);
//                System.out.println(pass + " aaa " + (interval - pass) + " " + interval);
//            }

            return interval - pass;
        }
        return delay - pass;
    }

    public boolean isInterval() {
        return interval > 0;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public long getStart() {
        return start;
    }

    public long getExecutions() {
        return executions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonObject toJson() {
        JsonObject data = new JsonObject();
        data.addProperty("id", id);
        data.addProperty("name", name);
        data.addProperty("interval", interval);
        data.addProperty("in_excution", inExcution);
        data.add("start", Util.toJson(new Date(start)));
        data.addProperty("delay", delay);
        data.addProperty("executions", executions);
        data.addProperty("done", done);
        return data;
    }

    public Exception getError() {
        return error;
    }

    @Override
    public int compareTo(Task o) {
        long mt = getDelaySystemTime();
        long mo = o.getDelaySystemTime();
        if (mt == mo || (mt < 0 && mo < 0)) {
            if (priority == o.priority) {
                return 0;
            }
            if (priority < o.priority) {
                return 1;
            } else {
                return -1;
            }
        }
        if (mt > mo) {
            return 1;
        } else {
            return -1;
        }
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return name + " " + priority;
    }

}