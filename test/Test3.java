
import github.alexozk.scheduler.Scheduler;
import github.alexozk.scheduler.Task;
import github.alexozk.scheduler.TaskListener;
import github.alexozk.scheduler.TaskListenerAdapter;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author alexo
 */
public class Test3 {

    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler();
        scheduler.addTaskListener(new TaskListener() {
            @Override
            public void onTaskStarted(Task task) {
                System.out.println("STARTED " + task);
            }

            @Override
            public void onTaskCompleted(Task task) {
                System.out.println("COMPLETED " + task);
            }

            @Override
            public void onTaskCanceled(Task task) {
                System.out.println("CANCELED " + task);
            }

            @Override
            public void onTaskAdded(Task task) {
                System.out.println("ADDED " + task);
            }
        });
        scheduler.addTaskListener(new TaskListenerAdapter() {
            @Override
            public void onTaskAdded(Task task) {
                System.out.println("ADDED 2" + task);
            }
        });
        scheduler.start();
        scheduler.scheduleAtInterval("I1", () -> {
        }, 2000);
        Task t = scheduler.schedule("c1", () -> {
            System.out.println("can");
        }, 5000);
        scheduler.schedule("a1", () -> {
            t.cancel();
        }, 3000);
    }
}
