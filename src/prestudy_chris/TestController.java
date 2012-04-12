package prestudy_chris;

	public class TestController{

	    public static void main(String str[]) {
	    	PMgnt pm = new PMgnt(200,200,200,1);
			pm.printParticleLocation();
			
			TestParticleGUI t1 = new TestParticleGUI(pm,200);
			t1.setSize(500,500);
			t1.setVisible(true);
			
//			for(Particle p :pm.particlesystem){
//				p.setLocation(0, p.getLocation(0)+100);
//				p.setLocation(1, p.getLocation(1)+100);
//			}
//			t1.setParticleMgnt(pm);
//			t1.repaint();
			
	    }
}
