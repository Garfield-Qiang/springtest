package com.test.spring;


public class ThreadTest {

	public static void main(String[] args) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				runSafely(new Runnable1());
				System.out.println("down 1");
				runSafely(new Runnable2());
				System.out.println("down 2");
				runSafely(new Runnable3());
				System.out.println("down 3");
			}

			public void runSafely(Runnable runnable) {
				try {
					runnable.run();
				}
				catch (Throwable ex) {
					// Ignore
				}
			}
		},"test-thread");
		thread.start();
	}

	private static class Runnable1 implements Runnable {

		@Override
		public void run() {
			System.out.println("1");
		}

	}

	private static class Runnable2 implements Runnable {

		@Override
		public void run() {
			System.out.println("2");
		}

	}

	private static class Runnable3 implements Runnable {

		@Override
		public void run() {
			System.out.println("3");
		}

	}
}
