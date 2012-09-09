package newOne;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

public class Mp3 extends Thread{

	
	//sound
	private FileInputStream inGood;
	private FileInputStream inBad;
	private FileInputStream inlol;
	private FileInputStream innlol;
	
	private String path = "sounds";//getClass().getResource("/sounds").getPath();

	//Player-Instanz
	private Player pGood;
	private Player pBad;
	private Player plol;
	private Player pnlol;
	private Player p;
	private String was;
	

	public Mp3 (String was){
		this.was=was;
		try {
			inGood = new FileInputStream(path+"/CrazyLaugh.mp3");
			pGood = new Player(inGood);
	
			inBad = new FileInputStream(path+"/burp-1.mp3");
			pBad = new Player(inBad);
			
			//randomly define the played sound
			Random rnd = new Random();
			int choice = rnd.nextInt(6)+1;
			if(choice==1){
			inlol = new FileInputStream(path+"/laugh_1.mp3");
			plol = new Player(inlol);
			}if(choice==2){	
			inlol = new FileInputStream(path+"/laugh_2.mp3");
			plol = new Player(inlol);
			}if(choice==3){
			inlol = new FileInputStream(path+"/laugh_3.mp3");
			plol = new Player(inlol);
			}if(choice==4){
			inlol = new FileInputStream(path+"/laugh_4.mp3");
			plol = new Player(inlol);
			}if(choice==5){
			inlol = new FileInputStream(path+"/laugh_5.mp3");
			plol = new Player(inlol);
			}if(choice==6){
			inlol = new FileInputStream(path+"/laugh_6.mp3");
			plol = new Player(inlol);
			}
			
			//randomly define the played sound
			choice = rnd.nextInt(8)+1;
			if(choice==1){
			innlol = new FileInputStream(path+"/Angry.mp3");
			pnlol = new Player(innlol);
			}if(choice==2){	
			innlol = new FileInputStream(path+"/come_on_1.mp3");
			pnlol = new Player(innlol);
			}if(choice==3){
			innlol = new FileInputStream(path+"/no-5.mp3");
			pnlol = new Player(innlol);
			}if(choice==4){
			innlol = new FileInputStream(path+"/no-6.mp3");
			pnlol = new Player(innlol);
			}if(choice==5){
			innlol = new FileInputStream(path+"/oh-my-god-2.mp3");
			pnlol = new Player(innlol);
			}if(choice==6){
			innlol = new FileInputStream(path+"/shes-a-crazy-psycho-2.mp3");
			pnlol = new Player(innlol);
			}if(choice==7){
			innlol = new FileInputStream(path+"/sigh-2.mp3");
			pnlol = new Player(innlol);
			}if(choice==8){
			innlol = new FileInputStream(path+"/sigh-1.mp3");
			pnlol = new Player(innlol);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (JavaLayerException e) {
			e.printStackTrace();
		}
	}
	
	//plays a laugh
	private void runGood() {
		p = pGood;
		try {
			p.play();
		} catch (JavaLayerException e) {
			e.printStackTrace();
		}

		System.out.println("good");
		Mp3.currentThread().interrupt();
	}

	//play a burp
	private void runBad() {
		p = pBad;
		try {
			p.play();
		} catch (JavaLayerException e) {
			e.printStackTrace();
		}

		System.out.println("bad");
		Mp3.currentThread().interrupt();
	}
	
	//plays a lol
	private void runlol() {
		p = plol;
		try {
			p.play();
		} catch (JavaLayerException e) {
			e.printStackTrace();
		}

		Mp3.currentThread().interrupt();
	}
	
	//plays a no or oh my god or what ever
	private void runnlol() {
		p = pnlol;
		try {
			p.play();
		} catch (JavaLayerException e) {
			e.printStackTrace();
		}
		Mp3.currentThread().interrupt();
	}
	
	public void run(){
		System.out.print(this.was);
		if(this.was.equals("bad")){
			runBad();
		}
		else if(this.was.equals("good")){
			runGood();
		}
		else if(this.was.equals("lol")){
			runlol();
		}
		else if(this.was.equals("nlol")){
			runnlol();
		}
	}
	
}
