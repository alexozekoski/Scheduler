
import github.alexozk.service.Service;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author alexo
 */
public class Test4 {

    public static void main(String[] args) {
        Service s = new Service("s", 1000) {
            @Override
            protected void execute() {
                System.out.println("EXECUTE");

            }

            @Override
            protected void startup() {
                System.out.println("START");
                String a = null;
                System.out.println(a.toCharArray());
            }
        };
        s.start();
    }
}
