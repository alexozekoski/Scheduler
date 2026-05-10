
import github.alexozk.scheduler.Scheduler;
import github.alexozk.scheduler.Task;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author alexo
 */
public class Test5 {

    public static long time = System.currentTimeMillis();

    public static void print() {
        long now = System.currentTimeMillis();
        System.out.println((now - time));
        time = now;
    }

    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler();
        Task task = scheduler.schedule(() -> {
            
        }, 1000);
        task.onFinally(() -> {
            task.schedule();
        });
        task.onCatch((Exception ex) -> {
            ex.printStackTrace();
        });
        scheduler.scheduleAtInterval(() -> {
            task.schedule();
            System.out.println(scheduler.toJson());
        }, 0, 3000).onCatch((Exception ex) -> {

        });
        scheduler.start();
    }
}
