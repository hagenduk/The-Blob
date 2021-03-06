package testParticleSystem;

public class Events {
	private PMgnt pm;
	private int pos_x;
	private int pos_y;
	private int area_x = 200;
	private int area_y = 200;
	private int rad_distance = 10;
	private int particleCounter = 200;
	private PharmacyPanel panel;


	public Events(PMgnt pm) {
		this.pm = pm;
		panel = new PharmacyPanel(pm);
	}

	public void move(int x, int y) {
		boolean check = false;
		int resultVector_y = 0;
		int resultVector_x = 0;
		int distance = 0;
		while (!check) {
			for (int i = 0; i < particleCounter; i++) {
				resultVector_x = pos_x - pm.particlesystem[i].getLocation(0);
				resultVector_y = pos_y - pm.particlesystem[i].getLocation(1);
				distance = calculateDistance(resultVector_x, resultVector_y);
				if (distance <= rad_distance) {
					pm.particlesystem[i].setLocation(0, x);
					pm.particlesystem[i].setLocation(1, y);
					check = true;
					break;
				}
			}
			pos_x = (int) ((Math.random() + 1) * area_x);
			pos_y = (int) ((Math.random() + 1) * area_y);
		}
	}
	
	public void openPharmacyPanel(){
		panel.setVisible(true);
	}
	
	private int calculateDistance(int resultVector_x, int resultVector_y) {
		int distance = (int) Math.sqrt((resultVector_y * resultVector_x)
				+ (resultVector_y * resultVector_y));
		return distance;
	}

}
