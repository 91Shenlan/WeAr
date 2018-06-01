package com.webank.mbank.ar.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
import com.webank.mbank.ar.BuildConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个最轻量的日志类
 * 增加模块名, tag命名为: Wb-模块名-类名(可以把类名放到msg中去)
 */
public class Logger {
    //默认根Tag
    private static final String ROOT_TAT = "WeAr";
    private static final String ROOT_TAG_PREFIX = "WeAr";
    //代理出口
    private static List<ILog> proxys = new ArrayList<>();
    private static Reporter sReporter;

    public static void setReporter(Reporter reporter) {
        sReporter = reporter;
    }

    private static ILog logger = new ILog() {
        @Override
        public void v(String tag, Throwable t, String msg, Object... args) {
            for (int i = 0; i < proxys.size(); i++) {
                proxys.get(i).v(tag, t, msg, args);
            }
        }

        @Override
        public void d(String tag, Throwable t, String msg, Object... args) {
            for (int i = 0; i < proxys.size(); i++) {
                proxys.get(i).d(tag, t, msg, args);
            }
        }

        @Override
        public void i(String tag, Throwable t, String msg, Object... args) {
            for (int i = 0; i < proxys.size(); i++) {
                proxys.get(i).i(tag, t, msg, args);
            }
        }

        @Override
        public void w(String tag, Throwable t, String msg, Object... args) {
            for (int i = 0; i < proxys.size(); i++) {
                proxys.get(i).w(tag, t, msg, args);
            }
        }

        @Override
        public void e(String tag, Throwable t, String msg, Object... args) {
            for (int i = 0; i < proxys.size(); i++) {
                proxys.get(i).e(tag, t, msg, args);
            }
        }

        @Override
        public void wtf(String tag, Throwable t, String msg, Object... args) {
            for (int i = 0; i < proxys.size(); i++) {
                proxys.get(i).wtf(tag, t, msg, args);
            }
        }
    };
    //默认日志级别
    private static int logLevel = android.util.Log.VERBOSE;
    //异常处理器
    private static ExceptionHandler exceptionHandler;


    //配置类
    private static Config config = new Config();

    static {
        if (!BuildConfig.DEBUG) {
            closeLog();
        }
    }


    public static final class Config {
        private Config() {
        }

        public Config logLevel(int val) {
            Logger.logLevel = val;
            return this;
        }

        public Config exceptionHandler(ExceptionHandler val) {
            Logger.exceptionHandler = val;
            return this;
        }

        public Config addProxy(ILog val) {
            if (val != null) {
                proxys.add(val);
            }
            return this;
        }
    }


    public static Config config() {
        return config;
    }

    public Logger() {
    }

    public static void setExceptionHandler(ExceptionHandler exceptionHandler) {
        Logger.exceptionHandler = exceptionHandler;
    }

    public static void setLogLevel(int level) {
        logLevel = level;
    }

    public static void closeLog() {
        logLevel = 10;
    }

    public static void addProxy(ILog log) {
        if (log != null) {
            proxys.add(log);
        }
    }

    public static void removeProxy(ILog log) {
        if (log != null) {
            proxys.remove(log);
        }
    }

    public static void addLogger(ILog log) {
        addProxy(log);
    }

    public static void addLogger(ILog2 log) {
        addProxy(log);
    }

    //抛出一个异常:默认会打印
    public static void throwException(Throwable t) {
        if (t == null) {
            return;
        }
        if (exceptionHandler != null) {
            exceptionHandler.handle(false, t);
        } else {
            t.printStackTrace();
        }
    }

    /**
     * 代表一个流程的开始
     *
     * @param key
     */
    public static void start(String key) {
        if (sReporter != null) {
            sReporter.start(key, null, new Object[]{});
        }
    }

    public static void report(String tag) {
        report(tag, null, null, new Object[0]);
    }

    public static void report(String tag, String s, Object... args) {
        report(tag, null, s, args);
    }

    public static void report(String tag, Throwable t, String s, Object... args) {
        tag = getTag(tag);
        if (sReporter != null) {
            sReporter.report(tag, t, s, args);
        }
    }

    /**
     * 代表一个流程的结束
     */
    public static void flushReport() {
        flushReport(null, 0, "");
    }

