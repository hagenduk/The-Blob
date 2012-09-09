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
 * Diese Klasse verwaltet die events
 */

public class Events{
	//partikelmanagement
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
	
	//fuer verzerren
	private int zustand=0;
	private Particle gelocked;
	
	//fuer move
	private int xstart;
	private int ystart;
	
	//sound
	private Mp3 player;
	
	//Constructor
	public Events(PMgnt pm, TestParticleGUI testParticleGUI) {
		this.gui = testParticleGUI;
		this.pm = pm;
		orad = pm.particlesystem[0].OUTER_RAD;
	}

	//Events to forward to PMgmt and PE
	//fuettern means the click handling if chemie or homeopathy is selected
	public void fuettern(int state, int x, int y){
		if(state==1){
			//TODO chemie aktion
			//1. Rahmen auf ganzen Bildschirm ausweiten (nicht mehr da es komisch aussieht)
			//2. Random Kraft auf alle Partikel (Jepp)
			//3. Theoretisch sollten alle einen kreis bilden am ende (Nope^^)
			
			float x_speed;
			float y_speed;
			int xpos;
			int ypos;
			//random generator
			Random rnd = new Random();
			//iterate through particlearray
			for (Particle p : pm.particlesystem) {
				//dont edit it in physicsengine
				p.setLocked(true);
				//change speed
				x_speed = rnd.nextInt(100);
				y_speed = rnd.nextInt(100);
				//free for physics
				p.setLocked(false);
				//no position change - looks iritating
				//xpos = rnd.nextInt(1200);
				//ypos = rnd.nextInt(800);
				//p.setLocation(xpos, ypos);
				//change the speed
				p.setSpeed(x_speed, y_speed);
			}
			
			
		}
		if(state==2){
			//TODO homoeo aktion
			//which is actially not planned but for version 2.0 maybe
		}
	}
	
	//return wheater the click was in the center of the slime (move) or not (zerreissen)
	public boolean incenter(int x, int y){
		double xmin=0;
		double xmax=0;
		double ymin=0;
		double ymax=0;
		double centerx=0;
		double centery=0;
		int i=0;
		
		//draw a box around the particlesystem
		for (Particle p : pm.particlesystem) {
			if(i==0){xmin = p.getLocation(0); xmax = p.getLocation(0); ymin = p.getLocation(1); ymax = p.getLocation(1); i=1;}
			if(p.getLocation(0) < xmin)	xmin = p.getLocation(0);
			if(p.getLocation(0) > xmax)	xmax = p.getLocation(0);
			if(p.getLocation(1) < ymin)	ymin = p.getLocation(1);
			if(p.getLocation(1) > ymax)	ymax = p.getLocation(1);
			}
		//take the center of it
		centerx=(xmax-xmin)/2;
		centery=(ymax-ymin)/2;
		//if click was in a certain distance return true (170x,y pixel)
		if(x>(centerx+170) || x<(centerx-170)){ return false;}
		else if(y>(centery+170) || y<(centery-170)){ 	
			return false;
		}
		else{ return true;}	
	}
	
	
	
	/**
	 * Generates two random numbers as speed for every particle. if clicked
	 * @author eifinger, melchiors
	 * @param mouse_x
	 * @param mouse_y
	 */
	public void poke(int mouse_x, int mouse_y) {
		// TODO Auto-generated method stub
		//(1)all particles get a new random speed
		
		float x_speed;
		float y_speed;
		Random rnd = new Random();
		for (Particle p : pm.particlesystem) {
			//random speed for all particles
			x_speed = rnd.nextInt(40)-20;
			y_speed = rnd.nextInt(40)-20;
			//change particle speedvalues
			p.setLocked(true);
			p.setSpeed(x_speed, y_speed);
			p.setLocked(false);
		}
	}
	
	//if dragged at the outer area
	public void verzerren(int mouse_x, int mouse_y, boolean ende) {
		// TODO Auto-generated method stub
		//(1)partikel ansprechen mit abgefragten koordinaten
		//(2)diesen partikel locken
		//(3)partikel bewegen solange nicht ungelocked
		//(4)partikel unlocken(taste loslassen)

		//erster durchlauf:  Partikel finden und locken
				if(zustand==0){
					double distance = 0;

					for (Particle p : pm.particlesystem) {
						distance = p.getDistance(mouse_x, mouse_y);
						//if distance is smaler than radius +100
						if(distance <= orad+100){
							//lock the particle
							gelocked =p;
							p.setLocked(true);
							zustand=1;
							break;
						}
					}
				}
		
		//Wenn schon ein Partikel gefunden bewegen		
		if(zustand==1){
			//moves quite fast but this is due to physics....
			gelocked.setLocation(mouse_x-100, mouse_y-100);
			gelocked.setLocked(false);
		}
		
		//Wenn ein Partikel gefunden wurde und ende uebergeben wird freigeben
		if(zustand==1 && ende){
			//play sound
			playnlol();
			gelocked=null;
			zustand=0;
		}
	}
	
	//if clicked in center
	public void move(int mouse_x, int mouse_y, boolean ende) {
		// TODO Auto-generated method stub
		//(1)x und y start aufschreiben
		//(2)alle partikel um differenz zu start bewegen
		//(3)start = null

		//erster durchlauf:  start setzen und zustand aendern
				if(zustand==0){
					xstart=mouse_x;
					ystart=mouse_y;
					zustand=1;
					}
		
		//Wenn start gesetzt partikelsystem bewegen, neuen start setzen		
		if(zustand==1){
			int xmove = mouse_x - xstart;   
			int ymove = mouse_y - ystart;   
			
			xstart=mouse_x;
			ystart=mouse_y;
			
			for (Particle p : pm.particlesystem) {
			p.setLocked(true);
			p.setLocation(p.getLocation(0)+xmove, p.getLocation(1)+ymove);
			p.setLocked(false);
			}
		}
		
		//Wenn start gesetzt wurde und ende uebergeben wird freigeben
		if(zustand==1 && ende){
			//play sound
			playnlol();
			xstart=0;
			ystart=0;
			zustand=0;
		}	
	}
	
	//play a laugh
	public void playGood()
	{
		player = new Mp3("good");
		player.start();
	}
	
	//play a burk
	public void playBad()
	{
		player = new Mp3("bad");
		player.start();
	}
	
	//play a not laughing sound
	public void playnlol()
	{
		player = new Mp3("nlol");
		player.start();
	}
	
}
	
