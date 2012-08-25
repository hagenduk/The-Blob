package sleimiTests;
import static org.junit.Assert.*;

import newOne.Events;
import newOne.PMgnt;

import org.junit.Test;


public class Automation {

	
	@Test
	public void test() {
		boolean test = false;
		// Test vorbereiten
		final int XAREA_PART_MGNT = 100; 	// Sammelpunkt der Partikel
		final int YAREA_PART_MGNT = 100; 	// Sammelpunkt der Partikel 
		
		final int PARTICLES = 20;			// Amount of Particles
		final int PARTICLE_RADIUS = 20;  	// Particle radius
		
    	PMgnt pm = new PMgnt(PARTICLES,XAREA_PART_MGNT,YAREA_PART_MGNT,PARTICLE_RADIUS/2);
    	PMgnt pm_unchanged1 = new PMgnt(PARTICLES,XAREA_PART_MGNT,YAREA_PART_MGNT,PARTICLE_RADIUS/2);
    	
		Events event = new Events(pm,null);
		long autoTime = System.currentTimeMillis()+10000; // 1 Minute
		
		while(System.currentTimeMillis() != autoTime);

		// Aufruf auswerten
		if(pm.particlesystem == pm_unchanged1.particlesystem){
			test = false;
		}else{
			test = true;
		}
		assertTrue(test);
	}

}
