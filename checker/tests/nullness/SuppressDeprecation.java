import org.checkerframework.checker.nullness.qual.*;

class Other {
  @Deprecated
  void old() {}
}

class SuppressDeprecation {

  @MonotonicNonNull String tz1;

  @SuppressWarnings("deprecation")
  void processOptions(String tz, Other o) {
    tz1 = tz;

    // There should be no deprecation warning here.
    o.old();

    parseTime("hello");
  }

  @RequiresNonNull("tz1")
  void parseTime(String time) { }
}
