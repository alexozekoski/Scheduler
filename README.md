# Scheduler

A lightweight single-threaded scheduler for Java.

This project provides a simple scheduler that runs tasks sequentially using a single thread.  
It does not create additional threads, ensuring predictable execution and low overhead.

## Features

- Single-thread execution
- No additional threads created
- Task scheduling with delay
- Interval (repeating) tasks
- Task priority support
- Callable and Runnable support
- Optional virtual thread support (Java 21+)
- Task logging support
- Debugging support via Gson JSON (`toJson()`)

## Overview

The scheduler runs all tasks in a single execution loop.

This means:
- Tasks are executed one at a time
- No concurrency between tasks
- No thread management required
- Deterministic behavior
- When multiple tasks are ready, priority defines execution order

## Usage

### Create Scheduler
```java
public static void main(String[] args) {

        Scheduler scheduler = new Scheduler("Example");
        scheduler.setLogSize(5);
        scheduler.start();

        scheduler.execute(() -> {
            System.out.println("Immediate");
        });

        scheduler.schedule(() -> {
            System.out.println("Delayed");
        }, 2000);

        scheduler.scheduleAtInterval(() -> {
            System.out.println("Repeating");
        }, 1000);

        String result = scheduler.schedule(() -> {
            return "Callable result";
        }, 1000);

        System.out.println(result);
    }
```
### Shutdown
```java

scheduler.shutdown();
```

### Shutdown automatically when all tasks finish:
```java 

scheduler.setShutdownOnCompleteAllTasks(true);
```
### Virtual Thread (Java 21+)
```java 

Scheduler scheduler = new Scheduler(true);
scheduler.start();
```

### Execute Immediately
```java 
scheduler.execute(() -> {
    System.out.println("Run immediately");
});

//With name:
scheduler.execute("my-task", () -> {
    System.out.println("Named task");
});

//With priority:
scheduler.execute(() -> {
    System.out.println("High priority");
}, 10);
```


### Schedule Repeating Task (Interval)
```java 
scheduler.scheduleAtInterval(() -> {
    System.out.println("Runs every second");
}, 1000);

// With initial delay + interval:
scheduler.scheduleAtInterval(() -> {
    System.out.println("Delayed + repeating");
}, 2000, 1000);

// Using TimeUnit:
scheduler.scheduleAtInterval(() -> {
    System.out.println("Interval with TimeUnit");
}, 1, 2, TimeUnit.SECONDS);
```


## Tasks
```java
Task task = scheduler.execute("my-task", () -> {
    System.out.println("Named task");
});

task.get(); // wait indefinitely

task.get(5000); // wait with timeout (ms)

task.get(5, TimeUnit.SECONDS);

// Cancel task
task.cancel();

// Debug (JSON)
task.toJson();
```

## Service

### Creating a Service

```java
public class MyService extends Service<String> {

    public MyService() {
        super("MyService", 1000);
    }

    @Override
    protected void startup() {
        System.out.println("Service started");
    }

    @Override
    protected void execute() {
        System.out.println("Running task");
    }
}
// Start Service
MyService service = new MyService();
service.start();

// Stop Service
service.shutdown();

// Interval Control
service.setInterval(2000);


```

### Service Manual Execution
```java
// Synchronous
service.forceExecute(); 

service.forceExecute(5000); // wait up to 5s

// Asynchronous
service.forceExecuteAsync();

```

### Service Listeners
```java
service.getListeners().add(new ServiceListener() {
    @Override
    public void beforeExecute() {
        System.out.println("Before execution");
    }

    @Override
    public void afterExecute() {
        System.out.println("After execution");
    }
});

```
