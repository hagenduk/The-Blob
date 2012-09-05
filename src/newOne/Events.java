package newOne;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;


/*
 * @author Tobias Zogrotsky, Marvin Melchiors
 */

public class Events{
	private PMgnt pm;
	private double pos_x;
	private double pos_y;
	private int area_x = 200;
	private int area_y = 200;
	private int rad_distance = 10;
	private int particleCounter = 200;
	private int orad = 0;
	private Particle particle;
	private TestParticleGUI gui;
	
	//f�r verzerren
	private int zustand=0;
	private Particle gelocked;
	
	//f�r move
	private int xstart;
	private int ystart;

	private Mp3 player;
	
	public Events(PMgnt pm, TestParticleGUI testParticleGUI) {
		this.gui = testParticleGUI;
		this.pm = pm;
		orad = pm.particlesystem[0].OUTER_RAD;
	}

	//Events to forward to PMgmt and PE

	public void fuettern(int state, int x, int y){
		if(state==1){
			//TODO chemie aktion
			//1. Rahmen auf ganzen Bildschirm ausweiten
			//2. Random Kraft auf alle Partikel
			//3. Theoretisch sollten alle einen kreis bilden am ende
			//4. 
			
			float x_speed;
			float y_speed;
			int xpos;
			int ypos;
			Random rnd = new Random();
			for (Particle p : pm.particlesystem) {
				x_speed = rnd.nextInt(100);
				y_speed = rnd.nextInt(100);
				xpos = rnd.nextInt(1200);
				ypos = rnd.nextInt(800);
				
				System.out.println("chemie erkannt!");
				//System.out.println(p.getSpeed(0));
				//System.out.println(p.getSpeed(1));
				p.setLocation(xpos, ypos);
				p.setSpeed(x_speed, y_speed);
				//System.out.println(p.getSpeed(0));
				//System.out.println(p.getSpeed(1));
			}
			
			
		}
		if(state==2){
			//TODO homoeo aktion
		}
	}
	
	
	public boolean incenter(int x, int y){
		double xmin=0;
		double xmax=0;
		double ymin=0;
		double ymax=0;
		double centerx=0;
		double centery=0;
		int i=0;
		
		for (Particle p : pm.particlesystem) {
			if(i==0){xmin = p.getLocation(0); xmax = p.getLocation(0); ymin = p.getLocation(1); ymax = p.getLocation(1); i=1;}
			if(p.getLocation(0) < xmin)	xmin = p.getLocation(0);
			if(p.getLocation(0) > xmax)	xmax = p.getLocation(0);
			if(p.getLocation(1) < ymin)	ymin = p.getLocation(1);
			if(p.getLocation(1) > ymax)	ymax = p.getLocation(1);
			}
		
		centerx=(xmax-xmin)/2;
		centery=(ymax-ymin)/2;
		System.out.println("xmitte= " + centerx + "ymitte= " + centery);
		System.out.println("xklick= " + x + "yklick= " + y);
		//System.out.println(xmax+ ", " + xmin+ ", " + ymin+ ", " + ymax);
		if(x>(centerx+250) || x<(centerx-250)){ /*System.out.println("false1");*/ return false;}
		else if(y>(centery+250) || y<(centery-250)){ 	
			//System.out.println("false2"); 
			return false;
		}
		else{ System.out.println("true"); return true;}	
	}
	
	
	
