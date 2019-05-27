package org.apache.catalina.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * 生成HTTP 日期的工具类
 */
public final class FastHttpDateFormat {

    // -------------------------------------------------------------- Variables

    /**
     * HTTP 日期格式
     */
    protected static SimpleDateFormat format = 
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);


    protected final static TimeZone gmtZone = TimeZone.getTimeZone("GMT");


    /**
     * GMT timezone - 所有http日期都是格林尼治时间
     */
    static {
        format.setTimeZone(gmtZone);
    }


    /**
     * currentDate对象生成的瞬间
     */
    protected static long currentDateGenerated = 0L;


    /**
     * 当前格式的日期.
     */
    protected static String currentDate = null;


    /**
     * Date cache.
     */
    protected static HashMap dateCache = new HashMap();


    // --------------------------------------------------------- Public Methods


    /**
     * 以http格式获取当前日期
     */
    public static String getCurrentDate() {

        long now = System.currentTimeMillis();
        if ((now - currentDateGenerated) > 1000) {
            synchronized (format) {
                if ((now - currentDateGenerated) > 1000) {
                    currentDateGenerated = now;
                    currentDate = format.format(new Date(now));
                }
            }
        }
        return currentDate;
    }


    /**
     * 获取指定日期的HTTP格式
     */
    public static String getDate(Date date) {

        String cachedDate = (String) dateCache.get(date);
        if (cachedDate != null)
            return cachedDate;

        String newDate = null;
        synchronized (format) {
            newDate = format.format(date);
            dateCache.put(date, newDate);
        }
        return newDate;
    }
}
