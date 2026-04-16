/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package github.alexozk.scheduler;

import com.google.gson.JsonArray;
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
public class Scheduler {

    public static boolean SHOW_WARNINGS = true;

    private final LinkedList<Task> tasks = new LinkedList<>();

    private volatile long taskId = 1;

    private volatile boolean shutdownOnCompleteAllTasks = false;

    private volatile LinkedList<Task> tasksCompleted = new LinkedList();

    private volatile int logSize = 0;

    private final ExecutorTask[] executors;

    private String name;

    private boolean virtualThread = false;

    private volatile boolean started = false;

    private volatile boolean isShutdown = false;

    private final List<TaskListener> listeners = new ArrayList<>();

    public Scheduler() {
        this(null);
    }

    public Scheduler(String name) {
        this(name, false);
    }

    public Scheduler(String name, int executors) {
        this(name, executors, 0, false);
    }

    public Scheduler(String name, int executors, int logsSize) {
        this(name, executors, logsSize, false);
    }

    public Scheduler(int executors) {
        this(null, executors, 0, false);
    }

    public Scheduler(boolean virtualThread) {
        this(null, virtualThread);
    }

    public Scheduler(int executors, boolean virtualThread) {
        this(null, executors, 0, virtualThread);
    }

    public Scheduler(String name, int executors, boolean virtualThread) {
        this(null, executors, 0, virtualThread);
    }

    public Scheduler(String name, boolean virtualThread) {
        this(name, 1, 0, virtualThread);
    }

    public Scheduler(String name, int executors, int logsSize, boolean virtualThread) {
        setLogSize(logsSize);
        this.name = name == null ? "Scheduler@" + hashCode() : name;
        this.virtualThread = virtualThread;
        this.executors = new ExecutorTask[executors];
    }

    public synchronized void start() {
        if (executors[0] != null) {
            throw new RuntimeException("Scheduler " + getName() + " is already running");
        }
        for (int i = 0; i < executors.length; i++) {
            Thread executor;
            ExecutorTask executorTask = new ExecutorTask();
            try {
                if (virtualThread) {
                    var ofVirtual = Thread.class.getMethod("ofVirtual");
                    Object builder = ofVirtual.invoke(null);

                    var unstarted = builder.getClass().getMethod("unstarted", Runnable.class);
                    executor = (Thread) unstarted.invoke(builder, executorTask);
                } else {
                    executor = new Thread(executorTask);
                }
            } catch (Exception e) {
                executor = new Thread(executorTask);
            }

            if (name == null) {
                executor.setName("Scheduler " + executor.getId());
            } else {
                executor.setName(getExecutorName(i));
            }
            executorTask.setThread(executor);
            executor.start();
            executors[i] = executorTask;
        }
        started = true;
        // System.out.println(tasks);
        for (int i = 0; i < this.executors.length && !this.tasks.isEmpty(); i++) {
            addExecutorTask(this.tasks.removeFirst());
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
            if (this.isShutdown) {
                throw new RuntimeException("Scheduler " + getName() + " has been shut down and cannot accept new tasks");
            }
            Task task = new Task(taskId++, name, run, delayMili, delayInterval, this, priority);

            if (!addExecutorTask(task)) {
                sortTask(task);
            }
            onTaskAdded(task);
            return task;
        }
    }

    private synchronized boolean addExecutorTask(Task task) {
        if (!started) {
            return false;
        }
        for (ExecutorTask executorTask : executors) {
            Task t = executorTask.getTask();
            if (executorTask.setTaskIfCan(task)) {
                if (t != null) {
                    this.tasks.add(t);
                }
                return true;
            }
        }
        return false;
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
        if (task == null) {
            return false;
        }
        boolean t = tasks.remove(task);
        if (!t) {
            for (ExecutorTask ta : this.executors) {
                if (ta != null) {
                    if (ta.cancelTask(task)) {
                        ta.setTask(getNextTask());
                        t = true;
                        break;
                    }
                }
            }
        }
        if (t) {
            onTaskCanceled(task);
            tryShutdownWhenAllTasksCompleted();
        }
        return t;
    }

