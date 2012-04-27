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

	private JPanel contentPane;
	private PharmacyPanel pp;
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
		setUndecorated(true);
		setVisible(true);
		this.pp = this;
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(500, 100, 150, 88);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		ImageIcon iconchemie = new ImageIcon("src/images/iconchemie70x58.png");
		JButton btnChemie = new JButton(iconchemie);
		btnChemie.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Chemo");
			}
		});
		btnChemie.setPreferredSize(new Dimension(67, 29));
		
		ImageIcon iconhomo = new ImageIcon("src/images/iconhomo70x61.png");
		JButton btnHom = new JButton(iconhomo);
		btnHom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Homo");
			}
		});
		btnHom.setPreferredSize(new Dimension(67, 29));
		contentPane.add(btnChemie, BorderLayout.WEST);
		contentPane.add(btnHom, BorderLayout.EAST);
				
		contentPane.addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent arg0) {
			}
			
			@Override
			public void mouseDragged(MouseEvent arg0) {
				pp.setLocation((int)arg0.getLocationOnScreen().getX()-75, (int)arg0.getLocationOnScreen().getY()-44);
			}
		});
	}
}
