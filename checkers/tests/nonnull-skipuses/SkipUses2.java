import checkers.nonnull.quals.RequiresNonNull;
import checkers.nullness.quals.*;

public class SkipUses2 {

  static class SkipMe {
    static @Nullable Object f;
    @RequiresNonNull("f")
    static void foo() { }
  }

  static class DontSkip {
    static @Nullable Object f;
    @RequiresNonNull("f")
    static @Nullable Object foo() {
      return null;
    }
  }

  static class Main {
    void bar(boolean b) {
      SkipMe.f = null;
      SkipMe.foo();
      DontSkip.f = null;
      //:: error: (contracts.precondition.not.satisfied)
      DontSkip.foo();
    }
  }

}