    public synchronized Task getNextTask() {
        if (tasks.isEmpty()) {
            return null;
        }
        Task next = tasks.removeFirst();
        return next;
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
        if (task.isInterval()) {
            if (!task.isCanceled()) {
                sortTask(task);
            }
        }
        addLogTask(task);
        ExecutorTask executorTask = getExecutor(task);
        onTaskCompleted(task);
        if (executorTask != null) {
            executorTask.setTask(getNextTask());
        }
        tryShutdownWhenAllTasksCompleted();
    }

    private synchronized ExecutorTask getExecutor(Task task) {

        for (ExecutorTask executor : this.executors) {
            if (executor != null && executor.isInExecution() && task.equals(executor.getTask())) {
                return executor;
            }
        }
        return null;
    }

    public synchronized void addLogTask(Task task) {
        if (tasksCompleted != null && logSize > 0 && task != null) {
            tasksCompleted.add(task);
        }
        while (tasksCompleted.size() > logSize) {
            tasksCompleted.removeFirst();
        }
    }

    public void onError(Task task, Exception ex) {
        if (ex != null) {
            ex.printStackTrace();
        }
    }

    public synchronized boolean tryShutdownWhenAllTasksCompleted() {
        if (shutdownOnCompleteAllTasks && !hasTasks()) {
            shutdown();
            return true;
        }
        return false;
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
        return getTasksSize() > 0;
    }

