package newOne;

public class Particle {
	public final int INNER_RAD = 5;//Lieber beim erstellen im PMgnt festlegen?
	public final int OUTER_RAD = 10;//Lieber beim erstellen im PMgnt festlegen?
	private int[] location = new int[2];
	private float[] speed = {0.0f, 0.0f};

	Particle(int pos_x, int pos_y) {
		this.location[0] = pos_x;
		this.location[1] = pos_y;
	}

	public void setLocation(int x_loc, int y_loc) {
		this.location[0] = x_loc;
		this.location[1] = y_loc;
	}

	public int getLocation(int direction) {
		return location[direction];
	}

	public float getSpeed(int direction) {
		return speed[direction];
	}

	public void setSpeed(float x_speed, float y_speed) {
		this.speed[0] = x_speed;
		this.speed[1] = y_speed;
	}


}