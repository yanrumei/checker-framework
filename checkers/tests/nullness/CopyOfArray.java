
import java.util.Arrays;

public class CopyOfArray {
    protected void makeCopy(Object[] args) {
        //:: error: (assignment.type.incompatible)
        Object[] copy = Arrays.copyOf(args, args.length);
    }
}



