package java.lang;

import checkers.nullness.quals.Nullable;


public abstract interface CharSequence{
  public abstract int length();
  public abstract char charAt(int a1);
  public abstract CharSequence subSequence(int a1, int a2);
  public abstract String toString();
}
