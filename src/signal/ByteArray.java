package signal;


public abstract class ByteArray {

	public static final int BITS_PER_BYTE = 8;
	
	public static int averageInt(byte[] bytes, int numBytesPerInt) {
		int ret = 0;
		byte[] temp = new byte[numBytesPerInt];
		for (int i = 0; i < bytes.length / numBytesPerInt; i++) {
			for (int j = 0; j < numBytesPerInt; j++) {
				temp[j] = bytes[(i * numBytesPerInt) + j];
			}
			ret += byteArrayToInt(temp);
		}
		return (int) Math.round(((float) ret) / (bytes.length / numBytesPerInt));
	}
	
	public static int averageInt(byte[] bytes, int numBytesPerInt, boolean bigEndian) {
		int ret = 0;
		byte[] temp = new byte[numBytesPerInt];
		if (bigEndian) {
			for (int i = 0; i < bytes.length / numBytesPerInt; i++) {
				for (int j = 0; j < numBytesPerInt; j++) {
					temp[j] = bytes[(i * numBytesPerInt) + j];
				}
				ret += byteArrayToInt(temp);
			}
		} else {
			for (int i = 0; i < bytes.length / numBytesPerInt; i++) {
				for (int j = 0; j < numBytesPerInt; j++) {
					temp[j] = bytes[(i * numBytesPerInt) + j];
				}
				ret += byteArrayToInt(temp, false);
			}
		}
		return (int) Math
				.round(((float) ret) / (bytes.length / numBytesPerInt));
	}
	
	public static double averageDouble(byte[] bytes, int numBytesPerInt) {
		int ret = 0;
		byte[] temp = new byte[numBytesPerInt];
		for (int i = 0; i < bytes.length / numBytesPerInt; i++) {
			for (int j = 0; j < numBytesPerInt; j++) {
				temp[j] = bytes[(i * numBytesPerInt) + j];
			}
			ret += byteArrayToInt(temp);
		}
		return ((double) ret) / (bytes.length / numBytesPerInt);
	}
	
	public static double averageDouble(byte[] bytes, int numBytesPerInt, boolean bigEndian) {
		int ret = 0;
		byte[] temp = new byte[numBytesPerInt];
		for (int i = 0; i < bytes.length / numBytesPerInt; i++) {
			for (int j = 0; j < numBytesPerInt; j++) {
				temp[j] = bytes[(i * numBytesPerInt) + j];
			}
			ret += byteArrayToInt(temp, bigEndian);
		}
		return ((double) ret) / (bytes.length / numBytesPerInt);
	}
	
	public static byte[] intToByteArray(int x, int arrayLength) {
		byte[] bytes = new byte[arrayLength];
		for (int i = 0; i < arrayLength; i++) {
			bytes[arrayLength - (i + 1)] = (byte) ((x >>> (i * BITS_PER_BYTE)) & 0xFF);
		}
		return bytes;
	}
	
	public static byte[] intArrayToByteArray(Integer[] iarr, int numBytesPerInt) {
		byte[] bytes = new byte[iarr.length * numBytesPerInt];
		byte[] temp;
		for (int i = 0; i < iarr.length; i++) {
			temp = intToByteArray(iarr[i], numBytesPerInt);
			for (int j = 0; j < numBytesPerInt; j++) {
				bytes[(i * numBytesPerInt) + j] = temp[j];
			}
		}		
		return bytes;
	}
	
	public static int byteArrayToInt(byte[] bytes) {
		int ret = 0;
		int mask;
		for (int i = bytes.length - 1; i >= 0; i--) {
			mask = 0xFF << ((bytes.length - 1) - i) * BITS_PER_BYTE;
			ret = ret | ((bytes[i] << (((bytes.length - 1) - i) * BITS_PER_BYTE)) & mask);
		}
		if (bytes[0] < 0) {
			for (int i = 0; i < 4 - bytes.length; i++) {
				ret = ret | (0xFF << ((bytes.length + i) * BITS_PER_BYTE));
			}
		}
		return ret;
	}
	
