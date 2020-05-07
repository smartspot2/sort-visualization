package sorter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ShellsortGaps {
    public static List<Integer> getGaps(String seqName, int maxGap) {
        // Get sequence from methods
        String seqMethodName = seqName.toLowerCase().replaceAll("\\s+", "_") + "Next";

        List<String> allMethodsList = new ArrayList<>();
        for (Method method : ShellsortGaps.class.getMethods()) {
            if (method.getName().endsWith("Next")) {
                allMethodsList.add(method.getName().replace("Next", ""));
            }
        }
        String[] allMethods = allMethodsList.toArray(new String[0]);

        Method seqNext;
        try {
            seqNext = ShellsortGaps.class.getMethod(seqName.toLowerCase() + "Next", int.class, int.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new AssertionError("Invalid sequence name; got " + seqName + ", expected any of " + Arrays.toString(allMethods));
        }

        List<Integer> seq = new ArrayList<>();
        seq.add(1);
        try {
            for (int i = 0; seq.get(seq.size() - 1) < maxGap; i++) {
                seq.add((int) seqNext.invoke(ShellsortGaps.class, seq.get(seq.size() - 1), i));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        seq.remove(seq.size() - 1);
        Collections.reverse(seq);
        return seq;
    }

    /**
     * Tokuda's gap sequence for shellsort.
     * <p>
     * h_k = ceil(h'_k), h'_k = 2.25h'_{k-1} + 1, h'_1 = 1 <br>
     * or <br>
     * h_k = ceil(1/5 * (9 * (9/4)^k - 4)), k >= 0
     *
     * @param prevVal  previous gap value
     * @param curIndex current gap index (unused)
     * @return next gap value
     */
    public static int tokudaNext(int prevVal, int curIndex) {
        return (int) Math.ceil(prevVal * 2.25 + 1);
    }

    /**
     * Ciura's gap sequence for shellsort.
     * <p>
     * Experimentally derived: <br>
     * [1, 4, 10, 23, 57, 132, 301, 701, 1750]
     * <p>
     * Extended with: <br>
     * h_k = floor(2.25 * h_{k-1})
     *
     * @param prevVal  previous gap value
     * @param curIndex current gap index
     * @return next gap value
     */
    public static int ciuraNext(int prevVal, int curIndex) {
        int[] determinedSeq = {4, 10, 23, 57, 132, 301, 701, 1750};
        if (curIndex < determinedSeq.length) return determinedSeq[curIndex];
        return (int) Math.floor(2.25 * prevVal);
    }
}
