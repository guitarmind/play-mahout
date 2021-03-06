package markpeng.kaggle.mmc;

public class HexStringToBytes {

	public static byte[] convert(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static void main(String[] args) {
		// long value = Long.parseLong("75F92BC2", 16);
		int value = Integer.parseInt("FFFF", 16);
		System.out.println(value);
	}

}
