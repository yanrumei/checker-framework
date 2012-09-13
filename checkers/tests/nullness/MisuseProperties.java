import checkers.nullness.quals.*;

import java.util.*;

public class MisuseProperties {

  void propertiesToHashtable(Properties p) {
    //:: error: (argument.type.incompatible)
    p.setProperty("line.separator", null);
    //:: error: (argument.type.incompatible)
    p.put("line.separator", null);
    Hashtable h = p;
    //:: error: (argument.type.incompatible)
    h.put("line.separator", null);
    //:: error: (argument.type.incompatible)
    System.setProperty("line.separator", null);

    Dictionary d1 = p;
    //:: error: (argument.type.incompatible)
    d1.put("line.separator", null);

    //:: error: (assignment.type.incompatible)
    Dictionary<Object,@Nullable Object> d2 = p;
    d2.put("line.separator", null);

    System.setProperties(p);    // OK; p has no null values

    System.clearProperty("foo.bar"); // OK


    // Each of the following should cause an error, because it leaves
    // line.separator null.

    // These first few need to be special-cased, I think:

    System.clearProperty("line.separator");

    p.remove("line.separator");
    p.clear();

    // These are OK because they seem to only add, not remove, properties:
    // p.load(InputStream), p.load(Reader), p.loadFromXML(InputStream)


    // The following problems are a result of treating a Properties as one
    // of its supertypes.  Here are some solutions:
    //  * Forbid treating a Properties object as any of its supertypes.
    //  * Create an annotation on a Properties object, such as
    //    @HasSystemProperties, and forbid some operations (or any
    //    treatment as a supertype) for such properties.

    // TODO: I don't understand why this assignment.type.incompatible error happens
    //:: error: (assignment.type.incompatible)
    Set<Object> keys = p.keySet();
    // now remove  "line.separator" from the set
    keys.remove("line.separator");
    keys.removeAll(keys);
    keys.clear();
    keys.retainAll(Collections.EMPTY_SET);

    Set<Map.Entry<Object,Object>> entries = p.entrySet();
    // now remove the pair containing "line.separator" from the set, as above

    Collection<Object> values = p.values();
    // now remove the line separator value from values, as above

    Hashtable h9 = p;
    h9.remove("line.separator");
    h9.clear();
    // also access via entrySet, keySet, values

    Dictionary d9 = p;
    d9.remove("line.separator");

  }

}
