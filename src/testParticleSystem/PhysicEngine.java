package testParticleSystem;


public class PhysicEngine {
	
	
	private final float ABSORB=0.2f;//Absorption
	private final float GRAVITY=3.0f;//Physical Gravity constant
	//private final float ELASTICITY=0.9f;//Elasticity
	private final float QUANTUM=5f;//Quantum
	private final float CONSTANT=1000.0f;//Constant for Physic Functions
	private Particle[] pm;
	private int max_x;
	private int max_y;
	
	PhysicEngine(Particle[] pm,int x, int y){
		this.pm=pm;
		this.max_x=x;
		this.max_y=y;
	}
	
	public void run(){
		float[][][] Matrix = createMatrix(pm);//Create Matrix
		int p=0;//Current Particle
		while(p<pm.length){
			int i=0;//Particle for comparison
			int r[]; //Distance/Radius
			float[] a = new float[2];
			while(i<pm.length){
				if (i==p){//Don't do anything if compared with itself, array is initialised with zero, should be fine
					i++;
				}else{
					if(Matrix[p][i][1]!=0){//If distance!=0 it was already set
						i++;
					}else{
						r=get_Distance(pm[p],pm[i]);//Get Distance
						if(r[2]<=pm[p].OUTER_RAD){//Repulsion
							a=get_Repulsion(r);//REMEMBER! pm[p]->pm[i]
							Matrix[p][i]=a;//Store acceleration Vector in Matrix
							//Reverse a and input it into Matrix for pm[i]->pm[p]
							a[0]=-a[0];
							a[1]=-a[1];
							Matrix[i][p]=a;
							i++;
						}else{
							if(r[2]>pm[p].OUTER_RAD){//Gravitation
								a=get_Gravitation(r);
								Matrix[p][i]=a;//Store acceleration Vector in Matrix
								//Reverse a and input it into Matrix for pm[i]->pm[p]
								a[0]=-a[0];
								a[1]=-a[1];
								Matrix[i][p]=a;
								i++;
							}
						}
					}
				}
			}
			float[] absoluteVector=get_absoluteVector(Matrix,p, pm.length);//Absolutvektor für p berechnen
			systemIteration(pm[p],absoluteVector);//Iterationsschritt für p, kräfte auf v auswirken lassen und dämpfen
			quantumCheck(pm[p]);//Quantumcheck für p um Stillstand zu erreichen
			p++;
		}
	}


	/**
	 * Applies acceleration on velocity, applies Absorption, applies velocity on location
	 * @param particle
	 * @param a
	 */
	private void systemIteration(Particle particle, float[] a) {
		//Set velocity
		particle.setSpeed(0, particle.getSpeed(0)+a[0]*ABSORB);
		particle.setSpeed(1, particle.getSpeed(1)+a[1]*ABSORB);
		//Collision Detection
		collisionDetection(max_x,max_y,particle);
		//Set location
		particle.setLocation(0, particle.getLocation(0)+Math.round(particle.getSpeed(0)));
		particle.setLocation(1, particle.getLocation(1)+Math.round(particle.getSpeed(1)));
	}


	/**
	 * Takes the Matrix of interactive Forces and generates the absolute vector for particle p
	 * 
	 * @param matrix
	 * @param p
	 * @param length
	 * @return The absolute x,y Vector for a particle
	 */
	private float[] get_absoluteVector(float[][][] matrix,int p,int length) {
		int i=0;
		float[] a= new float[2];//absoluteVector
		while(i<length){
			a[0]=a[0]+matrix[p][i][0];//add xVector
			a[1]=a[1]+matrix[p][i][1];//add yVector
			i++;
		}
		return a;
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
		a[0]=(GRAVITY*(CONSTANT/(tmp)));//1 in the numerator can't be right
		if(r[0]<0){a[0]*=-1;}
		tmp = (r[1]*r[1]);
		a[1]=(GRAVITY*(CONSTANT/(tmp)));//1 in the numerator can't be right
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
	private float[] get_Repulsion(int r[]){//Add constant, and make r much smaller
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
	 */
	private void quantumCheck(Particle p){
		if (p.getSpeed(0)<QUANTUM){//Check both velocities or seperately??
			p.setSpeed(0, 0);
		}
		if (p.getSpeed(1)<QUANTUM){//Check both velocities or seperately??
			p.setSpeed(1, 0);
		}
		
	}
	/**
	 * Creates a Matrix for storing the interactive forces between all particles
	 * @param pm Particlemanagment
	 * @return A 2 dimensional Matrix of size pm.length storing arrays of length 2
	 */
	private float[][][] createMatrix(Particle[] pm){
		float[][][] Matrix = new float[pm.length][pm.length][2];//3-dimensional Matrix storing acceleration vectors
		return Matrix;
	}
	/**
	 * Calculates distance between two particles
	 * @param p1 Particle
	 * @param p2 Particle
	 * @return 3-dimensional array of integer containing 0=Vector_x, 1=Vector_y 2=Distance
	 */
	private int[] get_Distance(Particle p1, Particle p2){
		int[] result = new int[3];
		result[0]=(p1.getLocation(0)-p2.getLocation(0));
		result[1]=(p1.getLocation(1)-p2.getLocation(1));
		result[2]=(int) Math.sqrt((result[0]*result[0])+(result[1]*result[1]));
		return result;
	}
	/**
	 * Checks for Collision with given Max ranges and zero and negates velocity if Collision detected
	 * @param x Maximum x Coordinate
	 * @param y Maximum y Coordinate
	 * @param p Particle
	 */
	private void collisionDetection(int x, int y, Particle p){
		if(p.getLocation(0)+Math.round(p.getSpeed(0))>x || p.getLocation(0)+Math.round(p.getSpeed(0))<0){
			p.setSpeed(0, -p.getSpeed(0));
		}
		if(p.getLocation(1)+Math.round(p.getSpeed(1))>y || p.getLocation(1)+Math.round(p.getSpeed(1))<0){
			p.setSpeed(1, -p.getSpeed(1));
		}
	}
	
	

}
