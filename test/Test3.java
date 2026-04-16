
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
        Scheduler s = new Scheduler();
        s.addTaskListener(new TaskListener() {
            @Override
            public void onTaskStarted(Task task) {
                System.out.println("START " + task);
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
                System.out.println("ADD " + task);
            }
        });
        s.addTaskListener(new TaskListenerAdapter() {
            @Override
            public void onTaskAdded(Task task) {
                System.out.println("ADD 2" + task);
            }
        });
        s.start();
        s.scheduleAtInterval("I1", () -> {
        }, 2000);
        Task t = s.schedule("c1", () -> {
            System.out.println("can");
        }, 5000);
        s.schedule("a1", () -> {
            t.cancel();
        }, 3000);
    }
}
