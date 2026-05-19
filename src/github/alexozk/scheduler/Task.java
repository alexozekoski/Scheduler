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

    private final TaskRunnable runnable;

    private volatile boolean completed = false;

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

    private volatile boolean canceled = false;

    private volatile boolean scheduled = false;

    private TaskException runCatch = null;

    private Runnable runFinally = null;

    protected Task(long id, String name, TaskRunnable runnable, long delay, long interval, Scheduler scheduler, int priority) {
        this.id = id;
        this.name = name;
        this.runnable = runnable;
        this.scheduler = scheduler;
        this.delay = delay;
        this.interval = interval;
        this.priority = priority;
    }

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
        scheduler.onTaskStarted(this);
        long startRun = System.currentTimeMillis();
        try {
            runnable.run(this);
        } catch (Exception ex) {
            error = ex;
        }
        long endRun = System.currentTimeMillis();
        synchronized (this) {
            this.executionTime = endRun - startRun;
            executions++;
            if (isInterval()) {
                start = startRun;
            }
        }
        try {
            scheduler.completeTask(this);
        } catch (Exception ex) {
            error = ex;
        }

        synchronized (this) {
            inExcution = false;
            if (!isInterval()) {
                scheduled = false;
                completed = true;
            }
            notifyAll();
        }

        if (error != null) {
            executeCatch();
            scheduler.onError(this, error);
        }
        executeFinally();

    }


    public synchronized Task cancel() {
        if(isCompleted()){
            return this;
        }
        this.canceled = true;
        this.scheduler.cancelTask(this);
        notifyAll();
        return this;
    }

    public synchronized Task onFinally(Runnable run) {
        this.runFinally = run;
        if (this.isCompleted()) {
            executeFinally();
        }
        return this;
    }

    public synchronized Task onCatch(TaskException run) {
        this.runCatch = run;
        if (this.isCompleted()) {
            executeCatch();
        }
        return this;
    }

    private synchronized void executeCatch() {
        try {
            if (runCatch != null) {
                runCatch.run(error);
            } else {
                error.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private synchronized void executeFinally() {
        try {
            if (runFinally != null) {
                runFinally.run();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void schedule() {
        if (scheduled) {
            throw new IllegalStateException("Task " + toString() + " already scheduled");
        }
        canceled = false;
        start = System.currentTimeMillis();
        scheduled = true;
        this.scheduler.addTask(this);
    }

    public synchronized void get(long timeout) throws InterruptedException {
        if (!completed) {
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

            wait(timeout);
        }
    }

    public void get(long timeout, TimeUnit timeUnit) throws InterruptedException {
        get(TimeUnit.MILLISECONDS.convert(timeout, timeUnit));
    }

    public void get() throws InterruptedException {
        get(0);
    }

    public Task getSkippingInterrupted() {
        try {
            get(0);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        return this;
    }

    public boolean isCompleted() {
        return completed;
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
        data.addProperty("completed", completed);
        data.addProperty("execution_time", executionTime == -1 ? null : executionTime);
        return data;
    }

    protected void setError(Exception ex) {
        this.error = ex;
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

    public boolean isCanceled() {
        return canceled;
    }

}
