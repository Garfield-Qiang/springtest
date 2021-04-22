package com.test.spring.demo;

/**
 * @author xuwenqiang
 * @date 2021年04月16日 9:52
 */
public class Chiled extends Parent{

	String name;


	public static void main(String[] args) {
		Chiled chiled = new Chiled();
		chiled.setName("child");
		chiled.PrintName();
	}
}
