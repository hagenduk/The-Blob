package sleimiTests;
import static org.junit.Assert.*;

import newOne.Events;
import newOne.PMgnt;

import org.junit.Test;


public class Verformen {

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
		
		// Test aufruf  
		for(int i = 0; i < 100; i++){
			if(i >= 98){
				event.verzerren(250+i, 250, true);	
			}else{
				event.verzerren(250+i, 250, false);	
			}
		}

		// Aufruf auswerten
		if(pm.particlesystem == pm_unchanged1.particlesystem){
			test = false;
		}else{
			test = true;
		}
		System.out.println(test);
		assertTrue(test);
		
	}

}
