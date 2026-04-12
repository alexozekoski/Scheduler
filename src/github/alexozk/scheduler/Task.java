/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozk.scheduler;

import com.google.gson.JsonObject;
import java.util.Date;
import java.util.concurrent.RejectedExecutionException;
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
    private volatile long executionTime = -1;

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
        long startRun = System.currentTimeMillis();
        try {
            runnable.run();
        } catch (Exception ex) {
            error = ex;
        }
        long endRun = System.currentTimeMillis();

        synchronized (this) {
            this.executionTime = startRun - endRun;
            if (!isInterval()) {
                done = true;
            } else {
                start = startRun;
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
            if (scheduler.isCurrentThreadExecutor()) {
                if (scheduler.isSingleThread()) {
                    throw new RejectedExecutionException(
                            "The thread attempting to wait for the task " + toString() + " result cannot be the Scheduler's single executor thread, as this would cause a deadlock."
                    );
                } else {
                    if (Scheduler.SHOW_WARNINGS) {
                        System.err.println("Warning: A Scheduler executor thread is being used to wait for a task result in " + toString() + ". This can cause deadlocks and slow down execution.");
                    }

                }
            }
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
        data.addProperty("execution_time", executionTime == -1 ? null : executionTime);
        return data;
    }

    public Exception getError() {
        return error;
    }

    @Override
    public int compareTo(Task o) {
        long mt = getDelaySystemTime();
        long mo = o.getDelaySystemTime();
        if (mt < 0 && mo < 0) {
            if (priority == o.priority) {
                if (mt == mo) {
                    return 0;
                }
                if (mt > mo) {
                    return 1;
                } else {
                    return -1;
                }
            }
            long baseDiff = Math.abs(mt - mo);
            int pDiff = Math.abs(priority - o.priority) * 100;
            if (pDiff > baseDiff) {
                return -1;
            }
            if (priority < o.priority) {
                return 1;
            } else {
                return -1;
            }
        }
        if (mt == mo) {
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
        return (name != null ? name : "#" + id) + ":{" + priority + "}";
    }

    public long getId() {
        return id;
    }

    public long getExecutionTime() {
        return executionTime;
    }

}
