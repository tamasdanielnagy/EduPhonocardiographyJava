package signal;

/**
 * Functions for number arrays.
 * 
 * @author Nagy Tamás
 *
 */
public abstract class NumberArray {

	
	public static int intArrayMax(int[] iarr) {
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < iarr.length; i++)
			if (iarr[i] > max)
				max = iarr[i];
		return max;
	}
}
