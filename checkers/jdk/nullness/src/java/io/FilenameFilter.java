package java.io;

import checkers.nullness.quals.Nullable;


public abstract interface FilenameFilter{
  public abstract boolean accept(File a1, String a2);
}
