package com.test.spring;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xuwenqiang
 * @date 2021年04月09日 16:22
 */
public class SimpleDateFromatExample {

	public static final ThreadLocal<DateFormat> DATE_FORMAT_THREAD_LOCAL = new InheritableThreadLocal<>();
	public static final ThreadLocal<DateFormat> DATE_FORMAT_THREAD_LOCAL_SAFE = new InheritableThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
	};

	static {
		DATE_FORMAT_THREAD_LOCAL.set(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
	}

	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private  static DateFormat getSimpleDateFormat() {
		return DATE_FORMAT_THREAD_LOCAL.get();
	}

	private static Date parse(String date){
		try {
			return getSimpleDateFormat().parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
//		ExecutorService threadPool = new ThreadPoolExecutor(
//				5,50,0L, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>(100));
//
//		List<String> dates = Arrays.asList("2019-02-21 15:47:01",
//				"2018-03-22 16:46:02",
//				"2017-04-23 17:45:03",
//				"2016-05-24 18:44:04",
//				"2015-06-25 19:43:05",
//				"2014-07-26 20:42:06",
//				"2013-08-27 21:41:07",
//				"2012-09-28 22:40:08",
//				"2011-10-29 23:39:09");
//
//		for (String date : dates) {
//			threadPool.execute(()->{
//				try {
//					System.out.println(DATE_FORMAT_THREAD_LOCAL.get().parse(date));
//				} catch (Exception e){
//					e.printStackTrace();
//				}
//			});
//		}
		String s = new String("asd");
		String s1 = new String( "asd");
		System.out.println(s.hashCode());
		System.out.println(s1.hashCode());
		System.out.println(s == s1);
	}

}
