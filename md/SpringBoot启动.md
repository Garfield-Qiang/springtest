### 1 初始化

```java
package com.test.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
	}

}
```

上面是一个很简单的springboot项目的启动类，实际启动时就是调用`SpringApplication.run()`这个方法，我们看看这个方法具体做了啥。

```java
SpringApplication.java

public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
        return run(new Class[]{primarySource}, args);
}


public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) 
{
        return (new SpringApplication(primarySources)).run(args);
}
```

最终调用的run() 函数会初始化一个SpringApplication的对象，然后调用该对象的run()函数。所以这里首先得看看在初始化SpringApplication这个对象的时候搞了些什么飞机。

![1611820781404](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\1611820781404.png)

在初始化SpringApplication对象时，会通过SPI做两件重要的事情，会去加载在配置文件中提前定义好的实现了ApplicationContextInitializer接口和ApplicationListener接口的实现类

#### 1.1 SPI

SPI英文为Service Provider Interface，是一种服务发现机制，在springboot中的具体体现就是，springboot在启动的时候会去加载在classpath下面，所有jar包中的`/META-INF/spring.factories`中配置的各个接口的实现类。比如上面说的`ApplicationContextInitializer`和`ApplicationListener`接口。可以看看具体的`spring.factories`文件。

```
spring-boot.jar

# Application Context Initializers
org.springframework.context.ApplicationContextInitializer=\
org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer,\
org.springframework.boot.context.ContextIdApplicationContextInitializer,\
org.springframework.boot.context.config.DelegatingApplicationContextInitializer,\
org.springframework.boot.rsocket.context.RSocketPortInfoApplicationContextInitializer,\
org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer

# Application Listeners
org.springframework.context.ApplicationListener=\
org.springframework.boot.ClearCachesApplicationListener,\
org.springframework.boot.builder.ParentContextCloserApplicationListener,\
org.springframework.boot.cloud.CloudFoundryVcapEnvironmentPostProcessor,\
org.springframework.boot.context.FileEncodingApplicationListener,\
org.springframework.boot.context.config.AnsiOutputApplicationListener,\
org.springframework.boot.context.config.ConfigFileApplicationListener,\
org.springframework.boot.context.config.DelegatingApplicationListener,\
org.springframework.boot.context.logging.ClasspathLoggingApplicationListener,\
org.springframework.boot.context.logging.LoggingApplicationListener,\
org.springframework.boot.liquibase.LiquibaseServiceLocatorApplicationListener
```

在`new SpringApplication`这个对象时，简单来说就是通过键值对，`ApplicationContextInitializer`和`ApplicationListener`作为主键然后查询是否有实现了对应的接口的实现类已经被加载，如果没有就加载，然后返回一个实现类List。