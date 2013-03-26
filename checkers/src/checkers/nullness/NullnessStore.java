package checkers.nullness;

import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAbstractStore;
import checkers.initialization.InitializationStore;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.PolyNull;

/**
 * Behaves like {@link InitializationStore}, but additionally tracks whether
 * {@link PolyNull} is known to be {@link Nullable}.
 *
 * @author Stefan Heule
 */
public class NullnessStore extends
        InitializationStore<NullnessValue, NullnessStore> {

    protected boolean isPolyNullNull;

    public NullnessStore(
            CFAbstractAnalysis<NullnessValue, NullnessStore, ?> analysis,
            boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
        isPolyNullNull = false;
    }

    public NullnessStore(NullnessStore s) {
        super(s);
        isPolyNullNull = s.isPolyNullNull;
    }

    @Override
    public NullnessStore leastUpperBound(NullnessStore other) {
        NullnessStore lub = super.leastUpperBound(other);
        if (isPolyNullNull == other.isPolyNullNull) {
            lub.isPolyNullNull = isPolyNullNull;
        } else {
            lub.isPolyNullNull = false;
        }
        return lub;
    }

    @Override
    protected boolean supersetOf(CFAbstractStore<NullnessValue, NullnessStore> o) {
        if (!(o instanceof InitializationStore)) {
            return false;
        }
        NullnessStore other = (NullnessStore) o;
        if (other.isPolyNullNull != isPolyNullNull) {
            return false;
        }
        return super.supersetOf(other);
    }

    @Override
    protected void internalDotOutput(StringBuilder result) {
        super.internalDotOutput(result);
        result.append("  isPolyNonNull = " + isPolyNullNull + "\\n");
    }

    public boolean isPolyNullNull() {
        return isPolyNullNull;
    }

    public void setPolyNullNull(boolean isPolyNullNull) {
        this.isPolyNullNull = isPolyNullNull;
    }
}
