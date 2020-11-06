package fastset;

import fastset.FastSet;
import java.util.Arrays;

/**
 * @author Daniel J. B. Clarke - maayanlab
 *
 * The sequantial part of a keyed hash set, useful for fast HashSet computation, but not
 *  capable of set operations itself. Mainly here to provide O(1) insertion/clear and
 *  instant sequential access.
 */
public class FastHashSeq {
  private short[] value_seq = null;
  private short n = 0;
  private short min_value;
  private short max_value;

  public FastHashSeq(short min, short max) {
    min_value = min;
    max_value = max;
    int size = max_value - min_value + 1; // (1) inclusive bounds
    value_seq = new short[size];
    n = 0;
  }

  public void add(short value) throws Exception {
    value_seq[n] = value;
    n += 1;
  }

  public short value(int index) throws Exception {
    return value_seq[index];
  }

  public short size() {
    return n;
  }

  public void clear() throws Exception {
    n = 0;
  }

  /**
    * This will compute overlap with an array but note that duplicates
    *  in the array would be counted twice.
    */
  public void overlapWithArray(FastSet a, short[] b) throws Exception {
    this.clear();
    for (int i = 0; i < b.length; i++) {
      if (a.contains(b[i])) this.add(b[i]);
    }
  }

  public short[] toArray() {
    return Arrays.copyOfRange(value_seq, 0, n);
  }
}
