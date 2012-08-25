package sleimiTests;
import static org.junit.Assert.*;

import newOne.PMgnt;
import newOne.TestParticleGUI;

import org.junit.Test;


public class VisFeedback {

	@Test
	public void test() {
		
		boolean test = false;
		// Test vorbereiten
		final int XAREA_PART_MGNT = 100; 	// Sammelpunkt der Partikel
		final int YAREA_PART_MGNT = 100; 	// Sammelpunkt der Partikel 
		final int XAREA = 600;			// GUI size
		final int YAREA = 600;			// GUI size
		final int PARTICLES = 20;		// Amount of Particles
		final int PARTICLE_RADIUS = 20;  // Particle radius
		
    	PMgnt pm = new PMgnt(PARTICLES,XAREA_PART_MGNT,YAREA_PART_MGNT,PARTICLE_RADIUS/2);

		TestParticleGUI t1 = new TestParticleGUI(pm,PARTICLE_RADIUS, XAREA,YAREA);

		assertNotNull(t1);
	
	}

}
