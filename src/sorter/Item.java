package sorter;

import options.Settings;
import processing.core.PApplet;

import java.util.Arrays;

public class Item implements Comparable<Item> {
    private final PApplet app;
    /**
     * Left x-coordinate of entire Item list
     */
    public float x;
    /**
     * Bottom-left corner of base; all items should have the same y-coordinate
     */
    public float y;
    public float h;   // Calculated in constructor
    public float w;   // For customization
    public int c;
    public volatile boolean isAnimating = false;
    private int val;
    private volatile float dx, dy, dh;
    private String dispType; // Available: "rect", "dot"; defaults to "dot"

    public Item(PApplet app, int value, float x, float y, String dispType) {
        this.app = app;
        this.val = value;
        this.x = x;
        this.y = y;
        this.dispType = dispType;

        this.c = Settings.ITEMCOLOR;
        this.h = this.val * Settings.vertScale + Settings.itemSpace * (this.val - 1);
        this.w = Settings.horizScale;
    }

    /**
     * Show item on canvas, given an index to determine the x-coordinate
     *
     * @param index index of item in array; used to calculate x-coordinate
     */
    public void show(int index) {
        show(index, 1);
    }

    /**
     * Show item on canvas, given an index to determine the x-coordinate
     *
     * @param index       index of item in array; used to calculate x-coordinate
     * @param heightScale height scale of item
     */
    public void show(int index, float heightScale) {
        if (!Arrays.asList(Settings.VALID_DISPTYPES).contains(dispType)) dispType = "dot";
        if (dispType.equals("dot") && val > 0) {
            app.fill(c);
            app.noStroke();
            app.rect(x + (Settings.horizScale + Settings.itemSpace) * index + dx,
                    y + dy - heightScale * (h - dh), w, heightScale * Settings.vertScale);
        } else if (dispType.equals("rect") && val > 0) {
            app.fill(c);
            app.noStroke();
            app.rect(x + (Settings.horizScale + Settings.itemSpace) * index + dx,
                    y + dy - heightScale * (h - dh), w, heightScale * (h - dh));
        }
        app.fill(255);
    }

    public static float calcX(int index, float padLeft) {
        return padLeft + (Settings.horizScale + Settings.itemSpace) * index;
    }

    public static float calcDX(int indexFrom, int indexTo) {
        return (indexTo - indexFrom) * (Settings.horizScale + Settings.itemSpace);
    }

    public float calcX(int index) {
        return this.x + (Settings.horizScale + Settings.itemSpace) * index;
    }

    /**
     * Change item position and size for animation
     *
     * @param step float from 0 to 1, indicating progress to end of animation
     * @param dx   overall x-axis displacement
     * @param dy   overall y-axis displacement
     * @param dh   overall height displacement
     */
    public void animateStep(float step, float dx, float dy, float dh) {
        if (step < 0 || step > 1) throw new IllegalArgumentException("step must be between 0 and 1.");

        this.setDX(step * dx);
        this.setDY(step * dy);
        this.setDH(step * dh);
    }

    public int getVal() {
        return this.val;
    }

    public void setVal(int newVal) {
        this.val = newVal;
        this.h = this.val * Settings.vertScale + Settings.itemSpace * (this.val - 1);
    }

    public String getDispType() {
        return dispType;
    }

    public void setDispType(String dispType) {
        this.dispType = dispType;
    }

    public void setDX(float dx) {
        this.dx = dx;
    }

    public void setDY(float dy) {
        this.dy = dy;
    }

    public void setDH(float dh) {
        this.dh = dh;
    }

    public void setColor(int c) {
        this.c = c;
    }

    public String toString() {
        return String.format("Item(%d)", this.val);
    }

    public int hashCode() {
        return (int) (31 * (val + 17 * (x + 13 * y)));
    }

    public boolean equals(Object other) {
        if (other instanceof Item) {
            return ((Item) other).val == this.val;
        }
        return false;
    }

    public int compareTo(Item other) {
        return this.val - other.val;
    }

    public Item copy() {
        return new Item(this.app, this.val, this.x, this.y, this.dispType);
    }
}