package newOne;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.*;


/*
 * @author Tobias Zogrotsky, Marvin Melchiors
 */

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
	
	//für verzerren
	private int zustand=0;
	private Particle gelocked;
	
	//für move
	private int xstart;
	private int ystart;
	
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

	public void fuettern(){
		if(panel.getState()==1){
			//TODO chemie aktion
			//1. Rahmen auf ganzen Bildschirm ausweiten
			//2. Random Kraft auf alle Partikel
			//3. Theoretisch sollten alle einen kreis bilden am ende
			//4. 
			
		}
		if(panel.getState()==2){
			//TODO homoeo aktion
		}
	}
	
	public void poke(int mouse_x, int mouse_y, boolean hammer) {
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
	
	
	public void verzerren(int mouse_x, int mouse_y, boolean ende) {
		// TODO Auto-generated method stub
		//(1)partikel ansprechen mit abgefragten koordinaten
		//(2)diesen partikel locken
		//(3)partikel bewegen solange nicht ungelocked
		//(4)partikel unlocken(taste loslassen)

		//erster durchlauf:  Partikel finden und locken
				if(zustand==0){
					int distance = 0;

					for (Particle p : pm.particlesystem) {
						distance = p.getDistance(mouse_x, mouse_y);
						if(distance <= orad+200){
							gelocked =p;
							zustand=1;
							break;
						}
					}
					System.out.println("Partikel erkannt!");
					System.out.println(gelocked.getSpeed(0));
					System.out.println(gelocked.getSpeed(1));
				}
		
		//Wenn schon ein Partikel gefunden bewegen		
		if(zustand==1){		
			gelocked.setLocation(mouse_x, mouse_y);
			System.out.println("Partikel bewegt!");			
			System.out.println(particle.getLocation(0));
			System.out.println(particle.getLocation(1));
//			gui.repaint();
			
		}
		
		//Wenn ein Partikel gefunden wurde und ende übergeben wird freigeben
		if(zustand==1 && ende){
			System.out.println("Partikel freigegeben!");			
			gelocked=null;
			zustand=0;
		}
		
	}
	
	
	public void move(int mouse_x, int mouse_y, boolean ende) {
		// TODO Auto-generated method stub
		//(1)x und y start aufschreiben
		//(2)alle partikel um differenz zu start bewegen
		//(3)start = null

		//erster durchlauf:  start setzen und zustand ändern
				if(zustand==0){
					xstart=mouse_x;
					ystart=mouse_y;
					zustand=1;
					System.out.println("Partikel erkannt!");
					System.out.println(xstart);
					System.out.println(ystart);
				}
		
		//Wenn start gesetzt partikelsystem bewegen, neuen start setzen		
		if(zustand==1){
			int xmove = mouse_x - xstart;   
			int ymove = mouse_y - ystart;   

			xstart=mouse_x;
			ystart=mouse_y;
			
			for (Particle p : pm.particlesystem) {
			p.setLocation(p.getLocation(0)+xmove, p.getLocation(1)+ymove);
			System.out.println("Partikelsystem bewegt!");			
			System.out.println(particle.getLocation(0));
			System.out.println(particle.getLocation(1));

			}
//			gui.repaint();
			
		}
		
		//Wenn start gesetzt wurde und ende übergeben wird freigeben
		if(zustand==1 && ende){
			System.out.println("Move ende!");			
			xstart=0;
			ystart=0;
			zustand=0;
		}
		
	}
	

}
	
