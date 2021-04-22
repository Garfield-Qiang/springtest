package com.test.spring.controller;

import com.test.spring.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/test")
public class TestController {

	@Autowired
	private HttpServletRequest requestVo;

	@Autowired
	TestService testService;

	@GetMapping("/hello")
	public void getAddDiskSpec(HttpServletRequest request) throws InterruptedException {
		// 从请求上下文里获取Request对象
		ServletRequestAttributes requestAttributes = ServletRequestAttributes.class.cast(RequestContextHolder.getRequestAttributes());
//		Thread thread = new Thread(()->{
//			ServletRequestAttributes requestAttributes1 = ServletRequestAttributes.class.cast(RequestContextHolder.getRequestAttributes());
//			HttpServletRequest contextRequest1 = requestAttributes1.getRequest();
//			System.out.println("123"+contextRequest1);
//		});
//		thread.join();
//		thread.start();
		HttpServletRequest contextRequest = requestAttributes.getRequest();
		System.out.println(request);
		System.out.println(request.hashCode());
		System.out.println(contextRequest.hashCode());
		System.out.println(requestVo.getParameter("username"));

		// 比较两个是否是同一个实例
		System.out.println(contextRequest == request);
		System.out.println(requestVo == request);
	}

	@GetMapping("/hello2")
	public void getAddDiskSpec1(HttpServletRequest request) {
		// 从请求上下文里获取Request对象
		ServletRequestAttributes requestAttributes = ServletRequestAttributes.class.cast(RequestContextHolder.getRequestAttributes());
		HttpServletRequest contextRequest = requestAttributes.getRequest();
		System.out.println(request.hashCode());
		System.out.println(contextRequest.hashCode());
		System.out.println(request);
		System.out.println(contextRequest.getRequestURI());
		System.out.println(requestVo.getParameter("username"));

		// 比较两个是否是同一个实例
		System.out.println(contextRequest == request);
		System.out.println(requestVo == request);
	}

}
