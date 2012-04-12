package prestudy_chris;

public class Particle {
	public final int INNER_RAD = 5;
	public final int OUTER_RAD = 10;
	private int[] location = new int[2];
	private float[] speed = {0.0f, 0.0f};

	Particle(int pos_x, int pos_y) {
		location[0] = pos_x;
		location[1] = pos_y;
	}

	public void setLocation(int direction, int newcoord) {
		this.location[direction] = newcoord;
	}

	public int getLocation(int direction) {
		return location[direction];
	}

	public float getSpeed(int direction) {
		return speed[direction];
	}

	public void setSpeed(int direction, float newspeed) {
		speed[direction] = newspeed;
	}


}