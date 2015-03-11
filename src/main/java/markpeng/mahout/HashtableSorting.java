package markpeng.mahout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HashtableSorting {
	private Hashtable aTable = new Hashtable();

	public HashtableSorting(Hashtable aTable) {
		this.aTable = aTable;
	}

	public List sorting(Comparator comparator) {

		Hashtable temp = new Hashtable();
		List keyString = new ArrayList();

		if (aTable != null) {
			for (Object key : aTable.keySet())
				temp.put(key, aTable.get(key));
			List<Map.Entry> list = new ArrayList<Map.Entry>(temp.entrySet());
			Collections.sort(list, comparator);
			Collections.reverse(list);

			Iterator<Map.Entry> i = list.iterator();
			while (i.hasNext()) {
				Map.Entry entry = i.next();
				keyString.add(entry.getKey());
			}
		}

		return keyString;
	}

	public static class StringComparator implements
			java.util.Comparator<Map.Entry<String, List<String>>> {
		public int compare(Map.Entry<String, List<String>> o1,
				Map.Entry<String, List<String>> o2) {
			return o1.getKey().compareTo(o2.getKey());
		}
	}

	public static class DoubleComparator implements
			java.util.Comparator<Map.Entry> {
		public int compare(Map.Entry o1, Map.Entry o2) {
			// if (((Double) o1.getValue()).doubleValue() > ((Double) o2
			// .getValue()).doubleValue())
			// return 1;
			// else if (((Double) o1.getValue()).doubleValue() < ((Double) o2
			// .getValue()).doubleValue())
			// return -1;
			// else
			// return 0;

			return Double.compare((Double) o1.getValue(),
					(Double) o2.getValue());
		}
	}

	public static class IntegerComparator implements
			java.util.Comparator<Map.Entry> {
		public int compare(Map.Entry o1, Map.Entry o2) {
			return Double.compare((Integer) o1.getValue(),
					(Integer) o2.getValue());
		}
	}
}
