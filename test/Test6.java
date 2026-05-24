
import github.alexozk.scheduler.Debugger;
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
public class Test6 {

    public static void main(String[] args) throws Exception {
        Scheduler scheduler = new Scheduler();
        Task waitTask = scheduler.schedule("c1", () -> {
            System.out.println("shutdown");
            scheduler.shutdown();
        }, 5000);

        scheduler.setShutdownOnCompleteAllTasks(true);
        scheduler.start();
        Task un = scheduler.createUnscheduledTask(() -> {
            System.out.println("b");
        }, 8000);

        un.schedule();
        un.get();
    }
}
