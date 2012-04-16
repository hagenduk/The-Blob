package newOne;

	public class TestController{

	    public static void main(String str[]) {
	    	PMgnt pm = new PMgnt(2,200,200,10);
			TestParticleGUI t1 = new TestParticleGUI(pm,20);
			t1.setSize(400,400);
			t1.setVisible(true);
			PhysicEngine pe = new PhysicEngine(pm.particlesystem,400,400);
			int i=0;
			while(i<100){
				wait(500);
					pe.run();
					pm.printParticleLocation();
					pm.printParticleLocation();
					for(int x = 0; x < 100000; x++);
					i++;
					t1.repaint();
				
			}
	    	
	    	
	    	
	    	
//			pm.printParticleLocation();
//			
//			TestParticleGUI t1 = new TestParticleGUI(pm,20);
//			t1.setSize(200,200);
//			t1.setVisible(true);
//			PhysicEngine pe = new PhysicEngine(pm.particlesystem,200,200);
//			int i=0;
//			while(i<100){
//			pe.run();
//			pm.printParticleLocation();
//			for(int x = 0; x < 100000; x++);
//			i++;
//			t1.repaint();
//			}
//			
//			for(Particle p :pm.particlesystem){
//				p.setLocation(0, p.getLocation(0)+100);
//				p.setLocation(1, p.getLocation(1)+100);
//			}
//			t1.setParticleMgnt(pm);
//			t1.repaint();
			
	    }
	    
	    public static void wait (int n){
	        long t0,t1;
	        t0=System.currentTimeMillis();
	        do{
	            t1=System.currentTimeMillis();
	        }
	        while (t1-t0<n);
	}

	    
}
