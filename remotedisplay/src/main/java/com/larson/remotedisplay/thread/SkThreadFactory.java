package com.larson.remotedisplay.thread;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SkThreadFactory.java
 *
 * @author Administrator
 * @date 2018-07-25
 */
public class SkThreadFactory implements ThreadFactory {

    /**
     * 计算线程工厂创建的线程数
     */
    private final AtomicLong mThreadCounter;


    /**
     * 包装工厂
     */
    private final ThreadFactory mWrappedFactory;

    /**
     * 非捕获异常处理
     */
    private final Thread.UncaughtExceptionHandler mUncaughtExceptionHandler;

    /**
     * 线程命名模式
     */
    private final String mNamingPattern;

    /**
     * 优先级
     */
    private final Integer mPriority;

    /**
     * 后台状态标识
     */
    private final Boolean mDaemonFlag;

    /**
     * Creates a new instance of {@code ThreadFactoryImpl} and configures it
     * from the specified {@code Builder} object.
     *
     * @param builder the {@code Builder} object
     */
    private SkThreadFactory(Builder builder) {
        if (builder.mWrappedFactory == null) {
            mWrappedFactory = Executors.defaultThreadFactory();
        } else {
            mWrappedFactory = builder.mWrappedFactory;
        }

        mNamingPattern = builder.mNamingPattern;
        mPriority = builder.mPriority;
        mDaemonFlag = builder.mDaemonFlag;
        mUncaughtExceptionHandler = builder.mExceptionHandler;

        mThreadCounter = new AtomicLong();
    }

    /**
     * 获取包装工厂
     *
     * @return 不会返回null
     */
    public final ThreadFactory getWrappedFactory() {
        return mWrappedFactory;
    }

    /**
     * 获取命名模式
     *
     * @return
     */
    public final String getNamingPattern() {
        return mNamingPattern;
    }

    /**
     * 获取是否为后台线程标识
     *
     * @return
     */
    public final Boolean getDaemonFlag() {
        return mDaemonFlag;
    }

    /**
     * 获取优先级
     *
     * @return
     */
    public final Integer getPriority() {
        return mPriority;
    }

    /**
     * 获取非捕获异常处理器
     *
     * @return
     */
    public final Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return mUncaughtExceptionHandler;
    }

    /**
     * 获取创建的线程数量
     *
     * @return
     */
    public long getThreadCount() {
        return mThreadCounter.get();
    }

    /**
     * 创建新线程
     *
     * @param r
     * @return
     */
    @Override
    public Thread newThread(Runnable r) {
        Thread t = getWrappedFactory().newThread(r);
        initializeThread(t);

        return t;
    }

    /**
     * 初始化线程
     *
     * @param t
     */
    private void initializeThread(Thread t) {

        if (getNamingPattern() != null) {
            Long count = Long.valueOf(mThreadCounter.incrementAndGet());
            t.setName(String.format(getNamingPattern(), count));
        }

        if (getUncaughtExceptionHandler() != null) {
            t.setUncaughtExceptionHandler(getUncaughtExceptionHandler());
        }

        if (getPriority() != null) {
            t.setPriority(getPriority().intValue());
        }

        if (getDaemonFlag() != null) {
            t.setDaemon(getDaemonFlag().booleanValue());
        }
    }

    /**
     * 创建器类
     */
    public static class Builder {

        /**
         * 包装工厂
         */
        private ThreadFactory mWrappedFactory;

        /**
         * 非捕获异常处理器
         */
        private Thread.UncaughtExceptionHandler mExceptionHandler;


        /**
         * 命名模式
         */
        private String mNamingPattern;


        /**
         * 优先级
         */
        private Integer mPriority;

        /**
         * 后台标识
         */
        private Boolean mDaemonFlag;

        /**
         * 创建包装工厂
         *
         * @param factory
         * @return
         */
        public Builder wrappedFactory(ThreadFactory factory) {
            if (factory == null) {
                throw new NullPointerException(
                        "Wrapped ThreadFactory must not be null!");
            }

            mWrappedFactory = factory;
            return this;
        }

        /**
         * 设置命名模式
         *
         * @param pattern
         * @return
         */
        public Builder namingPattern(String pattern) {
            if (pattern == null) {
                throw new NullPointerException(
                        "Naming pattern must not be null!");
            }

            mNamingPattern = pattern;
            return this;
        }

        /**
         * 设置后台标识
         *
         * @param f
         * @return
         */
        public Builder daemon(boolean f) {
            mDaemonFlag = Boolean.valueOf(f);
            return this;
        }

        /**
         * 设置优先级
         *
         * @param prio
         * @return
         */
        public Builder priority(int prio) {
            mPriority = Integer.valueOf(prio);
            return this;
        }

        /**
         * 设置非捕获异常处理器
         *
         * @param handler
         * @return
         */
        public Builder uncaughtExceptionHandler(
                Thread.UncaughtExceptionHandler handler) {
            if (handler == null) {
                throw new NullPointerException(
                        "Uncaught exception handler must not be null!");
            }

            mExceptionHandler = handler;
            return this;
        }

        /**
         * 重置构建参数
         */
        public void reset() {
            mWrappedFactory = null;
            mExceptionHandler = null;
            mNamingPattern = null;
            mPriority = null;
            mDaemonFlag = null;
        }

        /**
         * 构建基类线程工厂
         *
         * @return
         */
        public SkThreadFactory build() {
            SkThreadFactory factory = new SkThreadFactory(this);
            reset();
            return factory;
        }
    }
}
