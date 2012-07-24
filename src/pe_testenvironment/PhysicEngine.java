package pe_testenvironment;


/**
 * 
 * @author eifinger
 *
 */
public class PhysicEngine {
	
	/**
	 * Is used to slow the particles down. 
	 * The higher Absorb the more the Particles will be slowed down
	 */
	private final double ABSORB=1;
	/**
	 * Used in physic equations, represents the gravitational constant of every object
	 */
	private final double GRAVITY=9.81;
	/**
	 * Used in physic equations, like strength of a spring, 
	 * the higher Elasticity, the faster the Particle will be accelerated
	 */
	private final double ELASTICITY=0.9;
	/**
	 * Determines the minimum velocity for a particle,
	 * if a Particle has a velocity smaller than this value it will be set to zero
	 */
	private final double QUANTUM=1;
	
	/**
	 * 
	 */
	
	private Particle[] pm;
	private int max_x;
	private int max_y;
	
	
	PhysicEngine(Particle[] pm,int x, int y){
		this.pm=pm;
		this.max_x=x;
		this.max_y=y;
	}
	
	/**
	 * Creates a 2 dimensional Matrix which represents how the particles push/pull each other
	 * It stores acceleration vectors (x,y)
	 * Each Particle is checked with every other Particle, Distance is calculated and depending
	 * on distance either gravitation or repulsion is invoked.
	 * After the completion of one row the absolute vector for the particle is calculated in a matter
	 * (P1xP2)+(P1xP3)+...+(P1xPn) = P1_absolute
	 * Afterwards this Particle is accelerated, absorption applied and quantumcheck performed to stop 
	 * a possible minor shiver of the Sleimi. Before the Particle is set to its new location the 
	 * collisionDetection checks if it would go past the borders, if so its velocity direction is negated.
	 * @return 
	 */
	public Particle[] run(Particle[] matrix){
		int t;
		Particle[] result = new Particle[pm.length];//Create Matrix
		for (t=0; t<matrix.length; t++) {
		      matrix[t].setLocation(matrix[t].getLocation(0) + matrix[t].getSpeed(0), matrix[t].getLocation(1) + matrix[t].getSpeed(1)); // set up input to diffeqs
		    }
		double[] new_vector = new double[2];//new acceleration vector for a particle summarisation of tmp_vector
		double[] tmp_vector = new double[2];//acceleration vector between 2 particles
		int p=0;//Current Particle
		while(p<matrix.length){
			int i=0;//Particle for comparison
			double r[]; //Distance/Radius
			new_vector = new double[2];
/*			new_vector[0]=0;
			new_vector[1]=0;
*/			while(i<matrix.length){
				if (i==p){//Don't do anything if compared with itself, array is initialised with zero, should be fine
					i++;
				}else{
					r=matrix[p].getDistance(matrix[i]);
						if(r[2]==0){//TODO BUG!
							i++;
						}else{
						tmp_vector=get_Spring(r);
						new_vector[0]+=tmp_vector[0];
						new_vector[1]+=tmp_vector[1];
						
						i++;
					}}
				}
			result[p][0]=new_vector[0];
			result[p][1]=new_vector[1];
			p++;
			}
		return result;
		}


	private double[] get_Spring(double[] r) {
		double[] a = new double[2];
		double f = (0.3/0.5)*(r[2] - 100)/r[2];
		System.out.println(f);
		a[0] = f*r[0]*0.5;
		a[1] = f*r[1]*0.5;
		return a;
	}
	
	public void step() {
		double stepSize = 0.05;
		Particle[] tmp = pm;//Create Matrix
		Particle [] matrix1;
		Particle [] matrix2;
		Particle [] matrix3;
		Particle [] matrix4;
	    int i;
	    matrix1 = run(pm);
	    for (i=0; i<pm.length; i++) {
	      tmp[i].setLocation((pm[i].getLocation(0)+matrix1[i][0]*stepSize/2),(pm[i].getLocation(1)+matrix1[i][1]*stepSize/2)); // set up input to diffeqs
	    }
	    matrix2 = run(tmp);
	    for (i=0; i<pm.length; i++) {
		      tmp[i].setLocation((pm[i].getLocation(0)+matrix2[i][0]*stepSize/2),(pm[i].getLocation(1)+matrix2[i][1]*stepSize/2)); // set up input to diffeqs
		    } // set up input to diffeqs
	    matrix3 = run(tmp);
	    for (i=0; i<pm.length; i++) {
		      tmp[i].setLocation((pm[i].getLocation(0)+matrix3[i][0]*stepSize/2),(pm[i].getLocation(1)+matrix3[i][1]*stepSize/2)); // set up input to diffeqs
		    }
	    matrix4 = run(tmp);
	    for (i=0; i<pm.length; i++) {
		      tmp[i].setLocation((pm[i].getLocation(0)+matrix4[i][0]*stepSize/2),(pm[i].getLocation(1)+matrix4[i][1]*stepSize/2)); // set up input to diffeqs
		    }
	    for(i=0; i<pm.length;i++){
	        pm[i].setLocation(pm[i].getLocation(0)+(matrix1[i][0]+2*matrix2[i][0]+2*matrix3[i][0]+matrix4[i][0])*stepSize/6, pm[i].getLocation(1)+(matrix1[i][1]+2*matrix2[i][1]+2*matrix3[i][1]+matrix4[i][1])*stepSize/6);
	    }
	  }

