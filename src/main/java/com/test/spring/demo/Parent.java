package com.test.spring.demo;

/**
 * @author xuwenqiang
 * @date 2021年04月16日 9:51
 */
public class Parent {

	public void PrintName() {
		System.out.println("parent print "+name);
	}

	public void setName(String name) {
		System.out.println("parent set "+name);
		this.name = name;
	}

	String name;


}
