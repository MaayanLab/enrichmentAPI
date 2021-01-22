package fastset;

import java.util.Arrays;
import fastset.FastSet;

/**
 * @author Daniel J. B. Clarke - maayanlab
 * 
 * A full keyed hash capable of very fast lookups and iteration. Can interoperate with
 *  FastSets, and is ideal for performing overlap computations given that it allows you
 *  to fetch the results reasonably efficiently, while memory can be much cheaper using the FastHashSet.
 */
public class FastKeyedHashSet implements FastSet  {
  private short[] index_to_value = null;
  private short[] value_to_index = null;
  private short n = 0;
  private short min_value;
  private short max_value;

  public FastKeyedHashSet(short min, short max) {
    min_value = min;
    max_value = max;
    int size = max_value - min_value + 2; // (1) inclusive bounds + (1) skip index[0]
    index_to_value = new short[size]; // wasting 1 allocated element arguably cheaper than treating something other than 0 as null
    value_to_index = new short[size]; // yes, there are *two* tables, if you want one you can't seq through it, FastHashSet.
    n = 0;
  }

  private int encode_index(short value) throws Exception {
    if (value < min_value || value > max_value) throw new Exception("Out of bounds: " + value);
    return (int)(value - min_value) + 1;
  }

  private short decode_index(int index) throws Exception {
    short value = (short)((index - 1) + min_value);
    if (value < min_value || value > max_value) throw new Exception("Out of bounds: " + value);
    return value;
  }

  public void add(short value) throws Exception {
    if (this.contains(value)) return;
    n += 1;
    index_to_value[n] = value;
    value_to_index[encode_index(value)] = n;
  }

  public int index(short value) throws Exception {
    return value_to_index[encode_index(value)] - 1;
  }

  public short value(int index) throws Exception {
    return index_to_value[index + 1];
  }

  public boolean contains(short value) throws Exception {
    return value_to_index[encode_index(value)] != 0;
  }

  public short size() {
    return n;
  }

  public void clear() throws Exception {
    while (n > 0) {
      value_to_index[encode_index(index_to_value[n])] = 0;
      index_to_value[n] = 0;
      n -= 1;
    }
  }

  public void overlap(FastKeyedHashSet a, FastKeyedHashSet b) throws Exception {
    this.clear();
    if (a.size() <= b.size()) {
      for (int i = 0; i < a.size(); i++) {
        short v = a.value(i);
        if (b.contains(v)) this.add(v);
      }
    } else {
      for (int i = 0; i < b.size(); i++) {
        short v = b.value(i);
        if (a.contains(v)) this.add(v);
      }
    }
  }

  public void overlapWithSet(FastKeyedHashSet a, FastSet b) throws Exception {
    this.clear();
    for (int i = 0; i < a.size(); i++) {
      short v = a.value(i);
      if (b.contains(v)) this.add(v);
    }
  }

  public void overlapWithArray(FastSet a, short[] b) throws Exception {
    this.clear();
    for (int i = 0; i < b.length; i++) {
      if (a.contains(b[i])) this.add(b[i]);
    }
  }

  public short[] toArray() {
    return Arrays.copyOfRange(index_to_value, 1, n+1);
  }

  public static void main(String[] args) throws Exception {
    short min_value = Short.MIN_VALUE+1;
    short max_value = Short.MAX_VALUE-1;
    FastKeyedHashSet a = new FastKeyedHashSet(min_value, max_value);
    FastKeyedHashSet b = new FastKeyedHashSet(min_value, max_value);
    FastKeyedHashSet c = new FastKeyedHashSet(min_value, max_value);

    for (short i = -100; i <= 100; i++) {
      if (i % 5 == 0 || (-i) % 5 == 0) a.add(i);
      if (i % 10 == 0) b.add(i);
    }
    c.overlap(a, b);
    for (int i = 0; i < c.size(); i++) {
      System.out.println(c.value(i));
    }

    System.out.println();
    a.clear(); b.clear();

    for (short i = -300; i <= 0; i++) {
      if (i % 5 == 0 || (-i) % 5 == 0) a.add(i);
      if (i % 10 == 0) b.add(i);
    }
    c.overlap(a, b);
    for (short v : c.toArray()) {
      System.out.println(v);
    }
  }
}
