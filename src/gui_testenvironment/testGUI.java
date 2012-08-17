package gui_testenvironment;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JWindow;

import pe_testenvironment.Events;
import pe_testenvironment.PMgnt;
import pe_testenvironment.Particle;

public class testGUI extends JWindow {

	private static BufferedImage texture; 
	private gui_testenvironment.PMgnt pm;
	private int radius;
	private Events event;
	

	public testGUI(gui_testenvironment.PMgnt pm, int radius, int xarea, int yarea) {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int mouse_x,mouse_y;
				mouse_x = e.getX();
				mouse_y = e.getY();
				
				event.poke(mouse_x, mouse_y);
				//used for Hammer(optional!) and poke
								
			}


			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		this.pm = pm;
		this.radius = radius;
		try {
			texture = ImageIO.read( new File( "images/texture01.png" ) );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/**/

		setSize(xarea,yarea);
		centerOnScreen(this);
		this.setOpacity(0.0f);
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public void paint(Graphics g) {
		super.paint(g);
		g.setColor(Color.green);
		for (gui_testenvironment.Particle p : pm.particlesystem) {
			g.fillOval((int) p.getLocation(0), (int) p.getLocation(1), 200, 200);
		}
	}
	


	private void centerOnScreen(Container c) {
		Dimension paneSize = c.getSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		c.setLocation((screenSize.width - paneSize.width) / 2,
				(screenSize.height - paneSize.height) / 2);
	}

}
