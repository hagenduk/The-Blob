package sleimiTests;
import static org.junit.Assert.*;

import newOne.PharmacyPanel;

import org.junit.Test;


public class MediPanel {

	@Test
	public void test() {
		PharmacyPanel panel = new PharmacyPanel();					
		panel.setVisible(true);
		assertNotNull(panel);
	
	}

}
