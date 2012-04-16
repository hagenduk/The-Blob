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
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JWindow;

public class TestParticleGUI extends JWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private PMgnt pm;
	private int radius;

	public TestParticleGUI(PMgnt pm, int radius) {
		this.pm = pm;
		this.radius = radius;

		/**/

		setSize(400,400);
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
			g.fillOval(p.getLocation(0), p.getLocation(1), radius, radius);
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
