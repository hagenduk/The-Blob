package newOne;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;

import newOne.PharmacyPanel;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class PharmacyPanel extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JPanel contentPane;
	private PharmacyPanel pPanel;
	private ImageIcon iconChemie;
	private ImageIcon iconHomoe;
	private JButton btnChemie;
	private JButton btnHomoe;

	//TODO test
	private int state=0;
	
	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	/**
	 * Create the frame.
	 * @param pm 
	 */
	
	public PharmacyPanel(PMgnt pm) {
		this.pPanel = this;

		setVisible(true);
		setResizable(true);		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(210, 100, 180, 100);
		
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		iconChemie = new ImageIcon("images/iconchemie70x58.png");
		btnChemie = new JButton(iconChemie);
		iconHomoe = new ImageIcon("images/iconhomo70x61.png");
		btnHomoe = new JButton(iconHomoe);
				
		btnChemie.setPreferredSize(new Dimension(67, 29));
		btnHomoe.setPreferredSize(new Dimension(67, 29));
		contentPane.add(btnChemie, BorderLayout.WEST);
		contentPane.add(btnHomoe, BorderLayout.EAST);
		
		/**
		 * Contentpane mouselistener waiting for mouse dragged event. 
		 * It extracts the current mouse position and relocates the 
		 * pharmacy panel related to the changed mouse position and the size.
		 */
		contentPane.addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent arg0) {
			}
			
			@Override
			public void mouseDragged(MouseEvent arg0) {
				pPanel.setLocation((int)arg0.getLocationOnScreen().getX()-75, (int)arg0.getLocationOnScreen().getY()-44);
			}
		});
		
		
		/**
		 * Workflow (Influence of homoeopatical pharmacy to particle system)
		 * TODO - trigger "homoe" function contained in Events.class 
		 */
		btnHomoe.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Homoe");
			}
		});
		
		/**
		 * Workflow (Influence of chemical pharmacy to particle system)
		 * TODO - trigger "chemie" function contained in Events.class
		 */
		btnChemie.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Chemie");
			}
		});
		setContentPane(contentPane);
	}
}
