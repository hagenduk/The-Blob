package sleimiTests;
import static org.junit.Assert.*;

import newOne.Events;
import newOne.PMgnt;

import org.junit.Test;


public class Poken {

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
		
		// Test aufruf - State == 1 
		event.poke(250, 250);
		
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