	/**
	 * Generates two random numbers as speed for every particle.
	 * @author eifinger
	 * @param mouse_x
	 * @param mouse_y
	 */
	public void poke(int mouse_x, int mouse_y) {
		// TODO Auto-generated method stub
		//(1)partikel ansprechen mit abgefragten koordinaten
		//(2)diesen partikel aendern
		/*
		int distance = 0;
		
		for (Particle p : pm.particlesystem) {
			distance = p.getDistance(mouse_x, mouse_y);
			if(distance <= orad+200){//Ändern!
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
		*/
		
//		for(int i = 0; i < 100; i++){
//			TestController.wait(300);
//			gui.repaint();
//		}
		float x_speed;
		float y_speed;
		Random rnd = new Random();
		for (Particle p : pm.particlesystem) {
			x_speed = rnd.nextInt(40)-20;
			y_speed = rnd.nextInt(40)-20;
			System.out.println("Poke erkannt!");
			//System.out.println(p.getSpeed(0));
			//System.out.println(p.getSpeed(1));
			p.setSpeed(x_speed, y_speed);
			//System.out.println(p.getSpeed(0));
			//System.out.println(p.getSpeed(1));
		}
	}
	
	
	public void verzerren(int mouse_x, int mouse_y, boolean ende) {
		// TODO Auto-generated method stub
		//(1)partikel ansprechen mit abgefragten koordinaten
		//(2)diesen partikel locken
		//(3)partikel bewegen solange nicht ungelocked
		//(4)partikel unlocken(taste loslassen)

		//erster durchlauf:  Partikel finden und locken
				int partikelid=0;
				if(zustand==0){
					double distance = 0;

					for (int i=0; i<pm.particlesystem.length; i++) {
						distance = pm.particlesystem[i].getDistance(mouse_x, mouse_y);
						if(distance <= orad+200){
							gelocked =pm.particlesystem[i];
							pm.pe.calc[i]=false;
							partikelid=i;
							zustand=1;
							break;
						}
					}
					System.out.println("Partikel erkannt!");
					//System.out.println(gelocked.getSpeed(0));
					//System.out.println(gelocked.getSpeed(1));
				}
		
		//Wenn schon ein Partikel gefunden bewegen		
		if(zustand==1){		
			pm.pe.vars[4*partikelid]=mouse_x-100; //gelocked.setLocation(mouse_x-100, mouse_y-100);
			pm.pe.vars[4*partikelid+1]=mouse_y-100;
			pm.pe.calc[partikelid]=true;
			System.out.println("Partikel bewegt!");			
			//System.out.println(gelocked.getLocation(0));
			//System.out.println(gelocked.getLocation(1));
//			gui.repaint();
			
		}
		
		//Wenn ein Partikel gefunden wurde und ende �bergeben wird freigeben
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

		//erster durchlauf:  start setzen und zustand �ndern
				if(zustand==0){
					xstart=mouse_x;
					ystart=mouse_y;
					zustand=1;
					System.out.println("Partikel erkannt!");
					//System.out.println(xstart);
					//System.out.println(ystart);
				}
		
		//Wenn start gesetzt partikelsystem bewegen, neuen start setzen		
		if(zustand==1){
			int xmove = mouse_x - xstart;   
			int ymove = mouse_y - ystart;   
			
			xstart=mouse_x;
			ystart=mouse_y;
			
			for (int i=0; i<pm.particlesystem.length;i++) {
			pm.pe.calc[i]=false;
			//p.setLocation(p.getLocation(0)+xmove, p.getLocation(1)+ymove);
			pm.pe.vars[4*i]+=xmove; //gelocked.setLocation(mouse_x-100, mouse_y-100);
			pm.pe.vars[4*i+1]+=ymove;
			pm.pe.calc[i]=true;
			System.out.println("Partikelsystem bewegt!");			
			//System.out.println(p.getLocation(0));
			//System.out.println(p.getLocation(1));

			}
//			gui.repaint();
			
		}
		
		//Wenn start gesetzt wurde und ende �bergeben wird freigeben
		if(zustand==1 && ende){
			System.out.println("Move ende!");			
			xstart=0;
			ystart=0;
			zustand=0;
		}	
	}
	public void playGood()
	{
		player = new Mp3();

		player.isBad = false;
		player.start();	
	}
	public void playBad()
	{
		player = new Mp3();

		player.isBad = true;
		player.start();	
	}
	
}
	
