
import github.alexozk.scheduler.Scheduler;
import java.util.Date;
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
public class Test1 {

    public static long time = System.currentTimeMillis();
    

    public static void main(String[] args) {
        Scheduler s = new Scheduler("Test", 1);
        Scheduler.SHOW_WARNINGS = true;
//        s.scheduleAtInterval("t1", () -> {
//            System.out.println("ok " + Thread.currentThread().getName());
//            waitTime(10000);
//            for (int i = 0; i < 10; i++) {
//                String name = "i" + i;
//                s.schedule(name, () -> {
//                    System.out.println(name + "  " + Thread.currentThread().getName());
//                }, 1000 * (i + 1));
//            }
//
//            System.out.println("COOUNT " + s.getCopyTasks());
//        }, 0, 1000, 1000);
//        s.scheduleAtInterval("t11", () -> {
//            long now = System.currentTimeMillis();
//            System.out.println("NO DELAY " + (now - time) + "ms " + Thread.currentThread().getName());
//            time = now;
//        }, 1000, 1000);
//        s.scheduleAtInterval("t3", () -> {
//            System.out.println("ok3 " + Thread.currentThread().getName());
//            waitTime(3000);
//        }, 0, 3000, 1);
        s.schedule("t2", () -> {
            String t = s.execute("CALL ", () -> {
                System.out.println("call " + Thread.currentThread().getName());
                return new Date().toString();
            });
            System.out.println("eee " + Thread.currentThread().getName());
        }, 5000);
        s.start();
    }

    public static void waitTime(long min) {
        try {
            Thread.sleep((long) (min + (Math.random() * 1000)));
        } catch (InterruptedException ex) {
            Logger.getLogger(Test1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
