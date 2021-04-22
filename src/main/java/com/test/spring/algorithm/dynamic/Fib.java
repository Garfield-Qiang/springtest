package com.test.spring.algorithm.dynamic;

/**
 *
 *f(n)= {
 *          1,n=1,2
 *            f(n-1)+f(n-2),n>2
 * }
 *
 *
 *
 *
 * @author xuwenqiang
 * @date 2021年04月22日 16:03
 */
public class Fib {


	int fib(int n){
		if (n<1) return 0;
		int[] memo = new int[n];
		return helper(memo,n);
	}

	/***
	 * 自上而下
	 * @author 许文强
	 * @date 2021/4/22 18:01
	 * @param memo
	 * @param n
	 * @return int
	 */
	int helper(int[] memo,int n){
		if(n==1|n==2) return 1;
		if(memo[n-1] !=0) return memo[n-1];
		memo[n-1] = helper(memo,n-1)+helper(memo,n-2);
		return memo[n-1];
	}

	/***
	 *  自下而上
	 * @author 许文强
	 * @date 2021/4/22 18:02
    * @param n
	 * @return int
	 */
	int fib1(int n) {
		int[] dp = new int[n];
		dp[0]=dp[1]=1;
		for(int i=2;i<=n-1;i++){
			dp[i]=dp[i-1]+dp[i-2];
		}
		return dp[n-1];
	}

	int fib2(int n){
		if(n==1||n==2) return 1;
		int pre = 1,curr = 1;
		for(int i=3;i<=n;i++){
			int sum = pre +curr;
			pre = curr;
			curr = sum;
		}
		return curr;
	}


	public static void main(String[] args) {
		Fib test = new Fib();
		System.out.println(test.fib2(6));
	}
}
