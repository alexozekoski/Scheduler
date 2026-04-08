/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package github.alexozk.scheduler;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author alexo
 */
public class Scheduler implements Runnable {

    private final LinkedList<Task> tasks = new LinkedList<>();

    private volatile boolean run = true;

    private volatile Task nextTask = null;

    private volatile long taskId = 1;

    private volatile boolean shutdownOnCompleteAllTasks = false;

    private volatile LinkedList<Task> tasksDone = new LinkedList();

    private volatile int logSize = 0;

    private volatile Thread executor;

    private String name;

    private boolean virtualThread = false;

    public Scheduler() {
        this("Scheduler");
    }

    public Scheduler(String name) {
        this(name, false);
    }

    public Scheduler(Thread executor) {
        this("Scheduler");
        this.executor = executor;
    }

    public Scheduler(String name, Thread executor) {
        this(name, false);
        this.executor = executor;
    }

    public Scheduler(boolean virtualThread) {
        this(null, virtualThread);
    }

    public Scheduler(String name, boolean virtualThread) {
        this(name, 0, virtualThread);
    }

    public Scheduler(String name, int logsSize, boolean virtualThread) {
        setLogSize(logsSize);
        this.name = name;
        this.virtualThread = virtualThread;
    }

    public synchronized void start() {
        if (executor != null && executor.isAlive()) {
            throw new RuntimeException("Scheduler " + getName() + " is already running");
        }

        try {
            if (virtualThread) {
                var ofVirtual = Thread.class.getMethod("ofVirtual");
                Object builder = ofVirtual.invoke(null);

                var unstarted = builder.getClass().getMethod("unstarted", Runnable.class);
                executor = (Thread) unstarted.invoke(builder, this);
            } else {
                executor = new Thread(this);
            }
        } catch (Exception e) {
            executor = new Thread(this);
        }

        if (name == null) {
            executor.setName("Scheduler " + executor.getId());
        } else {
            executor.setName(name);
        }

        executor.start();
    }

