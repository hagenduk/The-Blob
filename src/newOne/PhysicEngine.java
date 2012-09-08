package newOne;
/**
 * 
 * @author eifinger
 * Creates a Instance of a Physic Engine which is used for making the Blob "wobble".
 * It computes the forces between Particles and moves them.
 */
public class PhysicEngine {
	
	/**
	 * The Particle System to be worked with
	 */
	private Particle[] pm;
	/**
	 * Helper Particle Management used for creation of Helper Particle System
	 */
	private PMgnt tmp_pmgnt;
	/**
	 * Helper Particle System used for computation
	 */
	private Particle[] tmp_pm;
	
	
	/*
	 * Physical Constants can be changed via getter/setter methods
	 */
	
	private double SPRINGCONSTANT = 6;
	private double MASS = 0.5;
	private double LEN = 100;
	private double GRAVITY = 0;
	private double DAMPING = 0.1;
	/*
	 * End Physical Constants
	 */
	
	/**
	 * Applies forces to particles and step() computates the next step in the simulation
	 * The method modifyObjects() applies the computed locations and speeds to the pm
	 */
	public PhysicEngine(Particle[] pm) {
		this.pm = pm;
	}
	
	/**
	 * Runge Kutta Algorithm, computes the most accurate forecast of position and speed for 4 iterations
	 */
	public void step(double stepSize) {
		/*
		 * Reset the Helper pm
		 */
		tmp_pmgnt = new PMgnt(20,600,600,10);
		tmp_pmgnt.createParticle();
		tmp_pm = tmp_pmgnt.particlesystem;
		int i;
		int N = pm.length;
		/*
		 * Create Helper Particle Systems for each iteration and initialize them
		 */
		PMgnt pm0 = new PMgnt(N,600,600,10);
		PMgnt pm1 = new PMgnt(N,600,600,10);
		PMgnt pm2 = new PMgnt(N,600,600,10);
		PMgnt pm3 = new PMgnt(N,600,600,10);
		PMgnt pm4 = new PMgnt(N,600,600,10);
		
		pm0.createParticle();
		pm1.createParticle();
		pm2.createParticle();
		pm3.createParticle();
		pm4.createParticle();
		
		Particle[] inp = pm0.particlesystem;
		Particle[] k1 = pm1.particlesystem;
		Particle[] k2 = pm2.particlesystem;
		Particle[] k3 = pm3.particlesystem;
		Particle[] k4 = pm4.particlesystem;

		evaluate(pm, k1); // evaluate at time t
		for (i = 0; i < N; i++){
			inp[i].setLocation(pm[i].getLocation(0) + k1[i].getLocation(0) * stepSize / 2, pm[i].getLocation(1) + k1[i].getLocation(1) * stepSize / 2);
			inp[i].setSpeed(pm[i].getSpeed(0) + k1[i].getSpeed(0) * stepSize / 2, pm[i].getSpeed(1) + k1[i].getSpeed(1) * stepSize / 2);
		}
		evaluate(inp, k2); // evaluate at time t+stepSize/2
		for (i = 0; i < N; i++){
			inp[i].setLocation(pm[i].getLocation(0) + k2[i].getLocation(0) * stepSize / 2, pm[i].getLocation(1) + k2[i].getLocation(1) * stepSize / 2);
			inp[i].setSpeed(pm[i].getSpeed(0) + k2[i].getSpeed(0) * stepSize / 2, pm[i].getSpeed(1) + k2[i].getSpeed(1) * stepSize / 2);
		}
		evaluate(inp, k3); // evaluate at time t+stepSize/2
		for (i = 0; i < N; i++){
			inp[i].setLocation(pm[i].getLocation(0) + k3[i].getLocation(0) * stepSize, pm[i].getLocation(1) + k3[i].getLocation(1) * stepSize);
			inp[i].setSpeed(pm[i].getSpeed(0) + k3[i].getSpeed(0) * stepSize, pm[i].getSpeed(1) + k3[i].getSpeed(1) * stepSize);
		}
		evaluate(inp, k4); // evaluate at time t+stepSize
		/*
		 *  Input the results into the tmp pm
		 */
		for (i = 0; i < N; i++){
			 if (!pm[i].isLocked()){
			tmp_pm[i].setLocation(
					pm[i].getLocation(0) + //(new)x_loc = (old)x_loc + (k1 + 2* k2 + 2*k3 + k4)*stepSize / 6
					(k1[i].getLocation(0) + 
							2 * k2[i].getLocation(0) + 
							2 * k3[i].getLocation(0) + 
					k4[i].getLocation(0)) 
					* stepSize / 6,
					pm[i].getLocation(1) + //(new)y_loc = (old)y_loc + (k1 + 2* k2 + 2*k3 + k4)*stepSize / 6
					(k1[i].getLocation(1) + 
							2 * k2[i].getLocation(1) + 
							2 * k3[i].getLocation(1) + 
					k4[i].getLocation(1)) 
					* stepSize / 6);
			
			tmp_pm[i].setSpeed(
					pm[i].getSpeed(0) + //(new)x_speed = (old)x_speed + (k1 + 2* k2 + 2*k3 + k4)*stepSize / 6
					(k1[i].getSpeed(0) + 
							2 * k2[i].getSpeed(0) + 
							2 * k3[i].getSpeed(0) + 
					k4[i].getSpeed(0)) 
					* stepSize / 6,
					pm[i].getSpeed(1) + //(new)y_speed = (old)y_speed + (k1 + 2* k2 + 2*k3 + k4)*stepSize / 6
					(k1[i].getSpeed(1) + 
							2 * k2[i].getSpeed(1) + 
							2 * k3[i].getSpeed(1) + 
					k4[i].getSpeed(1)) 
					* stepSize / 6);
			 }
		}

	}
	/**
	 * Evaluates all changes in position and speed for all particles and writes them into the change pm
	 * @param inp
	 * @param change
	 */
	public void evaluate(Particle[] inp, Particle[] change) {
		for (int i = 0; i < inp.length; i++) {
				change[i].setLocation(inp[i].getSpeed(0), inp[i].getSpeed(1));// derivative of position U is velocity V
				double x=0;
				double y=0;
					for (int t = 0; t < inp.length; t++) {
						if ((i != t)) {
							double xx = inp[t].getLocation(0) - inp[i].getLocation(0);  // x distance between objects
				            double yy = inp[t].getLocation(1) - inp[i].getLocation(1);  // y distance betw objects
				            double len = Math.sqrt(xx*xx + yy*yy);  // total distance between objects
							double f = (SPRINGCONSTANT / MASS) * (len - LEN) / len;// Springforce
							x += f * xx;
							y += -GRAVITY + f * yy;
						}	
				}
					if (DAMPING != 0){
						x -= (DAMPING/MASS)*inp[i].getSpeed(0);
						y -= (DAMPING/MASS)*inp[i].getSpeed(1);
					}
				change[i].setSpeed(x,y);
			}
		}
	/**
	 * Applies the computed new positions and speeds to the actual pm
	 */
	public void modifyObjects() {
		for (int i = 0; i < pm.length; i++) {
			pm[i].setLocation((tmp_pm[i].getLocation(0)), (tmp_pm[i].getLocation(1) ));
			pm[i].setSpeed((tmp_pm[i].getSpeed(0)), (tmp_pm[i].getSpeed(1)));
		}
	}

	public double getSPRINGCONSTANT() {
		return SPRINGCONSTANT;
	}
	public void setSPRINGCONSTANT(double sPRINGCONSTANT) {
		SPRINGCONSTANT = sPRINGCONSTANT;
	}
	public double getMASS() {
		return MASS;
	}
	public void setMASS(double mASS) {
		MASS = mASS;
	}
	public double getLEN() {
		return LEN;
	}
	public void setLEN(double lEN) {
		LEN = lEN;
	}
	public double getGRAVITY() {
		return GRAVITY;
	}
	public void setGRAVITY(double gRAVITY) {
		GRAVITY = gRAVITY;
	}
	public double getDAMPING() {
		return DAMPING;
	}
	public void setDAMPING(double dAMPING) {
		DAMPING = dAMPING;
	}

}
