import lombok.Setter;
import lombok.ToString;
import org.junit.Test;

/**
 * @author xuwenqiang
 * @date 2021年04月09日 15:23
 */
public class TestThreadLocal {

	private static final ThreadLocal<Person> THREAD_LOCAL = new ThreadLocal<>();
//	private static final ThreadLocal<Person> THREAD_LOCAL_1 = new InheritableThreadLocal<>();

	@Test
	public void fun1() throws InterruptedException {
		setData(new Person());
//		getAndPrintDataAsync();
		getAndPrintData();
	}

	private void setData(Person person) {
		System.out.println("set数据，线程名："+ Thread.currentThread().getName());
		THREAD_LOCAL.set(person);
//		THREAD_LOCAL_1.set(person);
	}

	private void getAndPrintData() {
		Person person1 = THREAD_LOCAL.get();
//		Person person2 = THREAD_LOCAL_1.get();
		System.out.println("ThreadLocal get数据，线程名："+Thread.currentThread().getName()+" ,数据为："+person1);
//		System.out.println("InheritableThreadLocal get数据，线程名："+Thread.currentThread().getName()+" ,数据为："+person2);
	}

	private void getAndPrintDataAsync() throws InterruptedException {
		Thread thread = new Thread(() -> getAndPrintData());
		thread.start();
		thread.join();
	}

	@Setter
	@ToString
	private static class Person{
		private Integer age = 18;
	}
}


