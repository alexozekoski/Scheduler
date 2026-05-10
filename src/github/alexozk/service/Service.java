/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.service;

import github.alexozk.scheduler.Scheduler;
import github.alexozk.scheduler.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author alexo
 * @param <T>
 */
public abstract class Service<T> extends Scheduler {

    protected volatile Task mainTask;

    private final List<ServiceListener> listeners = Collections.synchronizedList(new ArrayList(10));

    public Service(String name, long interval) {
        this(name, interval, false);
    }

    public Service(String name, long interval, boolean virtualThread) {
        super(name, virtualThread);
        this.mainTask = createUnscheduledTask("Main loop", () -> {
            executeAction();
        }, interval, interval).onCatch((Exception ex) -> {
            ex.printStackTrace();
        });

        this.schedule("Startup", () -> {
            startup();
        }, 2000).onCatch((Exception ex) -> {
            ex.printStackTrace();
        }).onFinally(() -> {
            this.mainTask.schedule();
        });

    }

    private void executeAction() {
        beforeExecute();
        execute();
        afterExecute();
    }

    protected abstract void execute();

    protected abstract void startup();

    @Override
    public void shutdown() {
        mainTask.cancel();
        super.shutdown();
    }

    public void setInterval(long interval) {
        mainTask.setInterval(interval);
    }

    public long getInterval() {
        return mainTask.getInterval();
    }

    public void forceExecute() throws InterruptedException {
        forceExecute(0);
    }

    public void forceExecute(long timeout) throws InterruptedException {
        execute("Force execute", () -> {
            mainTask.execute();
        }).get(timeout);
    }

    public void forceExecuteAsync() {
        execute("Force execute async", () -> {
            mainTask.execute();
        });
    }

    public Task getMainTask() {
        return mainTask;
    }

    protected void beforeExecute() {
        listeners.forEach((l) -> {
            l.beforeExecute();
        });
    }

    protected void afterExecute() {
        listeners.forEach((l) -> {
            l.afterExecute();
        });
    }

    protected void executeValue(T value) {
        listeners.forEach((l) -> {
            l.executeValue(value);
        });
    }

    public List<ServiceListener> getListeners() {
        return listeners;
    }

}
