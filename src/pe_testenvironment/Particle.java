package pe_testenvironment;

public class Particle {
	public final int INNER_RAD = 5;//Lieber beim erstellen im PMgnt festlegen?
	public final int OUTER_RAD = 20;//Lieber beim erstellen im PMgnt festlegen?
	private double[] location = new double[2];
	private double[] speed = {0, 0};
	private boolean locked = false;

	Particle(double pos_x, double pos_y) {
		this.location[0] = pos_x;
		this.location[1] = pos_y;
	}

	public void setLocation(double x_loc, double y_loc) {
		this.location[0] = x_loc;
		this.location[1] = y_loc;
	}

	public double getLocation(int direction) {
		return location[direction];
	}

	public double getSpeed(int direction) {
		return speed[direction];
	}

	public void setSpeed(double x_speed, double y_speed) {
		this.speed[0] = x_speed;
		this.speed[1] = y_speed;
	}
	
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public boolean isLocked() {
		return locked;
	}

	/**
	 * Calculates distance between two particles
	 * @param p1 Particle
	 * @param p2 Particle
	 * @return array of integer containing 0=Vector_x, 1=Vector_y 2=Distance
	 */
	public double[] getDistance(Particle p){
		double[] result = new double[3];
		result[0]=(p.getLocation(0)-this.getLocation(0));
		result[1]=(p.getLocation(1)-this.getLocation(1));
		result[2]=(int) Math.sqrt((result[0]*result[0])+(result[1]*result[1]));
		return result;
	}
	
	public double getDistance(double x, double y){
		double[] result = new double[3];
		result[0]=(this.getLocation(0)-x);
		result[1]=(this.getLocation(1)-y);
		result[2]=(double) Math.sqrt((result[0]*result[0])+(result[1]*result[1]));
		return result[2];
	}


}