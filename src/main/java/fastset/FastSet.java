package fastset;

/**
 * @author Daniel J. B. Clarke - maayanlab
 * 
 * A fastset must minimally be able to add values and report whether or not a value has been registered and how big it is.
 */
public interface FastSet {
  public abstract void add(short value) throws Exception;
  public boolean contains(short value) throws Exception;
  public short size() throws Exception;
}
