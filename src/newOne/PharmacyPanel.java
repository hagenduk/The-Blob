package newOne;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class PharmacyPanel extends JFrame {

	private JPanel contentPane;

	/**
	 * Create the frame.
	 * @param pm 
	 */
	public PharmacyPanel(PMgnt pm) {
		setVisible(true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 150, 88);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JButton btnChemie = new JButton("Chemie");
		btnChemie.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Chemie");
			}
		});
		btnChemie.setPreferredSize(new Dimension(70, 29));
		contentPane.add(btnChemie, BorderLayout.WEST);
		
		JButton btnHom = new JButton("Hom\u00F6");
		btnHom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Hom�");
			}
		});
		btnHom.setPreferredSize(new Dimension(70, 29));
		contentPane.add(btnHom, BorderLayout.EAST);
	}

}
