package newOne;

/**
 * This class is implemented for additional helping functions, during the development.
 * As like print function fpr print all particle speeds..
 * @author CF
 *
 */

public class HelperClass {
	private PMgnt pm;

	public HelperClass(PMgnt pm){
		this.pm = pm;
	}
	
	public void printAllSpeed(){
		int i = 0;
		for(Particle p : pm.particlesystem){
			System.out.println("Particle: " + i + " Speed-x: " + p.getSpeed(0) + " Speed-y: " + p.getSpeed(1));
		}
	}

}
