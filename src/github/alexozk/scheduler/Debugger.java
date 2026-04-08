/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.scheduler;

import com.google.gson.JsonArray;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;

import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author alexo
 */
public class Debugger {

    public static void printStackTrace() {
        printStackTrace(Thread.currentThread());
    }

    public static void printStackTrace(Thread thread) {
        printStackTrace(thread.getStackTrace());
    }

    public static void printStackTrace(StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append(element.toString());
            sb.append('\n');
        }
        System.out.println(sb.toString());
    }

    public static void printStackTrace(JsonArray tracer) {
        List<StackTracerCode> list = new ArrayList(tracer.size());
        for (int i = 0; i < tracer.size(); i++) {
            list.add(new StackTracerCode(tracer.get(i).getAsJsonObject()));
        }
        printStackTrace(list);
    }

    public static void printStackTrace(List<StackTracerCode> tracer) {
        StackTraceElement[] stack = new StackTraceElement[tracer.size()];
        for (int i = 0; i < tracer.size(); i++) {
            stack[i] = tracer.get(i).getAsStackTracerElement();
        }
        printStackTrace(stack);
    }

    public static StackTracerCode[] getStackTracer() {
        return getStackTracer(Thread.currentThread());
    }

    public static StackTracerCode[] getStackTracer(Thread thread) {
        if (thread == null) {
            return null;
        }
        return getStackTracer(thread.getStackTrace());
    }

    public static StackTracerCode[] getStackTracer(Exception ex) {
        return getStackTracer(ex.getStackTrace());
    }

    public static StackTracerCode[] getStackTracer(StackTraceElement[] stackTrace) {
        StackTracerCode[] list = new StackTracerCode[stackTrace.length];
        for (int i = 0; i < list.length; i++) {
            list[i] = new StackTracerCode(stackTrace[i]);
        }
        return list;
    }

    public static StackTracerCode[] getStackTracer(long threadId) {
        return getStackTracer(getThreadById(threadId));
    }

    public static Thread getThreadById(long threadId) {
        Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
        for (Thread t : all.keySet()) {
            if (t.getId() == threadId) {
                return t;
            }
        }
        return null;
    }

    public static List<ThreadInfo> getDeadLockInfo() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = bean.findDeadlockedThreads();
        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            List<ThreadInfo> infos = new ArrayList<>(deadlockedThreads.length);
            for (long id : deadlockedThreads) {
                infos.add(getThreadInfo(id, true));
            }
            return infos;
        }
        return null;
    }

    public static ThreadInfo getThreadInfo(Thread thread, boolean stackTracer) {
        return getThreadInfo(thread.getId(), stackTracer);
    }

    public static ThreadInfo getThreadInfo(long threadId, boolean stackTracer) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        java.lang.management.ThreadInfo info = threadMXBean.getThreadInfo(threadId);
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long processCpuTime = osBean.getProcessCpuTime();
        ThreadInfo of = new ThreadInfo();
        of.id = threadId;
        if (info != null) {
            long cpuTime = threadMXBean.getThreadCpuTime(info.getThreadId());
            of.id = info.getThreadId();
            of.name = info.getThreadName();
            of.state = info.getThreadState().toString();
            of.timeNs = cpuTime;
            of.timeMs = cpuTime / 1000000;
            of.percent = cpuTime == 0 || processCpuTime == 0 ? 0 : cpuTime / (double) processCpuTime;
            of.lockName = info.getLockName();
            of.lockOwnerName = info.getLockOwnerName();
            of.lockOwnerId = info.getLockOwnerId();
            LockInfo[] locksInf = info.getLockedSynchronizers();
            String[] locks;
            if (locksInf != null) {
                locks = new String[locksInf.length];
                for (int i = 0; i < locksInf.length; i++) {
                    locks[i] = locksInf[i].toString();
                }
            } else {
                locks = new String[0];
            }
            of.lockSynchronizers = locks;
            if (stackTracer) {
                of.stackTracer = getStackTracer(threadId);
            }
        }

        return of;
    }

    public static List<ThreadInfo> getAllThreadsInfo(boolean stackTracer) {

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long processCpuTime = osBean.getProcessCpuTime();
        Map<Thread, StackTraceElement[]> thres = Thread.getAllStackTraces();
        List<ThreadInfo> list = new ArrayList(thres.keySet().size());
        for (Thread t : thres.keySet()) {
            ThreadInfo of = new ThreadInfo();
            long cpuTime = threadMXBean.getThreadCpuTime(t.getId());
            of.id = t.getId();
            of.name = t.getName();

            of.timeNs = cpuTime;
            of.timeMs = cpuTime / 1000000;
            of.percent = cpuTime == 0 || processCpuTime == 0 ? 0 : cpuTime / (double) processCpuTime;
            of.state = t.getState().toString();
            java.lang.management.ThreadInfo info = threadMXBean.getThreadInfo(t.getId());
            if (info != null) {
                of.lockName = info.getLockName();
                of.lockOwnerName = info.getLockOwnerName();
                of.lockOwnerId = info.getLockOwnerId();
                LockInfo[] locksInf = info.getLockedSynchronizers();
                String[] locks;
                if (locksInf != null) {
                    locks = new String[locksInf.length];
                    for (int i = 0; i < locksInf.length; i++) {
                        locks[i] = locksInf[i].toString();
                    }
                } else {
                    locks = new String[0];
                }
                of.lockSynchronizers = locks;
                if (stackTracer) {
                    of.stackTracer = getStackTracer(of.getId());
                }
            }

            list.add(of);
        }
        Collections.sort(list);
        return list;
    }
}
