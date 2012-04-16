package newOne;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JFrame;

public class TestParticleGUI extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private PMgnt pm;
	private int radius;
	
	public TestParticleGUI(PMgnt pm, int radius){
		this.pm = pm;
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.radius = radius;
	}
	
	public void paint(Graphics g) {
        super.paint(g);
        g.setColor(Color.green);
        for(Particle p:pm.particlesystem){        
        	g.fillOval(p.getLocation(0), p.getLocation(1), radius, radius);
        }
    }
	
	public void setParticleMgnt(PMgnt pm){
		this.pm = pm;
	}

}