    @Override
    public void run() {
        while (run) {
            try {
                waitTask();
                if (nextTask != null && nextTask.getDelaySystemTime() <= 0) {
                    nextTask.execute();
                    nextTask = getNextTask();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private synchronized void waitTask() {
        long wait = 0;
        if (nextTask != null) {
            wait = nextTask.getDelaySystemTime();
            if (wait <= 0) {
                return;
            }
        }
        try {
            wait(wait);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public Task execute(Runnable run) {
        return schedule(null, run, 0, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task execute(String name, Runnable run) {
        return schedule(name, run, 0, 0, TimeUnit.MILLISECONDS, 0);
    }

    public <T> T execute(Callable<T> callable) {
        return execute(null, callable, 0);
    }

    public <T> T execute(String name, Callable<T> callable) {
        return execute(name, callable, 0);
    }

    public <T> T execute(Callable<T> callable, int priority) {
        return execute(null, callable, priority);
    }

    public <T> T execute(String name, Callable<T> callable, int priority) {
        return schedule(name, callable, 0, TimeUnit.MILLISECONDS, priority);
    }

    public <T> T schedule(Callable<T> callable, long delay) {
        return schedule(null, callable, delay, 0);
    }

    public <T> T schedule(Callable<T> callable, long delay, int priority) {
        return schedule(null, callable, delay, priority);
    }

    public <T> T schedule(String name, Callable<T> callable, long delay, int priority) {
        return schedule(name, callable, delay, TimeUnit.MILLISECONDS, priority);
    }

    public <T> T schedule(String name, Callable<T> callable, long delay, TimeUnit timeUnit, int priority) {
        Object[] response = new Object[1];
        schedule(name, () -> {
            try {
                response[0] = callable.call();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, delay, 0, timeUnit, priority).get();
        return (T) response[0];
    }

    public Task schedule(Runnable run, long delay) {
        return schedule(null, run, delay, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task schedule(String name, Runnable run, long delay) {
        return schedule(name, run, delay, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task schedule(Runnable run, long delay, TimeUnit timeUnit) {
        return schedule(null, run, delay, 0, timeUnit, 0);
    }

    public Task schedule(String name, Runnable run, long delay, TimeUnit timeUnit) {
        return schedule(name, run, delay, 0, timeUnit, 0);
    }

    public Task schedule(String name, Runnable run, long delay, long interval, TimeUnit timeUnit, int priority) {
        long delayMili = TimeUnit.MILLISECONDS.convert(delay, timeUnit);
        long delayInterval = TimeUnit.MILLISECONDS.convert(interval, timeUnit);
        synchronized (this) {
            if (!this.run) {
                throw new RuntimeException("Scheduler " + getName() + " has been shut down and cannot accept new tasks");
            }
            Task task = new Task(taskId++, name, run, delayMili, delayInterval, this, priority);
            sortTask(task);
            nextTask = getNextTask();
            notifyAll();
            return task;
        }
    }

    public Task scheduleAtInterval(Runnable run, long delay, long interval, TimeUnit timeUnit) {
        return schedule(null, run, delay, interval, timeUnit, 0);
    }

    public Task scheduleAtInterval(String name, Runnable run, long delay, long interval, TimeUnit timeUnit) {
        return schedule(name, run, delay, interval, timeUnit, 0);
    }

    public Task scheduleAtInterval(Runnable run, long delay, long interval) {
        return schedule(null, run, delay, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task scheduleAtInterval(String name, Runnable run, long delay, long interval) {
        return schedule(name, run, delay, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task scheduleAtInterval(Runnable run, long interval) {
        return schedule(null, run, interval, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task scheduleAtInterval(String name, Runnable run, long interval) {
        return schedule(name, run, 0, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task execute(Runnable run, int priority) {
        return schedule(null, run, 0, 0, TimeUnit.MILLISECONDS, priority);
    }

    public Task execute(String name, Runnable run, int priority) {
        return schedule(name, run, 0, 0, TimeUnit.MILLISECONDS, priority);
    }

    public Task schedule(Runnable run, long delay, int priority) {
        return schedule(null, run, delay, 0, TimeUnit.MILLISECONDS, priority);
    }

    public Task schedule(String name, Runnable run, long delay, int priority) {
        return schedule(name, run, delay, 0, TimeUnit.MILLISECONDS, priority);
    }

    public Task schedule(Runnable run, long delay, TimeUnit timeUnit, int priority) {
        return schedule(null, run, delay, 0, timeUnit, priority);
    }

    public Task schedule(String name, Runnable run, long delay, TimeUnit timeUnit, int priority) {
        return schedule(name, run, delay, 0, timeUnit, priority);
    }

    public Task scheduleAtInterval(Runnable run, long delay, long interval, TimeUnit timeUnit, int priority) {
        return schedule(null, run, delay, interval, timeUnit, priority);
    }

    public Task scheduleAtInterval(String name, Runnable run, long delay, long interval, TimeUnit timeUnit, int priority) {
        return schedule(name, run, delay, interval, timeUnit, priority);
    }

    public Task scheduleAtInterval(Runnable run, long delay, long interval, int priority) {
        return schedule(null, run, delay, interval, TimeUnit.MILLISECONDS, priority);
    }

    public Task scheduleAtInterval(String name, Runnable run, long delay, long interval, int priority) {
        return schedule(name, run, delay, interval, TimeUnit.MILLISECONDS, priority);
    }

    public Task scheduleAtInterval(Runnable run, int delay, int interval) {
        return schedule(null, run, delay, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task scheduleAtInterval(String name, Runnable run, long delay, int interval) {
        return schedule(name, run, delay, interval, TimeUnit.MILLISECONDS, 0);
    }

    public synchronized boolean cancelTask(Task task) {
        boolean t = tasks.remove(task);
        nextTask = getNextTask();
        tryShutdownWhenAllTasksDone();
        return t;
    }

    public synchronized Task getNextTask() {
        if (tasks.isEmpty()) {
            return null;
        }
        Task t = tasks.getFirst();
        long start = System.currentTimeMillis();
        boolean first = true;
        for (Task task : tasks) {
            if (first) {
                first = false;
                continue;
            }
            if (task.getDelaySystemTime(start) <= 0) {
                if (task.getPriority() > t.getPriority()) {
                    t = task;
                }
            } else {
                break;
            }
        }
        return t;
    }

    private int sortTask(Task task) {
        int pos = Collections.binarySearch(tasks, task);
        if (pos < 0) {
            pos = -pos - 1;
        }
        tasks.add(pos, task);
        Collections.sort(tasks);
        return pos;
    }

    public synchronized void completeTask(Task task) {
        if (!task.isInterval()) {
            tasks.remove(task);
        } else {
            tasks.remove(task);
            sortTask(task);
        }
        addLogTask(task);
        tryShutdownWhenAllTasksDone();
    }

    public synchronized void addLogTask(Task task) {
        if (tasksDone != null && logSize > 0 && task != null) {
            tasksDone.add(task);
        }
        while (tasksDone.size() > logSize) {
            tasksDone.removeFirst();
        }
    }

    public void onError(Task task, Exception ex) {
        if (ex != null) {
            ex.printStackTrace();
        }
    }

    public synchronized void tryShutdownWhenAllTasksDone() {
        if (shutdownOnCompleteAllTasks && !hasTasks()) {
            shutdown();
        }
    }

    public synchronized boolean hasTasksOnInterval() {
        for (Task task : tasks) {
            if (!task.isInterval()) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean hasTasks() {
        return !tasks.isEmpty();
    }

    public synchronized void shutdown() {
        this.nextTask = null;
        run = false;
        notifyAll();
    }

    public boolean isShutdown() {
        return !run;
    }

    public synchronized int getTasksSize() {
        return tasks.size();
    }

    public synchronized JsonObject toJson() {
        JsonObject data = new JsonObject();
        JsonArray ts = new JsonArray(tasks.size());
        for (Task t : tasks) {
            ts.add(t.toJson());
        }
        data.addProperty("id", executor != null ? executor.getId() : null);
        data.addProperty("name", getName());
        data.addProperty("is_running", run);
        data.addProperty("is_interrupted", executor != null ? executor.isInterrupted() : false);
        data.addProperty("is_alive", executor != null ? executor.isAlive() : false);
        data.addProperty("is_daemon", executor != null ? executor.isDaemon() : false);
        data.addProperty("priority", executor != null ? executor.getPriority() : null);
        StackTracerCode[] tracer = executor != null ? Debugger.getStackTracer(executor) : null;
        JsonArray st = null;
        if (tracer != null) {
            st = new JsonArray(tracer.length);
            for (StackTracerCode code : tracer) {
                st.add(code.toJson());
            }
        }

        data.add("stack_tracer", st);
        data.add("next_task", nextTask != null ? nextTask.toJson() : JsonNull.INSTANCE);
        data.add("tasks", ts);

        if (logSize > 0) {
            ts = new JsonArray(tasks.size());
            for (Task t : tasksDone) {
                ts.add(t.toJson());
            }
            data.add("tasks_done", ts);
        }
        return data;
    }

    public int getLogSize() {
        return logSize;
    }

    public synchronized void setLogSize(int logSize) {
        if (logSize < 0) {
            throw new IllegalArgumentException("logSize < 0");
        }

        this.logSize = logSize;
        addLogTask(null);
    }

    public boolean isShutdownOnCompleteAllTasks() {
        return shutdownOnCompleteAllTasks;
    }

    public void setShutdownOnCompleteAllTasks(boolean shutdownOnCompleteAllTasks) {
        this.shutdownOnCompleteAllTasks = shutdownOnCompleteAllTasks;
    }

    public boolean isVirtualThread() {
        return virtualThread;
    }

    public void setVirtualThread(boolean virtualThread) {
        this.virtualThread = virtualThread;
    }

    public Thread getExecutor() {
        return executor;
    }

    public String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
        if (executor != null) {
            executor.setName(name);
        }
    }

    public synchronized List<Task> getCopyTasks() {
        return new ArrayList(tasks);
    }

}
