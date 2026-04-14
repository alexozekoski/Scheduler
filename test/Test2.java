
import github.alexozk.scheduler.Scheduler;
import github.alexozk.scheduler.Task;
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
        Scheduler s = new Scheduler(2);
        Task a = s.scheduleAtInterval(() -> {
           // System.out.println("e");
        }, 1);
        s.scheduleAtInterval(() -> {
           System.out.println(Thread.currentThread().getName() + " 1");
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(Test2.class.getName()).log(Level.SEVERE, null, ex);
//            }
        }, 1000);
        s.scheduleAtInterval(() -> {
            System.out.println(Thread.currentThread().getName() + " 2");
        }, 2000);
        s.schedule("c",() -> {
            System.out.println("cancel");
            a.cancel();
        }, 5000);
        s.setShutdownOnCompleteAllTasks(true);
        s.start();
        
        
    }
}