    /**
     * 真正执行上报
     */
    public static void flushReport(String key, int code, String msg) {
        if (sReporter != null) {
            if (key != null) {
                sReporter.report(key, null, String.format("code:%d,msg:%s", code, msg));
            }
            sReporter.flush();
        }
    }


    public static void v(String s, Object... args) {
        v(null, null, s, args);
    }

    public static void d(String s, Object... args) {
        d(null, null, s, args);

    }

    public static void i(String s, Object... args) {
        i(null, null, s, args);

    }

    public static void w(String s, Object... args) {
        w(null, null, s, args);
    }

    public static void e(String s, Object... args) {
        e(null, null, s, args);

    }

    public static void wtf(String s, Object... args) {
        wtf(null, null, s, args);

    }

    public static void v(String tag, String s, Object... args) {
        v(tag, null, s, args);
    }

    public static void d(String tag, String s, Object... args) {
        d(tag, null, s, args);

    }

    public static void i(String tag, String s, Object... args) {
        i(tag, null, s, args);
    }

    public static void w(String tag, String s, Object... args) {
        w(tag, null, s, args);

    }

    public static void e(String tag, String s, Object... args) {
        e(tag, null, s, args);

    }

    public static void wtf(String tag, String s, Object... args) {
        wtf(tag, null, s, args);

    }


    public static void v(String tag, Throwable t, String s, Object... args) {
        tag = getTag(tag);
        if (proxys.size()>0) {
            logger.log(android.util.Log.VERBOSE, tag, t, s, args);
        } else if (logLevel <= 2) {
            if (args.length > 0) {
                android.util.Log.v(tag, String.format(s, args), t);
            } else {
                android.util.Log.v(tag, s, t);
            }
            handleException(true, t);
        }

    }

    public static void d(String tag, Throwable t, String s, Object... args) {
        tag = getTag(tag);
        if (proxys.size()>0) {
            logger.log(android.util.Log.DEBUG, tag, t, s, args);
        } else if (logLevel <= 3) {
            if (args.length > 0) {
                android.util.Log.d(tag, String.format(s, args), t);
            } else {
                android.util.Log.d(tag, s, t);
            }
            handleException(true, t);
        }

    }

    public static void i(String tag, Throwable t, String s, Object... args) {
        tag = getTag(tag);
        if (logger != null) {
            logger.log(android.util.Log.INFO, tag, t, s, args);
        } else if (logLevel <= 4) {
            if (args.length > 0) {
                android.util.Log.i(tag, String.format(s, args), t);
            } else {
                android.util.Log.i(tag, s, t);
            }
            handleException(true, t);
        }

    }

    public static void w(String tag, Throwable t, String s, Object... args) {
        tag = getTag(tag);
        if (logger != null) {
            logger.log(android.util.Log.WARN, tag, t, s, args);
        } else if (logLevel <= 5) {
            if (args.length > 0) {
                android.util.Log.w(tag, String.format(s, args), t);
            } else {
                android.util.Log.w(tag, s, t);
            }
            handleException(true, t);
        }

    }

    public static void e(String tag, Throwable t, String s, Object... args) {
        tag = getTag(tag);
        if (logger != null) {
            logger.log(android.util.Log.ERROR, tag, t, s, args);
        } else if (logLevel <= 6) {
            if (args.length > 0) {
                android.util.Log.e(tag, String.format(s, args), t);
            } else {
                android.util.Log.e(tag, s, t);
            }
            handleException(true, t);
        }

    }

    public static void wtf(String tag, Throwable t, String s, Object... args) {
        tag = getTag(tag);
        if (logger != null) {
            logger.log(android.util.Log.ASSERT, tag, t, s, args);
        } else if (logLevel <= 7) {
            if (args.length > 0) {
                android.util.Log.wtf(tag, String.format(s, args), t);
            } else {
                android.util.Log.wtf(tag, s, t);
            }
            handleException(true, t);
        }

    }


    private static void handleException(boolean hasProcess, Throwable t) {
        if (exceptionHandler != null && t != null) {
            exceptionHandler.handle(hasProcess, t);
        }
    }

    private static String getTag(String tag) {
        if (tag == null) {
            return ROOT_TAT;
        } else {
            return ROOT_TAG_PREFIX + tag;
        }
    }


    public interface Reporter {

        /**
         * 开始上报,会清空之前还未上报的所有信息
         *
         * @param key
         * @param msg
         * @param args
         */
        void start(String key, String msg, Object... args);

