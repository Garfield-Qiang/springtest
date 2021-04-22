package com.test.spring;

import jdk.nashorn.internal.objects.annotations.Constructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xuwenqiang
 * @date 2021年04月14日 17:20
 */
public class ThreadLocalTest {


	private static ThreadLocal<Person> personThreadLocal = new InheritableThreadLocal<Person>(){
		@Override
		protected Person initialValue() {
			return new Person("zhangsan");
		}
	};

	public Person getNextNum(){
		Person person = personThreadLocal.get();
		person.setName(System.currentTimeMillis()+"");
		personThreadLocal.set(person);
		return  personThreadLocal.get();
	}

	public static void main(String[] args) {
//		System.out.println("thread["+Thread.currentThread().getName()+
//				"] test["+personThreadLocal.get()+"]");
		Person person = new Person("zhangsan");
		personThreadLocal.set(person);
		ThreadLocalTest test = new ThreadLocalTest();
		TestClient c1 = new TestClient(test);
		TestClient c2 = new TestClient(test);
		TestClient c3 = new TestClient(test);
		c1.start();
		c2.start();
		c3.start();
//		Map<String,Object> map = new HashMap<>();
//		Person zhangsan = new Person("zhangsan");
//		System.out.println(zhangsan);
//		map.put("1",zhangsan);
//		Person person = (Person) map.get("1");
//		System.out.println(person);
	}

	private static class TestClient extends Thread{

		private ThreadLocalTest test;

		public TestClient(ThreadLocalTest test){
			this.test = test;
		}

		public void run() {
			for (int i = 0;i<3;i++){
				System.out.println("thread["+Thread.currentThread().getName()+
						"] test["+test.getNextNum()+"]");
			}
		}


	}
	@Setter
	@Getter
	@Builder
	static
	class Person{
		private String name = "";

	}

}
