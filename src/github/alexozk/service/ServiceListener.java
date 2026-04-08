/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package github.alexozk.service;

/**
 *
 * @author alexo
 * @param <T>
 */
public interface ServiceListener<T> {

    public void beforeExecute();

    public void afterExecute();

    public void executeValue(T value);
}
