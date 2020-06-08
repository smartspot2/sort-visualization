package options;

import java.util.HashMap;

public class Settings {
    public static final float padTop = 25;
    public static final float padBottom = 150;
    public static final float padLeft = 50;
    public static final float padRight = 50;
    public static final int BACKGROUNDCOLOR = 0x000000;
    public static final int COMPARECOLOR = 0xFF00FF00; // color(0, 255, 0);
    public static final int ITEMCOLOR = 0xFFFF0000; // color(255, 0, 0);
    public static final int SWAPCOLOR = 0xFF0000FF; // color(0, 0, 255);
    public static final int HEAPCOLOR = 0xFFFFFF00; // color(255, 255, 0);
    public static final int HEAPLINECOLOR = 0xFF888888;
    public static final int HEAPCOMPLINECOLOR = 0xFF008800;
    public static final int SORTEDITEMCOLOR = 0xFFFF3333;
    public static final int[] HEAPBARCOLORS = {0xFF03899C, 0xFF7908AA, 0xFFF3FD00, 0xFFFF7A00};
    public static final int[] BUCKETLABELCOLORS = {0xFF212121, 0xFF424242};
    public static final String[] VALID_DISPTYPES = {"rect", "dot"};
    public static final boolean fastRandomize = true;
    public static final int randRange = 10;
    // TODO: add option to change setFrames
    public static final int setFrames = 5;  // Frames per setvalue; only matters if animateSwap is true
    public static int appHeight = 950;
    public static int appWidth = 900;
    public static boolean BYSTEP = false;
    public static int itemSpace = 0; // Horizontal/Vertical spacing between items
    public static int totalItems = 250;  // Total items
    public static boolean randomItemHeights = false;
    public static int frameDelay = 5; // In milliseconds
    public static int frameSkip = 0;  // Frames to skip before redrawing
    public static boolean animateMovement = false;  // Whether or not to animate swaps
    public static int swapFrames = 0;  // Frames per swap; only matters if animateSwap is true
    public static boolean showComparisons = true;
    public static int compDelay = 0;  // Frames to pause after highlighting comparison

    // Even when swapFrames = 0, still move items in groups
    public static boolean moveFromAuxilInBlocks = false;

    public static float vertScale, horizScale;
    public static int NODERADIUS = 5;

    public static HashMap<String, HashMap<String, Float>> getLayouts() {
        return new HashMap<String, HashMap<String, Float>>() {{
            put("normal", new HashMap<String, Float>() {{
                put("arrHeightScale", 1f);
                put("arrBottom", getContentBottom());
            }});
            put("auxil", new HashMap<String, Float>() {{
                put("arrHeightScale", 0.45f);
                put("arrBottom", getContentTop() + getContentHeight() * 0.45f);
                put("auxilHeightScale", 0.45f);
                put("auxilBottom", getContentBottom());
            }});
            put("auxil2", new HashMap<String, Float>() {{
                put("arrHeightScale", 0.3f);
                put("arrBottom", getContentTop() + getContentHeight() * 0.3f);
                put("auxilHeightScale", 0.3f);
                put("auxilBottom", getContentTop() + getContentHeight() * 0.65f);
                put("auxil2HeightScale", 0.3f);
                put("auxil2Bottom", getContentBottom());
            }});
            put("buckets", new HashMap<String, Float>() {{
                put("arrHeightScale", 0.45f);
                put("arrBottom", getContentTop() + getContentHeight() * 0.45f);
                put("bucketsHeightScale", 0.45f);
                put("bucketsBottom", getContentBottom());
            }});
        }};
    }

    public static HashMap<String, Float> getLayout(String layoutMode) {
        return getLayouts().get(layoutMode);
    }

    /**
     * Centers content based on scales and spaces, and calculates the top y-value
     */
    public static float getContentTop() {
        float contentHeightDiff = getAvailableContentHeight() - getContentHeight();
        return padTop + contentHeightDiff / 2f;
    }

    /**
     * Centers content based on scales and spaces, and calculates the bottom y-value
     */
    public static float getContentBottom() {
        float contentHeightDiff = getAvailableContentHeight() - getContentHeight();
        return appHeight - padBottom - contentHeightDiff / 2f;
    }

    public static float getContentLeft() {
        float contentWidthDiff = getAvailableContentWidth() - getContentWidth();
        return padLeft + contentWidthDiff / 2f;
    }

    public static float getContentRight() {
        float contentWidthDiff = getAvailableContentWidth() - getContentWidth();
        return appWidth - padRight - contentWidthDiff / 2f;
    }

    public static float getAvailableContentHeight() {
        return appHeight - padTop - padBottom;
    }

    public static float getAvailableContentWidth() {
        return appWidth - padLeft - padRight;
    }

    public static float getContentHeight() {
        return (vertScale + itemSpace) * totalItems - itemSpace;
    }

    // ----- GRAPH WINDOW -----

    public static float getContentWidth() {
        return (horizScale + itemSpace) * totalItems - itemSpace;
    }
}
