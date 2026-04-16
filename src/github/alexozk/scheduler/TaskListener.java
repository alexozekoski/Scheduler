/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozk.scheduler;

/**
 *
 * @author alexo
 */
public interface TaskListener {

    public void onTaskStarted(Task task);

    public void onTaskCompleted(Task task);

    public void onTaskCanceled(Task task);

    public void onTaskAdded(Task task);
}
