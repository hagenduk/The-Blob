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
	private final int ABSORB=2;
	/**
	 * Used in physic equations, represents the gravitational constant of every object
	 */
	private final int GRAVITY=3;
	/**
	 * Used in physic equations, like strength of a spring, 
	 * the higher Elasticity, the faster the Particle will be accelerated
	 */
	//private final float ELASTICITY=0.9f;
	/**
	 * Determines the minimum velocity for a particle,
	 * if a Particle has a velocity smaller than this value it will be set to zero
	 */
	private final int QUANTUM=1;
	/**
	 * Used in physic equations, the higher constant the faster the particle
	 */
	private final int CONSTANT=2000;
	
	/**
	 * 
	 */
	private boolean equilibrium;
	
	private Particle[] pm;
	private int max_x;
	private int max_y;
	
	public boolean isEquilibrium() {
		return equilibrium;
	}
	
	PhysicEngine(Particle[] pm,int x, int y){
		this.pm=pm;
		this.max_x=x;
		this.max_y=y;
		this.equilibrium=false;
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
	 */
	public void run(){
		int[][] matrix = createMatrix(pm);//Create Matrix
		int[] new_vector = new int[2];//new acceleration vector for a particle summarisation of tmp_vector
		int[] tmp_vector = new int[2];//acceleration vector between 2 particles
		int p=0;//Current Particle
		while(p<pm.length){
			int i=0;//Particle for comparison
			int r[]; //Distance/Radius
			new_vector = new int[2];
/*			new_vector[0]=0;
			new_vector[1]=0;
*/			while(i<pm.length){
				if (i==p){//Don't do anything if compared with itself, array is initialised with zero, should be fine
					i++;
				}else{
					r=pm[p].getDistance(pm[i]);
						if(r[2]==0){//TODO BUG!
							i++;
						}else{
							//Hit
						if(r[2]<=pm[0].OUTER_RAD) {
							int kin[] = new int[2];
							kin[0] = Math.abs(pm[p].getSpeed(0) + pm[i].getSpeed(0));
							kin[1] = Math.abs(pm[p].getSpeed(1) + pm[i].getSpeed(1));
							
						}
						
//						System.out.println("Gravitation");
						tmp_vector=get_Gravitation(r);
						new_vector[0]+=tmp_vector[0];
						new_vector[1]+=tmp_vector[1];
						
						i++;
					}}
				}
			matrix[p][0]=new_vector[0];
			matrix[p][1]=new_vector[1];
			p++;
			}
			
		systemIteration(matrix, pm.length);
		}


	/**
	 * Applies acceleration on velocity, applies Absorption, applies velocity on location
	 * @param particle
	 * @param a
	 */
	private void systemIteration(int[][] matrix, int length) {
		/**
		 * Counts up if Quantumcheck was true, used to check whether there is any movement or
		 * an equilibrium is reached
		 */
		int standcount=0;
		
		int p=0;
		while(p<length){
		//Max velocity
			
		int orad=pm[0].OUTER_RAD;
		if(matrix[p][0]>orad){matrix[p][0]=orad;}
		if(matrix[p][0]<-orad){matrix[p][0]=-orad;}
		if(matrix[p][1]>orad){matrix[p][1]=orad;}
		if(matrix[p][1]<-orad){matrix[p][1]=-orad;}
		
		//Set velocity
		pm[p].setSpeed(Math.round((pm[p].getSpeed(0)+matrix[p][0])/ABSORB), Math.round((pm[p].getSpeed(1)+matrix[p][1])/ABSORB));
		//Quantum Check
		if(!quantumCheck(pm[p])){
			//Collision Detection
			collisionDetection(max_x,max_y,pm[p]);
			//while(innerRadDetection(pm, p));
			//Set location
			int x=pm[p].getSpeed(0);
			int y=Math.round(pm[p].getSpeed(1));
			pm[p].setLocation(pm[p].getLocation(0)+x,pm[p].getLocation(1)+y);
		}else{
			pm[p].setSpeed(0, 0);
			standcount++;
		}
		p++;
		}
		if(standcount==length){
			equilibrium=true;
		}else{
			equilibrium=false;
		}
	}



	/**
	 * Calculates the force between two particles of distance r
	 * a=-(m³/kg*s²) * kg²/m²
	 * a= kg*m/s² kg=1
	 * a=m/s²
	 * F=-G*MASS
	 * @param r[] array containing distances in x,y direction
	 * @return float representation in x and y directions of the acceleration between two particles
	 */
	private int[] get_Gravitation(int r[]){
		int[] a = new int[2];
		int dist = r[2]*r[2];
		int tmp = Math.round(r[0]/Math.abs(r[2]));
		a[0]=-GRAVITY*(CONSTANT/(dist))*tmp;
		tmp = Math.round(r[1]/Math.abs(r[2]));
		a[1]=-GRAVITY*(CONSTANT/(dist))*tmp;
		return a;
	}
	
	/*
	void get_elasticity(int r[]){
		float U=0.0f;
		U=(0.5*k*r*r);
	}
	*/
	

	/**
	 * Calculates the Repulsion a between two particles
	 * of distance r once the outer radius was passed
	 * @param r[] array containing distances in x,y direction and overall distance
	 * @return float representation of the acceleration between two particles
	 */
	private int[] get_Repulsion(int r[]){
		int[] a = new int[2];
		int orad = pm[0].OUTER_RAD/4*3;
		int dist = (r[2]-orad)*(r[2]-orad)*(r[2]-orad)*(r[2]-orad);
		int tmp = Math.round(r[0]/Math.abs(r[2]));
		a[0]=GRAVITY*(CONSTANT/dist)*tmp;
		tmp = Math.round(r[1]/Math.abs(r[2]));
		a[1]=GRAVITY*(CONSTANT/dist)*tmp;
		return a;
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
	
	private int[][] createMatrix(Particle[] pm){
		int[][] Matrix = new int[pm.length][2];//2-dimensional Matrix storing acceleration vectors
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
