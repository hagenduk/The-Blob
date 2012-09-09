package newOne;

//this is the particlemanagement class
public class PMgnt {
	
	private int system_size = 0;
	public Particle[] particlesystem;
	public Particle[] particlesystem2; 
	private int pos_x = 0;
	private int pos_y = 0;
	private int area_x = 0;
	private int area_y = 0;
	private int rad_distance = 0;
	private int particleCounter = 0;
	
	public PMgnt(int system_size, int area_x, int area_y, int rad_distance){
		this.system_size = system_size;
		this.area_x = area_x;
		this.area_y = area_y;
		this.rad_distance = rad_distance;
		particlesystem = new Particle[this.system_size];
		createParticle();
		particleCounter = 0;
	}
	
	
	//this method is not released in this version because of serious sideeffects - it should have inserted particles when chemie is selected
	public void insertparticle(int number){
		/*//Helper particle system
		particlesystem2 = new Particle[particlesystem.length+number];
		//loading old system in helper one
		for(int i=0; i<particlesystem.length; i++ ){
			particlesystem2[i]=particlesystem[i];
		}
		//add new particles
		for(int i=0; i<number; i++ ){
			pos_x = (int) ((Math.random()+1)*area_x);
			pos_y = (int) ((Math.random()+1)*area_y);
			particlesystem2[particlesystem.length+i-1] = new Particle(pos_x,pos_y);	
		}
		//switch the arrays
		particlesystem=particlesystem2;
		//create new physicsarray
		double[] newvars= new double[particlesystem.length*4];
		for(int i=0; i>number; i+=4){
			for(int j=0; j<4; j++){
				if(j==0){newvars[i+j]=particlesystem[i].getLocation(0);}
				if(j==1){newvars[i+j]=particlesystem[i].getLocation(1);}
				if(j==2){newvars[i+j]=particlesystem[i].getSpeed(0);}
				if(j==3){newvars[i+j]=particlesystem[i].getSpeed(1);}	
			}
		}
		pe.vars = newvars;
		//new calc array
		boolean[] calc = new boolean[particlesystem.length];
		for (int i = 0; i < calc.length; i++) {
			calc[i] = true;
		}
		pe.calc=calc;*/
	}
	
	//this method is not released in this version because of serious sideeffects - it should remove particles when homeopathy is selected
	public void removeparticle(int number){
		//prevent errors
		/*if(particlesystem.length>4){
			//Helper particle system
			particlesystem2 = new Particle[particlesystem.length-number];
			//loading old system in helper one
			for(int i=particlesystem.length-1; i>number-1; i-- ){
				particlesystem2[i-number]=particlesystem[i];
			}
			//switch the arrays
			particlesystem=particlesystem2;
		}
		//create new physicsarray
				double[] newvars= new double[particlesystem.length*4];
				for(int i=0; i>number; i+=4){
					for(int j=0; j<4; j++){
						if(j==0){newvars[i+j]=particlesystem[i].getLocation(0);}
						if(j==1){newvars[i+j]=particlesystem[i].getLocation(1);}
						if(j==2){newvars[i+j]=particlesystem[i].getSpeed(0);}
						if(j==3){newvars[i+j]=particlesystem[i].getSpeed(1);}	
					}
				}
				pe.vars = newvars;
				//new calc array
				boolean[] calc = new boolean[particlesystem.length];
				for (int i = 0; i < calc.length; i++) {
					calc[i] = true;
				}*/
		
	}

//creates a new particle	
public void createParticle() {
		boolean done = false;
		pos_x = (int) ((Math.random()+1)*area_x);
		pos_y = (int) ((Math.random()+1)*area_y);
		particlesystem[0] = new Particle(pos_x,pos_y);
		particleCounter++;
		for(int i = 1; i < system_size; i++){
			while (!done) {
				pos_x = (int) ((Math.random()+1)*area_x);
				pos_y = (int) ((Math.random()+1)*area_y);
				 if(isNeighbour()){
						particlesystem[i] = new Particle(pos_x,pos_y);
						if(i==system_size-2){
							particlesystem[i].kind=2;
						}
						else if(i==system_size-1){
							particlesystem[i] = new Particle(pos_x,pos_y);
							particlesystem[i].kind=1;
						}
						particleCounter++;
						done = true;
				 }
			}
			done = false;
		}
	}

//are particles next to each other?
	private boolean isNeighbour() {
		boolean check = false;
		int resultVector_y = 0;
		int resultVector_x = 0;
		int distance = 0;
		while(!check){
			for (int i = 0; i < particleCounter; i++) {
				resultVector_x = (int) (pos_x - particlesystem[i].getLocation(0));
				resultVector_y = (int) (pos_y - particlesystem[i].getLocation(1));
				distance = calculateDistance(resultVector_x, resultVector_y);
				if (distance <= rad_distance) {
					check = true;
					break;
				}
			}
			pos_x = (int) ((Math.random()+1)*area_x);
			pos_y = (int) ((Math.random()+1)*area_y);
		}
		return check;
	}

	//distance calculation
	public int calculateDistance(int resultVector_x, int resultVector_y) {
		int distance = (int) Math.sqrt((resultVector_y*resultVector_x)+(resultVector_y*resultVector_y));
		return distance;
	}

	//just for testing
	public void printParticleLocation() {
		int i = 1;
		for(Particle p : particlesystem){
			System.out.println("X_LOCATION: " + p.getLocation(0) + " Particle:"+ i);
			System.out.println("Y_LOCATION: " + p.getLocation(1) + " Particle:"+ i);
			System.out.println();
			i++;
		}
	}
}
