package calculation;

/**
 * Random generator class.
 * 
 * @author Nagy Tamas
 *
 */
public abstract class RandGenerator {
	
	public static int randomInt(int min, int max) {
		return min + (int) Math.floor(Math.random() * (max - min + 1));
	}
	
	public static double randomDouble(double min, double max) {
		return min + (Math.random() * (max - min));
	}
	
	public static float randomFloat(float min, float max) {
		return (float) (min + (Math.random() * (max - min)));
	}
}
