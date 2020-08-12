package fastset;

import fastset.FastSet;

/**
 * @author Daniel J. B. Clarke - maayanlab
 * 
 * We implement a fast hashset by storing whether or not the value is set in a boolean array
 *  sized accordingly to fit some bounds, but offer no obvious ways to iterate through the set
 *  as such *clear* is somewhat slow. If you need clear / iteration through the set sey FastKeyedHashSet.
 */
public class FastHashSet implements FastSet {
  private boolean[] value_set = null;
  private short n = 0;
  private short min_value;
  private short max_value;

  public FastHashSet(short min, short max) {
    min_value = min;
    max_value = max;
    int size = max_value - min_value + 2; // (1) inclusive bounds + (1) skip index[0]
    value_set = new boolean[size]; // wasting 1 allocated element arguably cheaper than treating something other than 0 as null
    n = 0;
  }

  private int encode_index(short value) throws Exception {
    if (value < min_value || value > max_value) throw new Exception("Out of bounds: " + value);
    return (int)(value - min_value) + 1;
  }

  public void add(short value) throws Exception {
    if (this.contains(value)) return;
    n += 1;
    value_set[encode_index(value)] = true;
  }

  public boolean contains(short value) throws Exception {
    return value_set[encode_index(value)];
  }

  public short size() {
    return n;
  }

  public void clear() throws Exception {
    // WARNING: this should not be used, if you need it use keyedhashset
    //  the only reason I have it is because it is likely still better than re-malloc
    for (short i = min_value; i <= max_value; i++) {
      if (value_set[encode_index(i)]) {
        value_set[encode_index(i)] = false;
        n -= 1;
      }
    }
  }
}
