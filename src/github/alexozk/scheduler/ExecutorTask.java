/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozk.scheduler;

/**
 *
 * @author alexo
 */
public class ExecutorTask implements Runnable {

    private volatile Thread thread;

    private volatile Task task;

    private boolean runnig = true;

    private volatile boolean inExecution = false;

    protected ExecutorTask() {

    }

    protected void setThread(Thread thread) {
        this.thread = thread;
    }

    public Thread getThread() {
        return thread;
    }

    public Task getTask() {
        return this.task;
    }

    public synchronized void setTask(Task task) {
        this.task = task;
    }

    public synchronized boolean setTaskIfCan(Task task) {
        if (this.inExecution) {
            return false;
        }
        if (this.task == null || task == null) {
            this.task = task;
            notifyAll();
            return true;
        }
        if (this.task.compareTo(task) > 0) {
            this.task = task;
            notifyAll();
            return true;
        }

        return false;
    }

    @Override
    public void run() {
        while (runnig) {
            try {
                waitDelayTask();
            } catch (InterruptedException ex) {
            }
            try {
                Task t;
                synchronized (this) {
                    inExecution = true;
                    t = getTask();
                }
                if (t != null && t.getDelaySystemTime() <= 0) {
                    t.execute();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                this.inExecution = false;
            }

        }
    }

    public synchronized void waitDelayTask() throws InterruptedException {
        if (this.task == null) {
            wait();
        } else {
            long delay = this.task.getDelaySystemTime();
            if (delay > 0) {
                wait(delay);
            }
        }
    }

    public boolean isRunnig() {
        return runnig;
    }

    public synchronized void shutdown() {
        this.task = null;
        this.runnig = false;
        notifyAll();
    }

    public boolean isInExecution() {
        return inExecution;
    }

}
