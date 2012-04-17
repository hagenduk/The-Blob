package newOne;

	public class TestController{

	    public static void main(String str[]) {
	    	PMgnt pm = new PMgnt(20,100,100,20);
			TestParticleGUI t1 = new TestParticleGUI(pm,40, 600,600);
			
			t1.setVisible(true);
			PhysicEngine pe = new PhysicEngine(pm.particlesystem,200,200);
			
			int i=0;
			
			while(i < 1000){
				wait(300);	// waits for 300 ms
					pe.run();
					for(int x = 0; x < 100000; x++);
					i++;
					t1.repaint();
				
			}
	    }
	    
	    public static void wait (int timeToWait){
	        long t0,t1;
	        t0 = System.currentTimeMillis();
	        do{
	            t1 = System.currentTimeMillis();
	        }
	        while(t1 - t0 < timeToWait);
	}

	    
}
