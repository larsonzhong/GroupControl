package larson.groupcontrol.app.util;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Rony
 * @date 2018/8/29
 */

public class ExecUtils {
    private static final String END_COMMAND = "skyruler_command_end";
    private static ExecUtils instance;
    private final ExecutorService mExecutor;
    private final ExecRunnable mExecRunnable;
    private BufferedWriter mWriter;
    private AtomicBoolean isResponse;
    private BlockingQueue<String> mQueue;

    public static ExecUtils getInstance() {
        if (instance == null) {
            synchronized (ExecUtils.class) {
                if (instance == null) {
                    instance = new ExecUtils();
                }
            }
        }
        return instance;
    }

    private ExecUtils() {
        mExecRunnable = new ExecRunnable();
        mExecutor = newExecutor();
        mExecutor.execute(mExecRunnable);
        isResponse = new AtomicBoolean(false);
        mQueue = new LinkedBlockingQueue<>();
    }

    public void shutdown() {
        if (mWriter != null) {
            try {
                mWriter.write("exit\n");
                mWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mExecutor != null) {
            mExecutor.shutdown();
            try {
                if (!mExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    mExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                mExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        instance = null;
    }

    public String execRoot(String command, long timeOutMs) throws IOException, InterruptedException {
        StringBuilder result = new StringBuilder();
        if (mWriter == null) {
            LogUtils.e("command exec failed :" + command);
            return null;
        }
        mQueue.clear();
        synchronized (this) {
            isResponse.set(false);
            mWriter.write(command + "\n");
            mWriter.write(END_COMMAND + "\n");
            mWriter.flush();
            while (true) {
                String string = mQueue.poll(timeOutMs, TimeUnit.MILLISECONDS);
                if (TextUtils.isEmpty(string)) {
                    break;
                }
                if (string.contains(END_COMMAND)) {
                    break;
                }
                result.append(string);
            }
        }
        mQueue.clear();
        return result.toString();
    }

    public String execRoot(String command) throws IOException, InterruptedException {
        return execRoot(command, 5000);
    }

    private ExecutorService newExecutor() {
        //设置核心池大小
        int corePoolSize = 10;
        //设置线程池最大能接受多少线程
        int maxPoolSize = 500;
        //当前线程数大于corePoolSize、小于maximumPoolSize时，超出corePoolSize的线程数的生命周期
        long keepActiveTime = 200;
        //设置时间单位，秒
        TimeUnit timeUnit = TimeUnit.SECONDS;
        //设置线程池缓存队列的排队策略为FIFO，并且指定缓存队列大小为1
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(1);
        //创建ThreadPoolExecutor线程池对象，并初始化该对象的各种参数
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger integer = new AtomicInteger();

            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, "Exec thread: " + integer.getAndIncrement());
            }
        };
        return new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepActiveTime, timeUnit, workQueue, factory);
    }

    private class ExecRunnable implements Runnable {

        @Override
        public void run() {
            ProcessBuilder builder = new ProcessBuilder("su");
            builder.redirectErrorStream(true);
            BufferedReader input = null;
            Process process = null;
            try {
                process = builder.start();
                mWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                LogUtils.d("Start root exec!");
                while ((line = input.readLine()) != null) {
                    mQueue.add(line + "\n");
                }
                LogUtils.e("mWriter=null, cannot exc root command:" + line);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (mWriter != null) {
                    mWriter.close();
                    mWriter = null;
                }
                if (input != null) {
                    input.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            LogUtils.d("Exit root exec!");
        }
    }

    private static String exec(String[] commands) throws IOException {
        StringBuilder result = new StringBuilder();
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            result.append(line).append("\n");
        }
        process.getInputStream().close();
        process.destroy();
        return result.toString();
    }

    public static String exec(String command) throws IOException {
        String[] strings = command.split(" ");
        return exec(strings);
    }

    public static boolean isCanRoot() {
        // nexus 5x "/su/bin/"
        String[] paths = {"/system/xbin/", "/system/bin/", "/system/sbin/", "/sbin/", "/vendor/bin/", "/su/bin/"};
        for (String path : paths) {
            path = path + "su";
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }
}