	/**
	 * Applies acceleration on velocity, applies Absorption, applies velocity on location
	 * @param particle
	 * @param a
	 */
	private void systemIteration(double[][] matrix, int length) {
		/**
		 * Counts up if Quantumcheck was true, used to check whether there is any movement or
		 * an equilibrium is reached
		 */
		int standcount=0;
		
		int p=0;
		while(p<length){
		
		//Set velocity
		pm[p].setSpeed(pm[p].getSpeed(0)+matrix[p][0], pm[p].getSpeed(1)+matrix[p][1]);
		//Quantum Check
		/*if(!quantumCheck(pm[p]))*/{
			//Collision Detection
			collisionDetection(max_x,max_y,pm[p]);
			//while(innerRadDetection(pm, p));
			//Set location
			double x=pm[p].getSpeed(0);
			double y=pm[p].getSpeed(1);
			pm[p].setLocation(pm[p].getLocation(0)+x*0.005,pm[p].getLocation(1)+y*0.005);
		}/*else{
			pm[p].setSpeed(0, 0);
			standcount++;
		}*/
		p++;
		}
	}

	

	/**
	 * Checks for a velocity<QUANTUM and sets the velocity 0 if true
	 * @param p Particle
	 * @return true if x and y velocity have been zero'd
	 */
	private boolean quantumCheck(Particle p){
		/*boolean result1=false;
		boolean result2=false;
		if (Math.abs(p.getSpeed(0))<QUANTUM){//Check both velocities or seperately??
			p.setSpeed(0, p.getSpeed(1));
			result1=true;
		}
		if (Math.abs(p.getSpeed(1))<QUANTUM){//Check both velocities or seperately??
			p.setSpeed(p.getSpeed(0), 0);
			result2=true;
		}
		if(result1&&result2){
			return true;
		}else{
			return false;
		}*/
		
		 //Alternative quantumCheck
		 int v=(int) Math.sqrt(p.getSpeed(0)*p.getSpeed(0)+p.getSpeed(1)*p.getSpeed(1));
		 if(v<QUANTUM){
		 	System.out.println("TRUE Quantum v = " + Integer.toString(v));
		 	return true;
		 }else{
			System.out.println("FALSE Quantum v = " + Integer.toString(v));
		 	return false;
		 }
		 
		 
		
	}
	
	/**
	 * FOR DEBUG
	 * Checks for Collision with given Max ranges and zero and negates velocity if Collision detected
	 * @param x Maximum x Coordinate
	 * @param y Maximum y Coordinate
	 * @param p Particle
	 */
	private void collisionDetection(int x, int y, Particle p){
		if(p.getLocation(0)+Math.round(p.getSpeed(0))>x || p.getLocation(0)+Math.round(p.getSpeed(0))<0){
			p.setSpeed(-p.getSpeed(0),p.getSpeed(1));
		}
		if(p.getLocation(1)+Math.round(p.getSpeed(1))>y || p.getLocation(1)+Math.round(p.getSpeed(1))<0){
			p.setSpeed(p.getSpeed(0), -p.getSpeed(1));
		}
	}
	
	private double[][] createMatrix(Particle[] pm){
		double[][] Matrix = new double[pm.length][2];//2-dimensional Matrix storing acceleration vectors
		return Matrix;
	}
	/**
	 * CURRENTLY NOT USED, WILL BE ACTIVATED IF SPARE TIME IS AVAILABLE
	 * Checks if the desired new Location of Particle p collides with inner rad of another particle
	 * @param pm
	 * @param p
	 * @return true if a collision was detected
	 */
	private boolean innerRadDetection(Particle[] pm, int p){
		boolean result=false;
		int i=0;
		int x_loc=0;//Desired new x-Location
		int y_loc=0;//Desired new y-Location
		Particle tmp;
		while(i<pm.length){
			x_loc=(int) (pm[p].getLocation(0)+pm[p].getSpeed(0));
			y_loc=(int) (pm[p].getLocation(1)+pm[p].getSpeed(1));
			tmp= new Particle(x_loc, y_loc);
			if(tmp.getDistance(pm[i])[2]<pm[i].INNER_RAD*2){
				pm[p].setSpeed(-pm[p].getSpeed(0)/2, -pm[p].getSpeed(1)/2);
				result = true;
			}
			i++;
		}
		return result;
	}
	
	

}
