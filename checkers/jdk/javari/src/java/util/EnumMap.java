package java.util;
import checkers.javari.quals.*;

public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> implements java.io.Serializable, Cloneable {
    private static final long serialVersionUID = 0L;
  public EnumMap(Class<K> a1) { throw new RuntimeException(("skeleton method")); }
  public EnumMap(@PolyRead EnumMap this, @PolyRead EnumMap<K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public EnumMap(@PolyRead EnumMap this, @PolyRead Map<K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public int size(@ReadOnly EnumMap this) { throw new RuntimeException(("skeleton method")); }
  public boolean containsValue(@ReadOnly EnumMap this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public boolean containsKey(@ReadOnly EnumMap this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public V get(@ReadOnly EnumMap this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public V put(K a1, V a2) { throw new RuntimeException(("skeleton method")); }
  public V remove(@ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public void putAll(@ReadOnly Map<? extends K, ? extends V> a1) { throw new RuntimeException(("skeleton method")); }
  public void clear() { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Set<K> keySet(@PolyRead EnumMap this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Collection<V> values(@PolyRead EnumMap this) { throw new RuntimeException(("skeleton method")); }
  public @PolyRead Set<Map.Entry<K, V>> entrySet(@PolyRead EnumMap this) { throw new RuntimeException(("skeleton method")); }
  public boolean equals(@ReadOnly EnumMap this, @ReadOnly Object a1) { throw new RuntimeException(("skeleton method")); }
  public EnumMap<K, V> clone() { throw new RuntimeException("skeleton method"); }
}
