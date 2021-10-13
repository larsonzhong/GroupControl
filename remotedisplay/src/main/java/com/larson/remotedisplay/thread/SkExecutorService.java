package com.larson.remotedisplay.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;

/**
 * @author Administrator
 * @date 2018\8\27 0027
 **/
public class SkExecutorService {
    public static ExecutorService newExecutor() {
        //设置核心池大小
        int corePoolSize = 3;
        //设置线程池最大能接受多少线程
        int maxPoolSize = 5;
        //当前线程数大于corePoolSize、小于maximumPoolSize时，超出corePoolSize的线程数的生命周期
        long keepActiveTime = 0;
        //设置时间单位，秒
        TimeUnit timeUnit = TimeUnit.SECONDS;
        //设置线程池缓存队列的排队策略为FIFO，并且指定缓存队列大小为1
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(1);
        //创建ThreadPoolExecutor线程池对象，并初始化该对象的各种参数
        return new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                keepActiveTime, timeUnit,
                workQueue,
                new SkThreadFactory.Builder().namingPattern("RemoteDisplayThread-pool-%d").daemon(true).build());
    }
}