        /**
         * @param key  单次上报唯一
         * @param t
         * @param msg
         * @param args
         */
        void report(String key, Throwable t, String msg, Object... args);

        /**
         * 调用flush会执行上传,并清空现有report内容
         */
        void flush();
    }

    //日志代理类
    public abstract static class ILog {
        public ILog() {
        }

        public abstract void v(String tag, Throwable t, String msg, Object... args);

        public abstract void d(String tag, Throwable t, String msg, Object... args);

        public abstract void i(String tag, Throwable t, String msg, Object... args);

        public abstract void w(String tag, Throwable t, String msg, Object... args);

        public abstract void e(String tag, Throwable t, String msg, Object... args);

        public void wtf(String tag, Throwable t, String msg, Object... args) {
        }

        //也可以直接实现该接口来做代理
        public void log(int level, String tag, Throwable t, String msg, Object... args) {
            switch (level) {
                case android.util.Log.VERBOSE:
                    v(tag, t, msg, args);
                    break;
                case android.util.Log.DEBUG:
                    d(tag, t, msg, args);
                    break;
                case android.util.Log.INFO:
                    i(tag, t, msg, args);
                    break;
                case android.util.Log.WARN:
                    w(tag, t, msg, args);
                    break;
                case android.util.Log.ERROR:
                    e(tag, t, msg, args);
                    break;
                case android.util.Log.ASSERT:
                    wtf(tag, t, msg, args);
                    break;
            }
        }
    }

    public abstract static class ILog2 extends ILog {
        @Override
        public void v(String tag, Throwable t, String msg, Object... args) {
            log(Log.VERBOSE, tag, t, msg, args);
        }

        @Override
        public void d(String tag, Throwable t, String msg, Object... args) {
            log(Log.DEBUG, tag, t, msg, args);
        }

        @Override
        public void i(String tag, Throwable t, String msg, Object... args) {
            log(Log.INFO, tag, t, msg, args);
        }

        @Override
        public void w(String tag, Throwable t, String msg, Object... args) {
            log(Log.WARN, tag, t, msg, args);
        }

        @Override
        public void e(String tag, Throwable t, String msg, Object... args) {
            log(Log.ERROR, tag, t, msg, args);
        }

        //也可以直接实现该接口来做代理
        @Override
        public abstract void log(int level, String tag, Throwable t, String msg, Object... args);
    }

    //异常处理器
    public interface ExceptionHandler {
        /**
         * @param hasProcess 系统日志工具会默认调用printStackTrace()打印该异常的栈,这就会为TRUE,如果是直接调用的{@link #throwException(Throwable)}这里会是false,防止打印两次异常栈
         * @param t          异常对象
         */
        void handle(boolean hasProcess, Throwable t);
    }

    /**
     * 内置一个最常见的带设备信息的异常类
     */
    public static class ApiException extends Throwable {
        private String apiName; //api名称
        private String device = "product:" + Build.PRODUCT
                + "\nbrand:" + Build.BRAND
                + "\nmodel:" + Build.MODEL
                + "\nfingerprint:" + Build.FINGERPRINT
                + "\nsdk_version:" + Build.VERSION.SDK_INT //19,22,23
                + "\nandroid_version:" + Build.VERSION.RELEASE //4.4,5.1,6.0
                ; //设备信息

        public ApiException(String apiName) {
            this.apiName = apiName;
        }

        public ApiException(String detailMessage, String apiName) {
            super(detailMessage);
            this.apiName = apiName;
        }

        public ApiException(String detailMessage, Throwable cause, String apiName) {
            super(detailMessage, cause);
            this.apiName = apiName;
        }

        public ApiException(Throwable cause, String apiName) {
            super(cause);
            this.apiName = apiName;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        public ApiException(String detailMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace, String apiName) {
            super(detailMessage, cause, enableSuppression, writableStackTrace);
            this.apiName = apiName;
        }

        public String getApiName() {
            return apiName;
        }

        public String getDevice() {
            return device;
        }

        public void setDevice(String device) {
            this.device = device;
        }

        @Override
        public String toString() {
            String msg = getLocalizedMessage();
            String name = getClass().getName();
            String result = name + "[" + apiName + "]:";
            if (msg != null) {
                result += msg;
            }
            result += "\ndevice info:\n" + device;
            return result;
        }
    }

}
