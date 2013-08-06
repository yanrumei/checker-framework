package java.lang;

import checkers.nullness.quals.Nullable;

public class IllegalAccessError extends IncompatibleClassChangeError {
    private static final long serialVersionUID = 0L;
    public IllegalAccessError() {
	super();
    }

    public IllegalAccessError(@Nullable String s) {
	super(s);
    }
}
