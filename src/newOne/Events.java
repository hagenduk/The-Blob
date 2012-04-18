package newOne;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.*;

public class Events{
	private PMgnt pm;
	private int pos_x;
	private int pos_y;
	private int area_x = 200;
	private int area_y = 200;
	private int rad_distance = 10;
	private int particleCounter = 200;
	private PharmacyPanel panel;
	private int orad = 0;
	private Particle particle;
	private TestParticleGUI gui;
	
	public Events(PMgnt pm, TestParticleGUI testParticleGUI) {
		this.gui = testParticleGUI;
		this.pm = pm;
		panel = new PharmacyPanel(pm);
		orad = pm.particlesystem[0].OUTER_RAD;
		
	}

	//Events to forward to PMgmt and PE
	
	public void openPharmacyPanel(){
		panel.setVisible(true);
	}


	
	public void poke(int mouse_x, int mouse_y) {
		// TODO Auto-generated method stub
		//(1)partikel ansprechen mit abgefragten koordinaten
		//(2)diesen partikel aendern
		int distance = 0;

		for (Particle p : pm.particlesystem) {
			distance = p.getDistance(mouse_x, mouse_y);
			if(distance <= orad+200){
				particle = p;
				break;
			}
		}
		System.out.println("Poke erkannt!");
		System.out.println(particle.getSpeed(0));
		System.out.println(particle.getSpeed(1));
		particle.setSpeed(20, 20);
		
		System.out.println(particle.getSpeed(0));
		System.out.println(particle.getSpeed(1));
		
		
//		for(int i = 0; i < 100; i++){
//			TestController.wait(300);
//			gui.repaint();
//		}
		
	}
	
	
}
	
