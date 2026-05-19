/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozk.scheduler;

/**
 *
 * @author alexo
 */
public abstract class Executor implements Runnable {

    private volatile Thread thread;

    private volatile Task task;

    private boolean runnig = true;

    private volatile boolean inExecution = false;

    private final int position;

    protected Executor(int position) {
        this.position = position;

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
        notifyAll();
    }

    public synchronized boolean setTaskIfCan(Task task) {

        if (this.inExecution || task == null) {
            return false;
        }
        if (this.task == null) {
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
            Task t = null;
            try {
                synchronized (this) {
                    do {
                        t = getTask();
                        waitDelayTask();
                    } while (t == null || t.getDelaySystemTime() > 0);
                    inExecution = true;
                }
            } catch (InterruptedException ex) {
            }
            try {
                if (t != null) {
                    t.execute();
                } else {
                    continue;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                synchronized (this) {
                    this.task = null;
                    this.inExecution = false;
                }
            }
            onCompleteExecution();
        }
    }

    public abstract void onCompleteExecution();

    public synchronized boolean cancelTask(Task task) {
        Task t = getTask();
        if (t == null || task == null) {
            return false;
        }
        if (!inExecution) {
            setTask(null);
        }
        return t.equals(task);
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
        this.runnig = false;
        notifyAll();
    }

    public boolean isInExecution() {
        return inExecution;
    }

}