    public synchronized void shutdown() {
        isShutdown = true;
        for (ExecutorTask ex : executors) {
            if (ex != null) {
                ex.shutdown();
            }
        }
        ExecutorTask current = getExecutorTaskByThread(Thread.currentThread());
        for (ExecutorTask exec : executors) {
            if (exec != null) {
                if (current == null || !current.equals(exec)) {
                    try {
                        exec.getThread().join();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public synchronized int getNotIntervalTasksSize() {
        int countTask = 0;
        for (ExecutorTask ex : executors) {
            if (ex != null) {
                Task t = ex.getTask();
                if (t != null && !t.isInterval()) {
                    countTask++;
                }
            }
        }
        for (Task t : tasks) {
            if (!t.isInterval()) {
                countTask++;
            }
        }
        return countTask;
    }

    public synchronized int getIntervalTasksSize() {
        int countTask = 0;
        for (ExecutorTask ex : executors) {
            if (ex != null) {
                Task t = ex.getTask();
                if (t != null && t.isInterval()) {
                    countTask++;
                }
            }
        }
        for (Task t : tasks) {
            if (t.isInterval()) {
                countTask++;
            }
        }
        return countTask;
    }

    public synchronized int getTasksSize() {
        int countTask = 0;
        for (ExecutorTask ex : executors) {
            if (ex != null && ex.getTask() != null) {
                countTask++;
            }
        }
        return tasks.size() + countTask;
    }

    public synchronized JsonObject toJson() {
        JsonObject data = new JsonObject();
        JsonArray ts = new JsonArray(tasks.size());
        for (Task t : tasks) {
            ts.add(t.toJson());
        }
        JsonArray executors = null;
        if (this.executors != null) {
            executors = new JsonArray(this.executors.length);
            for (int i = 0; i < executors.size(); i++) {
                ExecutorTask executor = this.executors[i];
                JsonObject edata = new JsonObject();
                edata.addProperty("id", executor.getThread().getId());
                edata.addProperty("name", executor.getThread().getName());
                edata.addProperty("is_running", started);
                edata.addProperty("is_shutdown", isShutdown);
                edata.addProperty("is_interrupted", executor.getThread().isInterrupted());
                edata.addProperty("is_alive", executor.getThread().isAlive());
                edata.addProperty("is_daemon", executor.getThread().isDaemon());
                edata.addProperty("priority", executor.getThread().getPriority());
                Task t = executor.getTask();
                edata.add("task", t != null ? t.toJson() : null);
                StackTracerCode[] tracer = Debugger.getStackTracer(executor.getThread());
                JsonArray st = null;
                if (tracer != null) {
                    st = new JsonArray(tracer.length);
                    for (StackTracerCode code : tracer) {
                        st.add(code.toJson());
                    }
                }
                edata.add("stack_tracer", st);
            }
        }
        data.add("executors", executors);
        data.add("tasks", ts);
        if (logSize > 0) {
            ts = new JsonArray(tasks.size());
            for (Task t : tasksCompleted) {
                ts.add(t.toJson());
            }
            data.add("tasks_completed", ts);
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

    public boolean isSingleThread() {
        return this.executors.length == 1;
    }

    public boolean isMultiThread() {
        return this.executors.length > 1;
    }

    public Thread[] getExecutors() {
        Thread[] t = new Thread[this.executors.length];
        for (int i = 0; i < t.length; i++) {
            ExecutorTask et = this.executors[i];
            if (et != null) {
                t[i] = et.getThread();
            }

        }
        return t;
    }

    public String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
        int pos = 0;
        for (ExecutorTask t : executors) {
            if (t != null) {
                t.getThread().setName(getExecutorName(pos++));
            }
        }
    }

    public synchronized List<Task> getCopyTasks() {
        ArrayList tasks = new ArrayList(this.tasks.size() + this.executors.length);
        for (ExecutorTask ex : this.executors) {
            if (ex != null) {
                Task t = ex.getTask();
                if (t != null) {
                    tasks.add(t);
                }
            }
        }
        Collections.sort(tasks);
        tasks.addAll(this.tasks);
        return tasks;
    }

    public String getExecutorName(int index) {
        return this.executors.length == 1 ? name : name + "#[" + index + "]";
    }

    protected ExecutorTask getExecutorTaskByThread(Thread thread) {
        if (!started) {
            return null;
        }
        for (ExecutorTask ex : executors) {
            if (ex != null && ex.getThread().equals(thread)) {
                return ex;
            }
        }
        return null;
    }

    public boolean isThreadExecutor(Thread thread) {
        return getExecutorTaskByThread(Thread.currentThread()) != null;
    }

    public boolean isCurrentThreadExecutor() {
        return isThreadExecutor(Thread.currentThread());
    }

    public synchronized void addTaskListener(TaskListener listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
    }

    public synchronized boolean removeTaskListener(TaskListener listener) {
        if (listener == null) {
            return false;
        }
        return listeners.remove(listener);
    }

    public synchronized TaskListener[] getTaskListenersCopy() {
        return listeners.toArray(TaskListener[]::new);
    }

    private synchronized TaskListener[] getTaskListenersCopyOrNull() {
        if (listeners.isEmpty()) {
            return null;
        }
        return listeners.toArray(TaskListener[]::new);
    }

    protected void onTaskStarted(Task task) {
        TaskListener[] listeners = getTaskListenersCopyOrNull();
        if (listeners == null) {
            return;
        }
        for (TaskListener listener : listeners) {
            listener.onTaskStarted(task);
        }
    }

    protected void onTaskCompleted(Task task) {
        TaskListener[] listeners = getTaskListenersCopyOrNull();
        if (listeners == null) {
            return;
        }
        for (TaskListener listener : listeners) {
            listener.onTaskCompleted(task);
        }
    }

    protected void onTaskCanceled(Task task) {
        TaskListener[] listeners = getTaskListenersCopyOrNull();
        if (listeners == null) {
            return;
        }
        for (TaskListener listener : listeners) {
            listener.onTaskCanceled(task);
        }
    }

    protected void onTaskAdded(Task task) {
        TaskListener[] listeners = getTaskListenersCopyOrNull();
        if (listeners == null) {
            return;
        }
        for (TaskListener listener : listeners) {
            listener.onTaskAdded(task);
        }
    }
}
