package com.test.spring.service.impl;

import com.test.spring.service.TestService;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestService {

	@Override
	public String sayHello(String userName) {
		String message = userName + "said:fuck";
		return message;
	}


}
