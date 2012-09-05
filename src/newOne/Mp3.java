package newOne;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

public class Mp3 extends Thread{

	
	//sound
	private FileInputStream inGood;
	private FileInputStream inBad;

	private String path = "sounds";//getClass().getResource("/sounds").getPath();

	//Player-Instanz
	private Player pGood;
	private Player pBad;
	private Player p;
	
	public boolean isBad = true;

	public Mp3 (){
		try {
			inGood = new FileInputStream(path+"/CrazyLaugh.mp3");
			pGood = new Player(inGood);
	
			inBad = new FileInputStream(path+"/Angry.mp3");
			pBad = new Player(inBad);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JavaLayerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void runGood() {
		p = pGood;
		try {
			p.play();
		} catch (JavaLayerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("good");
		Mp3.currentThread().interrupt();
	}


	private void runBad() {
		p = pBad;
		try {
			p.play();
		} catch (JavaLayerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("bad");
		Mp3.currentThread().interrupt();
	}
	
	public void run(){
		if(isBad){
			runBad();
		}else{
			runGood();
		}
	}
	
}