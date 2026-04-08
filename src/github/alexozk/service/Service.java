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

/**
 *
 * @author alexo
 * @param <T>
 */
public abstract class Service<T> extends Scheduler {

    protected final Task mainTask;

    private final List<ServiceListener> listeners = Collections.synchronizedList(new ArrayList(10));

    public Service(String name, long interval) {
        super(name, false);
        this.execute(() -> {
            startup();
        });
        this.mainTask = scheduleAtInterval(() -> {
            executeAction();
        }, interval, interval);
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

    public void forceExecute() {
        forceExecute(0);
    }

    public void forceExecute(long timeout) {
        execute("Force execute", () -> {
            mainTask.execute();
        }).get(timeout);
    }

    public void forceExecuteAsync() {
        execute(() -> {
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

    public List<ServiceListener> getListeners() {
        return listeners;
    }

}
