/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package github.alexozk.scheduler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
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

    public static final List<Scheduler> ALL_ALIVE_SCHEDULERS = Collections.synchronizedList(new ArrayList());

    private final LinkedList<Task> tasks = new LinkedList<>();

    private volatile long taskId = 1;

    private volatile boolean shutdownOnCompleteAllTasks = false;

    private volatile LinkedList<Task> tasksCompleted = new LinkedList();

    private volatile int logSize = 0;

    private final Executor[] executors;

    private String name;

    private boolean virtualThread = false;

    private volatile boolean started = false;

    private volatile boolean isShutdown = false;

    private final List<TaskListener> listeners = new ArrayList<>();

    private Runnable onShutdown = null;

    public static Scheduler getSchedulerByCurrentThread() {
        return getSchedulerByThread(Thread.currentThread());
    }

    public static Scheduler getSchedulerByThread(Thread thread) {
        synchronized (ALL_ALIVE_SCHEDULERS) {
            for (Scheduler scheduler : ALL_ALIVE_SCHEDULERS) {
                for (Thread t : scheduler.getExecutors()) {
                    if (t.equals(thread)) {
                        return scheduler;
                    }
                }
            }
        }
        return null;
    }

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
        this.virtualThread = virtualThread && Runtime.version().feature() >= 21;
        this.executors = new Executor[executors];
    }

    public void start() {
        synchronized (this) {
            if (isShutdown) {
                throw new RuntimeException("Scheduler " + getName() + " has already been shut down");
            }
            if (executors[0] != null) {
                throw new RuntimeException("Scheduler " + getName() + " is already running");
            }
            for (int i = 0; i < executors.length; i++) {
                Thread executor;
                Executor executorTask = new Executor() {
                    @Override
                    public void onCompleteExecution() {
                        getNextTask(this);
                        tryShutdownWhenAllTasksCompleted();
                    }
                };
                if (virtualThread) {
                    executor = Thread.ofVirtual().unstarted(executorTask);
                } else {
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
            for (int i = 0; i < this.executors.length && !this.tasks.isEmpty(); i++) {
                addToExecutorTask(this.tasks.removeFirst());
            }
        }
        ALL_ALIVE_SCHEDULERS.add(this);
    }

    public Task execute(Runnable run) {
        return schedule(null, run, 0, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task execute(String name, Runnable run) {
        return schedule(name, run, 0, 0, TimeUnit.MILLISECONDS, 0);
    }

    public <T> T execute(Callable<T> callable) throws Exception {
        return execute(null, callable, 0);
    }

    public <T> T execute(String name, Callable<T> callable) throws Exception {
        return execute(name, callable, 0);
    }

    public <T> T execute(Callable<T> callable, int priority) throws Exception {
        return execute(null, callable, priority);
    }

    public <T> T execute(String name, Callable<T> callable, int priority) throws Exception {
        return schedule(name, callable, 0, TimeUnit.MILLISECONDS, priority);
    }

    public <T> T schedule(Callable<T> callable, long delay) throws Exception {
        return schedule(null, callable, delay, 0);
    }

    public <T> T schedule(Callable<T> callable, long delay, int priority) throws Exception {
        return schedule(null, callable, delay, priority);
    }

    public <T> T schedule(String name, Callable<T> callable, long delay, int priority) throws Exception {
        return schedule(name, callable, delay, TimeUnit.MILLISECONDS, priority);
    }

    public <T> T schedule(String name, Callable<T> callable, long delay, TimeUnit timeUnit, int priority) throws Exception {
        Object[] response = new Object[1];
        Exception[] exceptions = new Exception[1];
        Task task = schedule(name, () -> {
            try {
                response[0] = callable.call();
            } catch (Exception ex) {
                exceptions[0] = ex;
            }
        }, delay, 0, timeUnit, priority);

        task.setError(exceptions[0]);
        task.get();
        if (task.getError() != null) {
            throw task.getError();
        }
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
        return schedule(name, (Task t) -> {
            run.run();
        }, delay, interval, timeUnit, priority);
    }

    public Task execute(TaskRunnable run) {
        return schedule(null, run, 0, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task execute(String name, TaskRunnable run) {
        return schedule(name, run, 0, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task execute(TaskRunnable run, int priority) {
        return schedule(null, run, 0, 0, TimeUnit.MILLISECONDS, priority);
    }

    public Task execute(String name, TaskRunnable run, int priority) {
        return schedule(name, run, 0, 0, TimeUnit.MILLISECONDS, priority);
    }

    public Task schedule(TaskRunnable run, long delay) {
        return schedule(null, run, delay, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task schedule(String name, TaskRunnable run, long delay) {
        return schedule(name, run, delay, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task schedule(TaskRunnable run, long delay, TimeUnit timeUnit) {
        return schedule(null, run, delay, 0, timeUnit, 0);
    }

    public Task schedule(String name, TaskRunnable run, long delay, TimeUnit timeUnit) {
        return schedule(name, run, delay, 0, timeUnit, 0);
    }

    public Task schedule(String name, TaskRunnable run, long delay, long interval, TimeUnit timeUnit, int priority) {
        synchronized (this) {
            if (this.isShutdown) {
                throw new RuntimeException("Scheduler " + getName() + " has been shut down and cannot accept new tasks");
            }
            Task task = createUnscheduledTask(name, run, delay, interval, timeUnit, priority);
            task.schedule();
            return task;
        }
    }

    protected synchronized void addTask(Task task) {
        if (!addToExecutorTask(task)) {
            sortTask(task);
        }
        onTaskAdded(task);
    }

    public Task createUnscheduledTask(Runnable run) {
        return createUnscheduledTask(null, run, 0, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task createUnscheduledTask(Runnable run, long delay) {
        return createUnscheduledTask(null, run, delay, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task createUnscheduledTask(Runnable run, long delay, long interval) {
        return createUnscheduledTask(null, run, delay, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task createUnscheduledTask(Runnable run, long delay, long interval, int priority) {
        return createUnscheduledTask(null, run, delay, interval, TimeUnit.MILLISECONDS, priority);
    }

    public Task createUnscheduledTask(String name, Runnable run) {
        return createUnscheduledTask(name, run, 0, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task createUnscheduledTask(String name, Runnable run, long delay) {
        return createUnscheduledTask(name, run, delay, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task createUnscheduledTask(String name, Runnable run, long delay, long interval) {
        return createUnscheduledTask(name, run, delay, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task createUnscheduledTask(String name, Runnable run, long delay, long interval, int priority) {
        return createUnscheduledTask(name, run, delay, interval, TimeUnit.MILLISECONDS, priority);
    }

    public synchronized Task createUnscheduledTask(String name, Runnable run, long delay, long interval, TimeUnit timeUnit, int priority) {
        return createUnscheduledTask(name, (Task t) -> {
            run.run();
        }, delay, interval, timeUnit, priority);
    }

    public Task createUnscheduledTask(TaskRunnable run) {
        return createUnscheduledTask(null, run, 0, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task createUnscheduledTask(TaskRunnable run, long delay) {
        return createUnscheduledTask(null, run, delay, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task createUnscheduledTask(TaskRunnable run, long delay, long interval) {
        return createUnscheduledTask(null, run, delay, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task createUnscheduledTask(TaskRunnable run, long delay, long interval, int priority) {
        return createUnscheduledTask(null, run, delay, interval, TimeUnit.MILLISECONDS, priority);
    }

    public Task createUnscheduledTask(String name, TaskRunnable run) {
        return createUnscheduledTask(name, run, 0, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task createUnscheduledTask(String name, TaskRunnable run, long delay) {
        return createUnscheduledTask(name, run, delay, 0, TimeUnit.MILLISECONDS, 0);
    }

    public Task createUnscheduledTask(String name, TaskRunnable run, long delay, long interval) {
        return createUnscheduledTask(name, run, delay, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task createUnscheduledTask(String name, TaskRunnable run, long delay, long interval, int priority) {
        return createUnscheduledTask(name, run, delay, interval, TimeUnit.MILLISECONDS, priority);
    }

    public synchronized Task createUnscheduledTask(String name, TaskRunnable run, long delay, long interval, TimeUnit timeUnit, int priority) {
        long delayMili = TimeUnit.MILLISECONDS.convert(delay, timeUnit);
        long delayInterval = TimeUnit.MILLISECONDS.convert(interval, timeUnit);
        return new Task(taskId++, name, run, delayMili, delayInterval, this, priority);
    }

    private synchronized boolean addToExecutorTask(Task task) {
        if (!started) {
            return false;
        }
        for (Executor executorTask : executors) {
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

    public Task scheduleAtInterval(TaskRunnable run, long delay, long interval, TimeUnit timeUnit) {
        return schedule(null, run, delay, interval, timeUnit, 0);
    }

    public Task scheduleAtInterval(String name, TaskRunnable run, long delay, long interval, TimeUnit timeUnit) {
        return schedule(name, run, delay, interval, timeUnit, 0);
    }

    public Task scheduleAtInterval(TaskRunnable run, long delay, long interval) {
        return schedule(null, run, delay, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task scheduleAtInterval(String name, TaskRunnable run, long delay, long interval) {
        return schedule(name, run, delay, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task scheduleAtInterval(TaskRunnable run, long interval) {
        return schedule(null, run, interval, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task scheduleAtInterval(String name, TaskRunnable run, long interval) {
        return schedule(name, run, 0, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task scheduleAtInterval(TaskRunnable run, long delay, long interval, TimeUnit timeUnit, int priority) {
        return schedule(null, run, delay, interval, timeUnit, priority);
    }

    public Task scheduleAtInterval(String name, TaskRunnable run, long delay, long interval, TimeUnit timeUnit, int priority) {
        return schedule(name, run, delay, interval, timeUnit, priority);
    }

    public Task scheduleAtInterval(TaskRunnable run, long delay, long interval, int priority) {
        return schedule(null, run, delay, interval, TimeUnit.MILLISECONDS, priority);
    }

    public Task scheduleAtInterval(String name, TaskRunnable run, long delay, long interval, int priority) {
        return schedule(name, run, delay, interval, TimeUnit.MILLISECONDS, priority);
    }

    public Task scheduleAtInterval(TaskRunnable run, int delay, int interval) {
        return schedule(null, run, delay, interval, TimeUnit.MILLISECONDS, 0);
    }

    public Task scheduleAtInterval(String name, TaskRunnable run, long delay, int interval) {
        return schedule(name, run, delay, interval, TimeUnit.MILLISECONDS, 0);
    }

    public synchronized boolean cancelTask(Task task) {
        if (task == null) {
            return false;
        }
        boolean t = tasks.remove(task);
        if (!t) {
            for (Executor ta : this.executors) {
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

    protected synchronized void getNextTask(Executor executorTask) {
        if (executorTask != null) {
            executorTask.setTask(getNextTask());
        }
    }

    public synchronized void completeTask(Task task) {
        if (task.isInterval()) {
            if (!task.isCanceled()) {
                sortTask(task);
            }
        }
        addLogTask(task);

        onTaskCompleted(task);
    }

    public synchronized Executor getExecutor(Task task) {
        for (Executor executor : this.executors) {
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

    }

    public synchronized boolean tryShutdownWhenAllTasksCompleted() {
        if (shutdownOnCompleteAllTasks && !hasTasks() && !isShutdown) {
            shutdown();
            return true;
        }
        return isShutdown;
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

    public synchronized void onShutdown(Runnable runnable) {
        this.onShutdown = runnable;
        if (isShutdown) {
            executeOnShutdown();
        }
    }

    private synchronized void executeOnShutdown() {
        try {
            if (onShutdown != null) {
                onShutdown.run();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void shutdown() {
        synchronized (this) {
            if (isShutdown) {
                return;
            }
            isShutdown = true;
        }
        ALL_ALIVE_SCHEDULERS.remove(this);
        synchronized (this) {
            for (Executor ex : executors) {
                if (ex != null) {
                    ex.shutdown();
                }
            }
            Executor current = getExecutorTaskByThread(Thread.currentThread());

            for (Executor exec : executors) {
                if (exec != null) {
                    if (current == null || !current.equals(exec)) {
                        try {
                            exec.shutdown();
                            exec.getThread().join();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            executeOnShutdown();
        }

    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public synchronized int getNotIntervalTasksSize() {
        int countTask = 0;
        for (Executor ex : executors) {
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
        for (Executor ex : executors) {
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
        for (Executor ex : executors) {
            if (ex != null && ex.getTask() != null) {
                countTask++;
            }
        }
        return tasks.size() + countTask;
    }

    public synchronized JsonArray toJsonThreadInfo() {
        Thread[] threads = getExecutors();
        JsonArray data = new JsonArray(threads.length);
        for (Thread thread : threads) {
            JsonObject json = new JsonObject();
            json.addProperty("name", getName());
            json.addProperty("priority", thread.getPriority());
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            long cpuTime = threadMXBean.getThreadCpuTime(thread.getId());
            json.addProperty("cpu", cpuTime);
            json.addProperty("cpu_ms", cpuTime / 1000000);
            java.lang.management.ThreadInfo info = threadMXBean.getThreadInfo(thread.getId());
            String status = null;
            if (info != null) {
                status = info.getThreadState().toString();
            }
            json.addProperty("status", status);
            json.addProperty("thread_id", thread.getId());
        }
        return data;
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
                Executor executor = this.executors[i];
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
        if (this.executors == null) {
            return new Thread[0];
        }
        Thread[] t = new Thread[this.executors.length];
        for (int i = 0; i < t.length; i++) {
            Executor et = this.executors[i];
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
        for (Executor t : executors) {
            if (t != null) {
                t.getThread().setName(getExecutorName(pos++));
            }
        }
    }

    public synchronized List<Task> getCopyTasks() {
        ArrayList tasks = new ArrayList(this.tasks.size() + this.executors.length);
        for (Executor ex : this.executors) {
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

    protected Executor getExecutorTaskByThread(Thread thread) {
        if (!started) {
            return null;
        }
        for (Executor ex : executors) {
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
