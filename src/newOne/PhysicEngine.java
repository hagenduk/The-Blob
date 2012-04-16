package newOne;

/**
 * 
 * @author eifinger
 *
 */
public class PhysicEngine {
	
	/**
	 * Is used to slow the particles down. 
	 * The smaller Absorb the more the Particles will be slowed down
	 */
	private final float ABSORB=0.9f;
	/**
	 * Used in physic equations, represents the gravitational constant of every object
	 */
	private final float GRAVITY=3.0f;
	/**
	 * Used in physic equations, like strength of a spring, 
	 * the higher Elasticity, the faster the Particle will be accelerated
	 */
	//private final float ELASTICITY=0.9f;
	/**
	 * Determines the minimum velocity for a particle,
	 * if a Particle has a velocity smaller than this value it will be set to zero
	 */
	private final float QUANTUM=0.2f;
	/**
	 * Used in physic equations, the higher constant the faster the particle
	 */
	private final float CONSTANT=4000.0f;
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
	 */
	public void run(){
		float[] new_vector = new float[2];//new acceleration vector for a particle summarisation of tmp_vector
		float[] tmp_vector = new float[2];//acceleration vector between 2 particles
		int p=0;//Current Particle
		while(p<pm.length){
			int i=0;//Particle for comparison
			int r[]; //Distance/Radius
			new_vector = new float[2];//acceleration vector
			while(i<pm.length){
				if (i==p){//Don't do anything if compared with itself, array is initialised with zero, should be fine
					i++;
				}else{
					r=pm[p].getDistance(pm[i]);
					if(r[2]<=pm[p].OUTER_RAD){//Repulsion
						tmp_vector=get_Repulsion(r);
						new_vector[0]+=tmp_vector[0];
						new_vector[1]+=tmp_vector[1];
						i++;
					}else{
						if(r[2]>pm[p].OUTER_RAD){//Gravitation
							tmp_vector=get_Gravitation(r);
							new_vector[0]+=tmp_vector[0];
							new_vector[1]+=tmp_vector[1];
							i++;
						}
					}
				}
			}
			systemIteration(pm[p],new_vector);
			p++;
		}	
		}


	/**
	 * Applies acceleration on velocity, applies Absorption, applies velocity on location
	 * @param particle
	 * @param a
	 */
	private void systemIteration(Particle particle, float[] a) {
		//For Debugging
		System.out.println(a[0]);
		System.out.println(a[1]);
		System.out.println(particle.getLocation(0));
		//Set velocity
		particle.setSpeed(particle.getSpeed(0)+a[0]*ABSORB, particle.getSpeed(1)+a[1]*ABSORB);
		//Quantum Check
		if(!quantumCheck(particle)){
			//Collision Detection
			collisionDetection(max_x,max_y,particle);
			//Set location
			particle.setLocation(particle.getLocation(0)+Math.round(particle.getSpeed(0)), 
					particle.getLocation(1)+Math.round(particle.getSpeed(1)));
		}
	}



	/**
	 * Calculates the force between two particles of distance r
	 * a=-(m³/kg*s²) * kg²/m²
	 * a= kg*m/s² kg=1
	 * a=m/s²
	 * @param r[] array containing distances in x,y direction
	 * @return float representation in x and y directions of the acceleration between two particles
	 */
	private float[] get_Gravitation(int r[]){
		float[] a = new float[2];
		float tmp = r[0]*r[0];
		a[0]=(GRAVITY*(CONSTANT/(tmp)));
		if(r[0]<0){a[0]*=-1;}
		tmp = (r[1]*r[1]);
		a[1]=(GRAVITY*(CONSTANT/(tmp)));
		if(r[1]<0){a[1]*=-1;}
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
	private float[] get_Repulsion(int r[]){
		float[] a= new float[2];
		float tmp = (r[0]*r[0]);
		a[0]=CONSTANT/tmp;
		if(r[0]>0){a[0]*=-1;}
		tmp = (r[1]*r[1]);
		a[1]=CONSTANT/tmp;
		if(r[0]>0){a[0]*=-1;}
		return a;
	}
	

	/**
	 * Checks for a velocity<QUANTUM and sets the velocity 0 if true
	 * @param p Particle
	 * @return true if x and y velocity have been zero'd
	 */
	private boolean quantumCheck(Particle p){
		boolean result1=false;
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
		}
		
	}
	
	/**
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
	
	

}