	public static int byteArrayToInt(byte[] bytes, boolean bigEndian) {
		int ret = 0;
		int mask;
		if (bigEndian) {
			for (int i = bytes.length - 1; i >= 0; i--) {
				mask = 0xFF << ((bytes.length - 1) - i) * BITS_PER_BYTE;
				ret = ret
						| ((bytes[i] << (((bytes.length - 1) - i) * BITS_PER_BYTE)) & mask);
			}
			if (bytes[0] < 0) {
				for (int i = 0; i < 4 - bytes.length; i++) {
					ret = ret | (0xFF << ((bytes.length + i) * BITS_PER_BYTE));
				}
			}
		} else {
			for (int i = 0; i < bytes.length; i++) {
				mask = 0xFF << (i) * BITS_PER_BYTE;
				ret = ret
						| ((bytes[i] << (i * BITS_PER_BYTE)) & mask);
			}
			if (bytes[bytes.length - 1] < 0) {
				for (int i = 0; i < 4 - bytes.length; i++) {
					ret = ret | (0xFF << ((bytes.length + i) * BITS_PER_BYTE));
				}
			}
		}
		return ret;
	}
        
        public static short byteArrayToShort(byte[] bytes) {
		int ret = 0;
		int mask;
		for (int i = bytes.length - 1; i >= 0; i--) {
			mask = 0xFF << ((bytes.length - 1) - i) * BITS_PER_BYTE;
			ret = ret | ((bytes[i] << (((bytes.length - 1) - i) * BITS_PER_BYTE)) & mask);
		}
		if (bytes[0] < 0) {
			for (int i = 0; i < 4 - bytes.length; i++) {
				ret = ret | (0xFF << ((bytes.length + i) * BITS_PER_BYTE));
			}
		}
		return (short) ret;
	}
	
	public static Integer[] byteArrayToIntArray(byte[] bytes, int numBytesPerInt) {
		Integer[] iarr = new Integer[bytes.length / numBytesPerInt];
		byte[] temp = new byte[numBytesPerInt];
		for (int i = 0; i < iarr.length; i++) {
			for (int j = 0; j < numBytesPerInt; j++) {
				temp[j] = bytes[(i * numBytesPerInt) + j];
			}
			iarr[i] = byteArrayToInt(temp);
		}
		return (Integer[]) iarr;
	}
	
	public static Short[] byteArrayToShortArray(byte[] bytes, int numBytesPerInt) {
		Short[] iarr = new Short[bytes.length / numBytesPerInt];
		byte[] temp = new byte[numBytesPerInt];
		for (int i = 0; i < iarr.length; i++) {
			for (int j = 0; j < numBytesPerInt; j++) {
				temp[j] = bytes[(i * numBytesPerInt) + j];
			}
			iarr[i] = byteArrayToShort(temp);
		}
		return (Short[]) iarr;
	}
	
	public static Double[] byteArrayToDoubleArray(byte[] bytes, int numBytesPerInt) {
		Double[] iarr = new Double[bytes.length / numBytesPerInt];
		byte[] temp = new byte[numBytesPerInt];
		for (int i = 0; i < iarr.length; i++) {
			for (int j = 0; j < numBytesPerInt; j++) {
				temp[j] = bytes[(i * numBytesPerInt) + j];
			}
			iarr[i] = (double) byteArrayToInt(temp);
		}
		return (Double[]) iarr;
	}
	
	public static Float[] byteArrayToFloatArray(byte[] bytes, int numBytesPerInt) {
		Float[] iarr = new Float[bytes.length / numBytesPerInt];
		byte[] temp = new byte[numBytesPerInt];
		for (int i = 0; i < iarr.length; i++) {
			for (int j = 0; j < numBytesPerInt; j++) {
				temp[j] = bytes[(i * numBytesPerInt) + j];
			}
			iarr[i] = (float) byteArrayToInt(temp);
		}
		return (Float[]) iarr;
	}
	
	public static String toString(byte[] bytes) {
		if(bytes == null)
			return "null";
		String ret = "{" + bytes[0];
		for (int i = 1; i < bytes.length; i++)
			ret += ", " + bytes[i];
		ret += "}";
		return ret;
	}
	public static String toString(byte[] bytes, int groupSize) {
		if(bytes == null)
			return "null";
		String ret = "{(" + bytes[0];
		for (int i = 1; i < bytes.length; i++) {
			if (i % groupSize == 0)
				ret += "), (" + bytes[i];
			else
				ret += ", " + bytes[i];
		}
		ret += ")}";
		return ret;
	}
	
	
	

	/*public static void main(String[] args) {
		byte[] b = intToByteArray(32500, 2);
		byte temp = b[0];
		b[0] = b[1];
		b[1] = temp;
		System.out.println(byteArrayToInt(b, true) + ", " + byteArrayToInt(b, false) );
		int i = 0;
		for (i = 0; i < 10; i ++) {
			if (i == 2) {
				break;
			}
		}
		System.out.println(i);
	}*/
	
	
}
