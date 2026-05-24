
import github.alexozk.scheduler.Scheduler;
import github.alexozk.scheduler.Task;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author alexo
 */
public class Test2 {

    public static void main(String[] args) {
        Scheduler s = new Scheduler(1);
        s.start();
        Task t = s.scheduleAtInterval(() -> {
            System.out.println("exx");
            System.out.println(10 / 0);
        }, 1000).onCatch((Exception ex) -> {
            System.out.println("error");
        }).onFinally(() -> {
            System.out.println("eee");
        });
        s.schedule("c", () -> {
            t.cancel();
            System.out.println("cancel");

        }, 5000);
 
        s.setShutdownOnCompleteAllTasks(true);

    }
}
