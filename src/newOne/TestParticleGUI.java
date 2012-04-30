package newOne;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JWindow;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TestParticleGUI extends JWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static BufferedImage texture;
	private static BufferedImage texture2;
	private static BufferedImage texture3;
	private PMgnt pm;
	private int radius;
	private Events event;
	private PharmacyPanel panel;
	private int state=0;
	public int sauer=0;
	
	public void sauer(){
		if(sauer==10)
		try {
			texture2 = ImageIO.read( new File( "images/Auge6.png" ) );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(sauer==20)
			try {
				texture2 = ImageIO.read( new File( "images/Auge4.png" ) );
				texture3 = ImageIO.read( new File( "images/Mund3.png" ) );
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		if(sauer==30)
			try {
				texture2 = ImageIO.read( new File( "images/Auge2.png" ) );
				texture3 = ImageIO.read( new File( "images/Mund2.png" ) );
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		if(sauer==40)
			try {
				texture2 = ImageIO.read( new File( "images/Auge1.png" ) );
				texture3 = ImageIO.read( new File( "images/Mund1.png" ) );
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		if(sauer==50)
			try {
				texture2 = ImageIO.read( new File( "images/Auge5.png" ) );
				texture3 = ImageIO.read( new File( "images/Mund5.png" ) );
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		if(sauer==60)
			try {
				texture2 = ImageIO.read( new File( "images/Auge3.png" ) );
				texture3 = ImageIO.read( new File( "images/Mund5.png" ) );
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		if(sauer==70)
			try {
				texture2 = ImageIO.read( new File( "images/Auge8.png" ) );
				texture3 = ImageIO.read( new File( "images/Mund5.png" ) );
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		if(sauer==80)
			try {
				texture2 = ImageIO.read( new File( "images/Auge7.png" ) );
				texture3 = ImageIO.read( new File( "images/Mund4.png" ) );
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	}

	public TestParticleGUI(PMgnt pm, int radius, int xarea, int yarea) {
		 event = new Events(pm,this);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if((e.getModifiers() & InputEvent.BUTTON3_MASK)== InputEvent.BUTTON3_MASK){
					System.out.println("right");
					if(!(panel instanceof PharmacyPanel)){
						panel = new PharmacyPanel();
						panel.setVisible(true);
					}
				}
				else{
					int mouse_x,mouse_y;
					mouse_x = e.getX();
					mouse_y = e.getY();
				
					System.out.println("Poke erkannt!");
					event.poke(mouse_x, mouse_y);
					sauer=sauer+1;
					sauer();
					//used for Hammer(optional!) and poke
				}
			}


			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				System.out.println("Entered");
				if(sauer>0)sauer=sauer-1;
				sauer();
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				System.out.println("left");
				if(sauer>0)sauer=sauer-1;
				sauer();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				int mouse_x,mouse_y;
				mouse_x = e.getX();
				mouse_y = e.getY();
//				System.out.println("Mouse pressed on X: " + mouse_x + " Y:" + mouse_y);
				
				if(event.incenter(mouse_x,mouse_y)){
					event.move(mouse_x, mouse_y, false);
					state=1;
				}
				else{				
				//TODO state var check whether you klicked on rand or in the middle! DONE
					event.verzerren(mouse_x, mouse_y, false);
					state=2;
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				int mouse_x,mouse_y;
				mouse_x = e.getX();
				mouse_y = e.getY();
				if(state==1){
				event.move(mouse_x, mouse_y, false);
				event.move(mouse_x, mouse_y, true);
				}
				else{
					if(state==2){
						//TODO: state var!!
						event.verzerren(mouse_x, mouse_y, false);
						event.verzerren(mouse_x, mouse_y, true);
						sauer=sauer+1;
						sauer();
					}
				}
				

				
//				System.out.println("Mouse released on X: " + mouse_x + " Y:" + mouse_y);
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
			texture2 = ImageIO.read( new File( "images/Auge6.png" ) );
			texture = ImageIO.read( new File( "images/texture01.png" ) );
			texture3 = ImageIO.read( new File( "images/Mund3.png" ) );
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/**/

		setSize(xarea,yarea);
		centerOnScreen(this);
		// capture background before we add components;
		// we need JWindows's size here and component's location must also have
		// been done!
		BackgroundPanel backgroundPanel = new BackgroundPanel(this);
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TestParticleGUI.this.setVisible(false);
				TestParticleGUI.this.dispose();
				System.exit(0);
			}
		});
		backgroundPanel.add(closeButton);
		getContentPane().add(backgroundPanel);

	}

	public void paint(Graphics g) {
		super.paint(g);
		g.setColor(Color.green);
		for (Particle p : pm.particlesystem) {
			if(p.kind==0)
			g.drawImage(texture,p.getLocation(0), p.getLocation(1),this);
			else if(p.kind==1){
				g.drawImage(texture2,p.getLocation(0), p.getLocation(1),this);	
			}
			else{
				g.drawImage(texture3,p.getLocation(0), p.getLocation(1),this);	
			}
		}
	}

	public void setParticleMgnt(PMgnt pm) {
		this.pm = pm;
	}

	private void centerOnScreen(Container c) {
		Dimension paneSize = c.getSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		c.setLocation((screenSize.width - paneSize.width) / 2,
				(screenSize.height - paneSize.height) / 2);
	}

	private class BackgroundPanel extends JPanel {

		BufferedImage image = null;

		BackgroundPanel(JWindow window) {
			Rectangle rect = window.getBounds();
			try {
				image = new Robot().createScreenCapture(rect);
			} catch (AWTException e) {
				throw new RuntimeException(e.getMessage());
			}
		}

		protected void paintComponent(Graphics g) {
			g.drawImage(image, 0, 0, this);
		}
	}
	
	
	

}
