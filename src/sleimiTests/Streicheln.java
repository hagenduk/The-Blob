package sleimiTests;
import static org.junit.Assert.*;

import newOne.PMgnt;
import newOne.TestParticleGUI;

import org.junit.Test;


public class Streicheln {

	@Test
	public void test() {

		// Test vorbereiten
		final int XAREA_PART_MGNT = 100; 	// Sammelpunkt der Partikel
		final int YAREA_PART_MGNT = 100; 	// Sammelpunkt der Partikel 
		final int XAREA = 600;			// GUI size
		final int YAREA = 600;			// GUI size
		final int PARTICLES = 20;		// Amount of Particles
		final int PARTICLE_RADIUS = 20;  // Particle radius
		
    	PMgnt pm = new PMgnt(PARTICLES,XAREA_PART_MGNT,YAREA_PART_MGNT,PARTICLE_RADIUS/2);

		TestParticleGUI t1 = new TestParticleGUI(pm,PARTICLE_RADIUS, XAREA,YAREA);
		
		for(int i = 0; i < 9; i++){
			t1.sauer();
			t1.sauer = 10*i;
		}
		assertNotNull(t1.getTexture());
		assertNotNull(t1.getTexture2());
		assertNotNull(t1.getTexture3());
		assertNotNull(t1);
	}

}
