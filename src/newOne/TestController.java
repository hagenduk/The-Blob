package newOne;

	public class TestController{

		private static final int XAREA = 600;			// GUI size
		private static final int YAREA = 600;			// GUI size
		private static final int XAREA_PART_LOC = 600; 	// Sammelpunkt der Partikel
		private static final int YAREA_PART_LOC = 600; 	// Sammelpunkt der Partikel 
		private static final int XAREA_PART_MGNT = 100; 	// Sammelpunkt der Partikel
		private static final int YAREA_PART_MGNT = 100; 	// Sammelpunkt der Partikel 
		
		private static final int REFRESH_TIME = 60;	// Refresh rate (17)
		private static final int PARTICLES = 20;		// Amount of Particles - If oyu change this you need to change it in physicsengine also in helperparticlesystem
		private static final int PARTICLE_RADIUS = 20;  // Particle radius
		
		
	    public static void main(String str[]) {
	    	PMgnt pm = new PMgnt(PARTICLES,XAREA_PART_MGNT,YAREA_PART_MGNT,PARTICLE_RADIUS/2);
			TestParticleGUI t1 = new TestParticleGUI(pm,PARTICLE_RADIUS, XAREA,YAREA);
			
			t1.setVisible(true);
			PhysicEngine pe = new PhysicEngine(pm.particlesystem);
			
			//pe.stop();
			while(true){
				wait(REFRESH_TIME);	// waits for 300 ms
					pe.step(0.05);
					pe.modifyObjects();
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
