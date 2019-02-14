package serv;

public class Benchmark {

	static int limit = 100000000;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Benchmark b = new Benchmark();
	}
	
	public Benchmark() {
		long time = System.currentTimeMillis();
		for(int i=0;i < limit; i++) {
			foo();
		}
		System.out.println(System.currentTimeMillis() - time);
		
		time = System.currentTimeMillis();
		for(int i=0;i < limit; i++) {
			boo();
		}
		System.out.println(System.currentTimeMillis() - time);
	}

	public static void foo() {
		double d = Math.random()*2*6*7/Math.random();
	}
	
	public void boo() {
		double d = Math.random()*2/Math.random();
	}
	
}
