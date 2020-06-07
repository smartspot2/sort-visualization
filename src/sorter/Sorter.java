package sorter;

import javafx.util.Pair;
import options.Settings;
import processing.core.PApplet;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static processing.core.PConstants.CENTER;

public class Sorter {
    /**
     * Vertical guide lines; list of item indices, drawing line on left edge
     */
    public final List<Integer> vertLines = new ArrayList<>();
    /**
     * Horizontal guide lines; list of item indices, drawing line at top edge
     */
    public final List<Item> horizLines = new ArrayList<>();
    private final PApplet app;
    private final List<Item> animItems = new ArrayList<>();
    private final ReentrantReadWriteLock arrLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock auxilLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock auxil2Lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock bucketsLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock animItemsLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock workingRangeLock = new ReentrantReadWriteLock();
    private final List<Integer> leonardo = new ArrayList<>();
    public int curDispType = 0;
    public long totalSwaps = 0;
    public long totalComps = 0;
    public long totalAccesses = 0;
    public boolean BREAK = false;
    /**
     * Shaded working range; pair of item indices, inclusive
     */
    public Pair<Integer, Integer> curWorkingRange;
    /**
     * Temporary vertical guide line; index of item, drawing line on left edge
     */
    public int changingVertLineItemIndex = -1;
    public boolean onlyDispTopGuides = false;
    public boolean onlyDispTopRange = false;
    public boolean NEXTSTEP = false;
    public String caption = "";
    private volatile List<Item> arr;
    private volatile List<Item> auxil;
    private volatile List<Item> auxil2;
    private volatile List<List<Item>> buckets;
    /**
     * Item on left side of array for temp storage
     */
    private Item tempItem = null;
    private int toSkip = 0;
    private volatile List<Integer> orders;
    private volatile List<Integer> roots;
    /**
     * Whether or not we're displaying the auxiliary array
     */
    private String layoutMode = "normal";

    public Sorter(PApplet app) {
        this.app = app;

        this.createItems();

        // Initialize fibonacci numbers
        leonardo.add(1);
        leonardo.add(1);
        while (leonardo.get(leonardo.size() - 1) < arr.size()) {
            leonardo.add(leonardo.get(leonardo.size() - 1) + leonardo.get(leonardo.size() - 2) + 1);
        }
    }

    public void createItems() {
        updateSettingsScales();

        float destX = Settings.getContentLeft();
        float destY = Settings.getLayout(layoutMode).get("arrBottom");

        arrLock.writeLock().lock();
        this.arr = new ArrayList<>();

        for (int i = 1; i <= Settings.totalItems; i++) {
            if (!Settings.randomItemHeights) {
                arr.add(new Item(this.app, i,
                        destX, destY, Settings.VALID_DISPTYPES[curDispType]));
            } else {
                arr.add(new Item(this.app, (int) this.app.random(Settings.totalItems),
                        destX, destY, Settings.VALID_DISPTYPES[curDispType]));
            }
        }

        if (!leonardo.isEmpty()) {
            while (leonardo.get(leonardo.size() - 1) < arr.size()) {
                leonardo.add(leonardo.get(leonardo.size() - 1) + leonardo.get(leonardo.size() - 2) + 1);
            }
        }
        arrLock.writeLock().unlock();
    }

    public void updatePositions() {
        updateSettingsScales();

        arrLock.readLock().lock();
        for (Item it : arr) {
            it.x = Settings.getContentLeft();
            it.y = Settings.getLayout(layoutMode).get("arrBottom");
            it.w = Settings.horizScale;
            it.setVal(it.getVal());
        }
        arrLock.readLock().unlock();

        if (layoutMode.contains("auxil")) {
            auxilLock.readLock().lock();
            for (Item it : auxil) {
                it.x = Settings.getContentLeft();
                it.y = Settings.getLayout(layoutMode).get("auxilBottom");
                it.w = Settings.horizScale;
                it.setVal(it.getVal());
            }
            auxilLock.readLock().unlock();
        }
    }

    private void updateSettingsScales() {
        Settings.vertScale = (this.app.height
                - (Settings.padTop + Settings.padBottom)
                - Settings.itemSpace * (Settings.totalItems - 1)
        ) / Settings.totalItems;
        Settings.horizScale = (this.app.width
                - (Settings.padLeft + Settings.padRight)
                - Settings.itemSpace * (Settings.totalItems - 1)
        ) / Settings.totalItems;
    }

    /**
     * Moves items and changes heights to make room for an auxiliary array below
     */
    private void changeLayout(String newLayout) {
        arrLock.readLock().lock();
        auxilLock.writeLock().lock();
        auxil2Lock.writeLock().lock();
        bucketsLock.writeLock().lock();
        switch (newLayout) {
            case "normal":
                layoutMode = "normal";

                auxil = null;
                auxil2 = null;
                buckets = null;

                for (Item item : arr) {
                    item.y = Settings.getLayout("normal").get("arrBottom");
                }
                break;
            case "auxil":
                layoutMode = "auxil";

                auxil = new ArrayList<>();
                auxil2 = null;
                buckets = null;

                for (Item item : arr) {
                    item.y = Settings.getLayout("auxil").get("arrBottom");
                }
                break;
            case "auxil2":
                layoutMode = "auxil";

                auxil = new ArrayList<>();
                auxil2 = new ArrayList<>();
                buckets = null;

                for (Item item : arr) {
                    item.y = Settings.getLayout("auxil2").get("arrBottom");
                }
                break;
            case "buckets":
                // TODO: implement for radix sort
                layoutMode = "buckets";

                auxil = null;
                auxil2 = null;
                buckets = new ArrayList<>(10);

                for (int i = 0; i < 10; i++) {
                    buckets.add(new ArrayList<>());
                }
                for (Item item : arr) {
                    item.y = Settings.getLayout("buckets").get("arrBottom");
                }
                throw new NotImplementedException();
            default:
                throw new IllegalStateException("Unexpected value: " + newLayout);
        }
        arrLock.readLock().unlock();
        auxilLock.writeLock().unlock();
        auxil2Lock.writeLock().unlock();
        bucketsLock.writeLock().unlock();
    }

    public void draw() {
        workingRangeLock.readLock().lock();
        if (curWorkingRange != null) {
            this.app.fill(255, 255, 255, 15);
            this.app.noStroke();
            float height = Settings.getContentHeight();
            if (onlyDispTopRange) {
                height = (int) (Settings.getContentHeight() *
                        Settings.getLayout(layoutMode).get("arrHeightScale"));
            }
            float workingRangeStart = Item.calcX(curWorkingRange.getKey(),
                    Settings.getContentLeft());
            float workingRangeEnd = Item.calcX(curWorkingRange.getValue(),
                    Settings.getContentLeft()) + Settings.horizScale;
            this.app.rect(workingRangeStart, Settings.getContentTop(),
                    Math.abs(workingRangeEnd - workingRangeStart),
                    height);
        }
        workingRangeLock.readLock().unlock();

        for (boolean drawAnimatingItems : new boolean[]{false, true}) {
            arrLock.readLock().lock();
            for (int i = 0; i < arr.size(); i++) {
                Item item = arr.get(i);
                if (item.isAnimating == drawAnimatingItems) {
                    item.show(i, Settings.getLayout(layoutMode).get("arrHeightScale"));
                }
            }
            arrLock.readLock().unlock();

            if (layoutMode.contains("auxil")) {
                auxilLock.readLock().lock();
                for (int i = 0; i < auxil.size(); i++) {
                    Item item = auxil.get(i);
                    if (item.isAnimating == drawAnimatingItems) {
                        item.show(i, Settings.getLayout(layoutMode).get("auxilHeightScale"));
                    }
                }
                auxilLock.readLock().unlock();
            } else if (layoutMode.equals("buckets")) {
                bucketsLock.readLock().lock();
                int curIndex = 0;
                for (List<Item> bucket : buckets) {
                    for (Item item : bucket) {
                        if (item.isAnimating == drawAnimatingItems) {
                            item.show(curIndex, Settings.getLayout(layoutMode).get("bucketsHeightScale"));
                        }
                        curIndex++;
                    }
                }
            }

            if (tempItem != null && (tempItem.isAnimating == drawAnimatingItems)) {
                tempItem.show(0, Settings.getLayout(layoutMode).get("arrHeightScale"));
            }
        }

        animItemsLock.readLock().lock();
        if (!this.animItems.isEmpty()) {
            for (Item animItem : animItems) {
                animItem.show(0, Settings.getLayout(layoutMode).get("arrHeightScale"));
            }
        }
        animItemsLock.readLock().unlock();

//        drawSecondaryGraph();

        // Draw vertical lines
        synchronized (this.vertLines) {
            float lineEnd = Settings.getContentBottom();
            if (onlyDispTopGuides) {
                lineEnd = Settings.getLayout(layoutMode).get("arrBottom");
            }
            for (int vertLineItemIndex : vertLines) {
                this.app.noFill();
                this.app.stroke(255);
                float vertLineX = Item.calcX(vertLineItemIndex,
                        Settings.getContentLeft());
                this.app.line(vertLineX, Settings.getContentTop(),
                        vertLineX, lineEnd);
            }
        }

        if (changingVertLineItemIndex >= 0) {
            this.app.noFill();
            this.app.stroke(255);
            float vertLineX = Item.calcX(changingVertLineItemIndex,
                    Settings.getContentLeft());
            this.app.line(vertLineX, Settings.getContentTop(),
                    vertLineX, Settings.getContentBottom());
        }

        // Draw horizontal lines
        synchronized (this.horizLines) {
            for (Item horizLineItem : horizLines) {
                this.app.noFill();
                this.app.stroke(255);
                float topEdge = horizLineItem.y - horizLineItem.h;
                this.app.line(Settings.getContentLeft(), topEdge,
                        Settings.getContentRight(), topEdge);
            }
        }

    }

    public void resetCounts() {
        this.totalSwaps = 0;
        this.totalComps = 0;
        this.totalAccesses = 0;
    }

    public void resetColors() {
        arrLock.readLock().lock();
        for (Item item : arr) {
            item.setColor(Settings.ITEMCOLOR);
        }
        arrLock.readLock().unlock();
        app.redraw();
    }

    public void resetTempItem() {
        this.tempItem = null;
        app.redraw();
    }

    private void createTempItem() {
        arrLock.readLock().lock();
        this.tempItem = new Item(this.app, 0,
                (Settings.getContentLeft() - Settings.horizScale) / 2f,
                Settings.getLayout(layoutMode).get("arrBottom"),
                Settings.VALID_DISPTYPES[curDispType]);
        arrLock.readLock().unlock();
    }

    public Item getTempItem() {
        return this.tempItem;
    }

    private void updateDisp(int delay) {
        if (toSkip <= 0) {
            toSkip = Settings.frameSkip;
            app.redraw();
            // frame delay
            app.delay(Settings.frameDelay);
            // any additional delay
            app.delay(delay);
        } else {
            toSkip--;
        }
    }

    // -------------------- ARRAY ACTIONS --------------------

    private void animateItems(Item[] items, int color, double[] dx_list, double[] dy_list, double[] dh_list) {
        assert items.length == dx_list.length && dx_list.length == dy_list.length && dy_list.length == dh_list.length;

        int[] prevColors = new int[items.length];

        for (int i = 0; i < items.length; i++) {
            Item item = items[i];
            item.isAnimating = true;
            prevColors[i] = item.c;
            item.setColor(color);
        }

        float step = (1f + Settings.frameSkip) / Settings.swapFrames;
        for (float curStep = 0; curStep < 1 + step; curStep += step) {
            for (int i = 0; i < items.length; i++) {
                items[i].animateStep(Math.min(1, curStep),
                        (float) dx_list[i], (float) dy_list[i], (float) dh_list[i]);
            }
            updateDisp(0);
        }

        for (int i = 0; i < items.length; i++) {
            Item item = items[i];
            item.setDX(0);
            item.setDY(0);
            item.setDH(0);

            item.setColor(prevColors[i]);
            item.isAnimating = false;
        }
    }

    private void animateItems(List<Item> items, int color,
                              List<Double> dx_list, List<Double> dy_list, List<Double> dh_list) {
        animateItems(items.toArray(new Item[0]), color,
                dx_list.stream().mapToDouble(x -> x).toArray(),
                dy_list.stream().mapToDouble(x -> x).toArray(),
                dh_list.stream().mapToDouble(x -> x).toArray()
        );
    }

    /**
     * Swaps two items in the array.
     */
    private void swap(int i1, int i2) {
        swap(i1, i2, "Swapping items");
    }

    /**
     * Swaps two items in the array.
     */
    private void swap(int i1, int i2, String stepCaption) {
        if (i1 == i2) return;
        this.totalSwaps++;
        this.totalAccesses += 2;

        arrLock.readLock().lock();

        if (Settings.animateMovement) {
            // Display caption while swapping
            this.caption = stepCaption;

            float i1InitX = arr.get(i1).calcX(i1);
            float i2InitX = arr.get(i2).calcX(i2);

            animateItems(new Item[]{arr.get(i1), arr.get(i2)}, Settings.SWAPCOLOR,
                    new double[]{Item.calcDX(i1, i2), Item.calcDX(i2, i1)},
                    new double[]{0, 0},
                    new double[]{0, 0});
        }

        fastSwap(i1, i2);
        markStep(stepCaption);
        updateDisp(0);

        arrLock.readLock().unlock();
    }

    /**
     * Swaps two items in the array instantly, without animation.
     */
    private void fastSwap(int i1, int i2) {
        // Swap positions in array
        Item temp = arr.get(i1);
        arr.set(i1, arr.get(i2));
        arr.set(i2, temp);
    }

    /**
     * Swap with temp item
     *
     * @param i           item in arr to swap with
     * @param stepCaption current step caption
     */
    private void swapTemp(int i, String stepCaption) {
        this.totalAccesses += 1;
        this.totalSwaps += 1;

        arrLock.readLock().lock();

        if (Settings.animateMovement) {
            // Display caption while swapping
            this.caption = stepCaption;

            float initX = arr.get(i).calcX(i);

            animateItems(new Item[]{this.tempItem, arr.get(i)}, Settings.SWAPCOLOR,
                    new double[]{initX - this.tempItem.x, this.tempItem.x - initX},
                    new double[]{0, 0},
                    new double[]{0, 0});
        }

        fastSwapTemp(i);
        markStep(stepCaption);
        updateDisp(0);

        arrLock.readLock().unlock();
    }

    private void fastSwapTemp(int i) {
        int temp = arr.get(i).getVal();
        arr.get(i).setVal(tempItem.getVal());
        tempItem.setVal(temp);
    }

    /**
     * Moves an element between two arrays (arr and auxil)
     *
     * @param iFrom         index in array to move from
     * @param iTo           index in array to move to
     * @param arrFrom       array to move from
     * @param arrTo         array to move to
     * @param keepBlankItem whether to remove the item or keep with value 0
     * @param stepCaption   current step caption
     */
    private void moveBetweenArrays(int iFrom, int iTo, List<Item> arrFrom, List<Item> arrTo,
                                   boolean keepBlankItem, String stepCaption) {
        this.totalSwaps++;
        this.totalAccesses += 1;

        float finalY;
        ReentrantReadWriteLock toLock, fromLock;
        if (arrTo == auxil) {
            toLock = auxilLock;
            fromLock = arrLock;
            finalY = Settings.getLayout(layoutMode).get("auxilBottom");
        } else if (arrTo == arr) {
            toLock = arrLock;
            fromLock = auxilLock;
            finalY = Settings.getLayout(layoutMode).get("arrBottom");
        } else {
            System.out.println("ERROR");
            throw new AssertionError();
        }

        if (iTo >= arrTo.size()) {
            appendUntilSize(arrTo, iTo, finalY);
        }

        fromLock.readLock().lock();
        toLock.readLock().lock();

        int prevColor = arrFrom.get(iFrom).c;

        float finalX = Settings.getContentLeft()
                + (iTo == -1 ? arrTo.size() : iTo) * (Settings.horizScale + Settings.itemSpace);


        if (Settings.animateMovement) {
            // Display caption while swapping
            this.caption = stepCaption;

            animateItems(new Item[]{arrFrom.get(iFrom)}, Settings.SWAPCOLOR,
                    new double[]{Item.calcDX(iFrom, iTo)},
                    new double[]{finalY - arrFrom.get(iFrom).y},
                    new double[]{0, 0});
        }
        toLock.readLock().unlock();

        // Do the actual move

        toLock.writeLock().lock();
        if (keepBlankItem) { // read from, write to
            Item newItem = new Item(this.app,
                    arrFrom.get(iFrom).getVal(), Settings.getContentLeft(), finalY,
                    arrFrom.get(iFrom).getDispType());
            if (iTo != -1) {
                if (arrTo.get(iTo).getVal() == 0) {
                    arrTo.get(iTo).setVal(arrFrom.get(iFrom).getVal());
                } else {
                    arrTo.add(iTo, newItem);
                }
            } else {
                arrTo.add(newItem);
            }

            arrFrom.get(iFrom).setVal(0);
        } else { // write from, write to
            fromLock.readLock().unlock();
            fromLock.writeLock().lock();
            if (iTo != -1) {
                if (arrTo.get(iTo).getVal() == 0) {
                    arrTo.get(iTo).setVal(arrFrom.get(iFrom).getVal());
                } else {
                    arrTo.add(iTo, arrFrom.get(iFrom));
                }
            } else {
                arrTo.add(arrFrom.get(iFrom));
            }
            arrFrom.remove(iFrom);
            fromLock.writeLock().unlock();
            fromLock.readLock().lock();
        }
        toLock.writeLock().unlock();

        markStep(stepCaption);
        updateDisp(0);

        // Reset colors; take swap into account
        toLock.readLock().lock();
        if (iTo != -1) {
            arrTo.get(iTo).setColor(prevColor);
        } else {
            arrTo.get(arrTo.size() - 1).setColor(prevColor);
        }
        arrFrom.get(iFrom).setColor(prevColor);
        toLock.readLock().unlock();
        fromLock.readLock().unlock();
    }

    private void appendUntilSize(List<Item> curArr, int size, float itemY) {
        Item itemTemplate = new Item(this.app, 0,
                Settings.getContentLeft(), itemY,
                Settings.VALID_DISPTYPES[this.curDispType]);
        if (curArr == arr) {
            arrLock.writeLock().lock();
        } else if (curArr == auxil) {
            auxilLock.writeLock().lock();
        }
        while (curArr.size() <= size) {
            curArr.add(itemTemplate.copy());
        }
        if (curArr == arr) {
            arrLock.writeLock().unlock();
        } else if (curArr == auxil) {
            auxilLock.writeLock().unlock();
        }
    }

    /**
     * Moves an item from the original array to the auxiliary array.
     * Replaces empty element if attempting to add at index.
     *
     * @param i             index of array item
     * @param aux_i         index to insert/replace in auxiliary array
     * @param keepBlankItem whether to remove the item or keep it with value 0
     * @param stepCaption   current step caption
     */
    private void moveToAuxil(int i, int aux_i, boolean keepBlankItem, String stepCaption) {
        moveBetweenArrays(i, aux_i, arr, auxil,
                keepBlankItem, stepCaption);
    }

    private void moveToAuxil(int i, boolean keepBlankItem, String stepCaption) {
        moveToAuxil(i, -1, keepBlankItem, stepCaption);
    }

    private void moveToAuxil(int i, int aux_i, boolean keepBlankItem) {
        moveToAuxil(i, aux_i, keepBlankItem, "Move element to auxiliary array");
    }

    private void moveToAuxil(int i, boolean keepBlankItem) {
        moveToAuxil(i, -1, keepBlankItem);
    }

    /**
     * Moves an item from the auxiliary array to the original array.
     * Replaces empty element if attempting to add at index.
     *
     * @param aux_i         index of auxiliary array item
     * @param i             index to insert/replace in array
     * @param keepBlankItem whether to remove the item or keep it with value 0
     * @param stepCaption   current step caption
     */
    private void moveToArr(int aux_i, int i, boolean keepBlankItem, String stepCaption) {
        moveBetweenArrays(aux_i, i, auxil, arr,
                keepBlankItem, stepCaption);
    }

    private void moveToArr(int aux_i, boolean keepBlankItem, String stepCaption) {
        moveToArr(aux_i, -1, keepBlankItem, stepCaption);
    }

    private void moveToArr(int aux_i, int i, boolean keepBlankItem) {
        moveToArr(aux_i, i, keepBlankItem, "Move element from auxiliary to array");
    }

    private void moveToArr(int aux_i, boolean keepBlankItem) {
        moveToArr(aux_i, -1, keepBlankItem);
    }

    /**
     * Cleans up auxil array so that there are no empty items present
     */
    private void removeEmptyAuxilItems() {
        auxilLock.writeLock().lock();
        for (int i = auxil.size() - 1; i >= 0; i--) {
            if (auxil.get(i).getVal() == 0) {
                auxil.remove(i);
            }
        }
        auxilLock.writeLock().unlock();
    }

    /**
     * Compares two items.
     * If a < b, compare(a, b) < 0. If a > b, compare(a, b) > 0. If a == b, compare(a, b) = 0.
     * Basically returns < 0 if a < b and > 0 if a > b; the comparison symbols are the same.
     *
     * @param a           item 1
     * @param b           item 2
     * @param stepCaption caption to display when moving step-by-step
     * @return -1 if a < b, 1 if a > b, 0 if a == b
     */
    private int compare(Item a, Item b, String stepCaption) {
        this.totalComps++;
        this.totalAccesses += 2;

        int aPrevColor = a.c;
        int bPrevColor = b.c;
        if (Settings.showComparisons) {
            a.setColor(Settings.COMPARECOLOR);
            b.setColor(Settings.COMPARECOLOR);

            markStep(stepCaption);

            if (!Settings.BYSTEP) {
                updateDisp(Settings.compDelay);
            }

            a.setColor(aPrevColor);
            b.setColor(bPrevColor);
        } else {
            markStep(stepCaption);
        }

        if (a.getVal() < b.getVal()) {
            return -1;
        } else if (a.getVal() > b.getVal()) {
            return 1;
        }
        return 0;
    }

    /**
     * Compares two items.
     * If a < b, compare(a, b) < 0. If a > b, compare(a, b) > 0. If a == b, compare(a, b) = 0.
     * Basically returns < 0 if a < b and > 0 if a > b; the comparison symbols are the same.
     *
     * @param a item 1
     * @param b item 2
     * @return -1 if a < b, 1 if a > b, 0 if a == b
     */
    private int compare(Item a, Item b) {
        return compare(a, b, "Comparing items");
    }

    /**
     * Sets the value of an item.
     * ONLY USE IF YOU KNOW WHAT YOU ARE DOING
     *
     * @param index  index of item to set
     * @param newVal new value of item
     */
    private void setItemVal(int index, int newVal, String stepCaption) {
        this.totalAccesses++;
        arrLock.readLock().lock();

        if (Settings.animateMovement) {
            animateItems(new Item[]{arr.get(index)}, Settings.SWAPCOLOR,
                    new double[]{0},
                    new double[]{0},
                    new double[]{(arr.get(index).getVal() - newVal) * Settings.vertScale});
        }

        arr.get(index).setVal(newVal);
        markStep(stepCaption);
        updateDisp(0);
        arrLock.readLock().unlock();
    }

    /**
     * Sets the value of an item.
     * ONLY USE IF YOU KNOW WHAT YOU ARE DOING
     *
     * @param index  index of item to set
     * @param newVal new value of item
     */
    private void setItemVal(int index, int newVal) {
        setItemVal(index, newVal, "Set item value");
    }

    private void setTempItemVal(int newVal, String stepCaption) {
        if (this.tempItem == null) {
            createTempItem();
        }

        if (Settings.animateMovement) {
            animateItems(new Item[]{this.tempItem}, Settings.SWAPCOLOR,
                    new double[]{0},
                    new double[]{0},
                    new double[]{(this.tempItem.getVal() - newVal) * Settings.vertScale});
        }

        this.tempItem.setVal(newVal);
        markStep(stepCaption);
        updateDisp(0);
    }

    private void setTempItemVal(int newVal) {
        setTempItemVal(newVal, "Set temp item value");
    }

    /**
     * Gets the value of an item.
     *
     * @param index index of item
     * @return value of item
     */
    private int getItemVal(int index) {
        this.totalAccesses++;
        return arr.get(index).getVal();
    }

    /**
     * If moving step-by-step, waits for client signal to continue.
     */
    private void markStep() {
        markStep("");
    }

    /**
     * If moving step-by-step, waits for client signal to continue.
     *
     * @param stepCaption string stepCaption to show while waiting
     */
    private void markStep(String stepCaption) {
        this.caption = stepCaption;
        if (Settings.BYSTEP && !NEXTSTEP) {
            // Write stepCaption
            app.redraw();
        }
        try {
            while (Settings.BYSTEP && !NEXTSTEP) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted.");
        }

        NEXTSTEP = false;
    }

    // -------------------- RANDOMIZATION --------------------

    /**
     * Randomizes array.
     */
    @Sort(dispName = "Randomize")
    public void randomize() {
        if (Settings.fastRandomize) {
            for (int i = 0; i < arr.size(); i++) {
                int randIndex = (int) app.random(0, arr.size());
                fastSwap(i, randIndex);
            }
        } else {
            for (int i = 0; i < arr.size(); i++) {
                int randIndex = (int) app.random(0, arr.size());
                swap(i, randIndex);

                if (BREAK) return;
            }
        }
    }

    /**
     * Randomizes array so that it's almost sorted.
     */
    @Sort(dispName = "Randomize (almost sorted)")
    public void randomizeClose() {
        if (Settings.fastRandomize) {
            for (int i = 0; i < arr.size(); i++) {
                int randIndex = (int) app.random(Math.max(0, i - Settings.randRange), Math.min(arr.size(), i + Settings.randRange));
                fastSwap(i, randIndex);
            }
        } else {
            for (int i = 0; i < arr.size(); i++) {
                int randIndex = (int) app.random(Math.max(0, i - Settings.randRange), Math.min(arr.size(), i + Settings.randRange));
                swap(i, randIndex);

                if (BREAK) return;
            }
        }
    }

    /**
     * Reverses array elements.
     */
    public void reverseItems() {
        if (Settings.fastRandomize) {
            Collections.reverse(arr);
        } else {
            for (int i = 0; i < arr.size() / 2; i++) {
                swap(i, arr.size() + ~i);
            }
        }
    }

    // -------------------- SORTS --------------------

    /**
     * Slow sort with inclusive start and end indices.
     * Intended as a humorous "multiply and surrender" sort.
     */
    @Sort(dispName = "Slowsort")
    public void slowSort() {
        slowSort(0, arr.size() - 1);
    }

    private void slowSort(int start, int end) {
        if (start >= end) return;
        if (BREAK) return;
        int m = (start + end) / 2;
        slowSort(start, m);
        slowSort(m + 1, end);
        if (compare(arr.get(m), arr.get(end)) > 0) {
            swap(m, end);
        }
        slowSort(start, end - 1);
    }

    /**
     * Stooge sort.
     * Recursively sorts first 2/3, last 2/3 and first 2/3 again.
     */
    @Sort(dispName = "Stooge Sort")
    public void stoogeSort() {
        stoogeSort(0, arr.size() - 1);
    }

    private void stoogeSort(int start, int end) {
        if (compare(arr.get(start), arr.get(end)) > 0) {
            swap(start, end);
        }

        if (end - start < 2) return;
        if (BREAK) return;

        int third = (end - start + 1) / 3;

        stoogeSort(start, end - third);
        stoogeSort(start + third, end);
        stoogeSort(start, end - third);
    }

    /**
     * Bubble sort (optimized).
     * Repeatedly "bubbles" the largest items to the right.
     */
    @Sort(dispName = "Bubble Sort")
    public void bubbleSort() {
        boolean hasSwapped;
        for (int n = arr.size() - 1; n > 0; n--) {
            hasSwapped = false;
            for (int i = 0; i < n; i++) {
                if (compare(arr.get(i), arr.get(i + 1)) > 0) {
                    swap(i, i + 1);
                    hasSwapped = true;
                }

                if (BREAK) return;
            }

            if (!hasSwapped) return;

            arr.get(n).setColor(Settings.SORTEDITEMCOLOR);
        }
        arr.get(0).setColor(Settings.SORTEDITEMCOLOR);
    }

    /**
     * Odd-Even sort.
     * Similar to bubble sort, alternates between even and odd items.
     */
    @Sort(dispName = "Odd-Even Sort")
    public void oddEvenSort() {
        boolean sorted = false;
        while (!sorted) {
            sorted = true;
            for (int i = 0; i < arr.size() - 1; i += 2) {
                if (compare(arr.get(i), arr.get(i + 1), "Compare even-odd pair") > 0) {
                    swap(i, i + 1, "Swap even-odd pair");
                    sorted = false;
                }

                if (BREAK) return;
            }

            for (int i = 1; i < arr.size() - 1; i += 2) {
                if (compare(arr.get(i), arr.get(i + 1), "Compare odd-even pair") > 0) {
                    swap(i, i + 1, "Swap odd-even pair");
                    sorted = false;
                }

                if (BREAK) return;
            }
        }
    }

    /**
     * Selection sort, storing cumulative minimum, then swapping.
     * In-place, builds up a sorted sublist from the left.
     */
    @Sort(dispName = "Selection Sort")
    public void selectionSort() {
        for (int i = 0; i < arr.size() - 1; i++) {
            int minLocation = i;
            for (int j = i + 1; j < arr.size(); j++) {
                if (compare(arr.get(minLocation), arr.get(j), "Finding minimum element") > 0) {  // arr[i] > arr[j]
                    minLocation = j;
                }

                if (BREAK) return;
            }

            if (minLocation != i) { // Swap only if there's a new min
                swap(i, minLocation, "Swapping minimum to sorted section");
            }

            arr.get(i).setColor(Settings.SORTEDITEMCOLOR);
        }
    }

    /**
     * Insertion sort.
     * Moves from left to right, builds a sorted sublist,
     * inserting items in their correct place.
     */
    @Sort(dispName = "Insertion Sort")
    public void insertionSort() {
        for (int i = 0; i < arr.size(); i++) {
            int j;
            for (j = i - 1; j >= 0 && compare(arr.get(j), arr.get(j + 1)) > 0; j--) {
                // Keep swapping until arr[i] is inserted into sorted head of arr
                swap(j, j + 1, "Swap out of place items");

                if (BREAK) return;
            }

            arr.get(j + 1).setColor(Settings.SORTEDITEMCOLOR);
            if (j == 0) {
                arr.get(j).setColor(Settings.SORTEDITEMCOLOR);
            }
        }
    }

    /**
     * Gnome sort.
     * A version of insertion sort that only looks at consecutive items
     * and consecutively swaps the items into position.
     */
    @Sort(dispName = "Gnome Sort")
    public void gnomeSort() {
        int index = 1;
        while (index < arr.size()) {
            if (compare(arr.get(index - 1), arr.get(index)) > 0) {
                swap(index - 1, index);
                index--;
            } else {
                index++;
            }

            if (index == 0) index = 1;

            if (BREAK) return;
        }
    }

    /**
     * Merge sort.
     * Divides array into halves and recurses on each half.
     */
    @Sort(dispName = "Merge Sort")
    public void mergeSort() {
        this.changeLayout("auxil");
        onlyDispTopGuides = true;

        mergeSort(0, arr.size());

        this.changeLayout("normal");
        onlyDispTopGuides = false;

        workingRangeLock.writeLock().lock();
        curWorkingRange = null;
        workingRangeLock.writeLock().unlock();
    }

    private void mergeSort(int i, int j) {
        // Base case; one or less element = sorted
        if (i >= j - 1) return;

        if (BREAK) return;

        // Sort halves
        int mid = (i + j) / 2;
        synchronized (this.vertLines) {
            vertLines.add(mid);
        }
        mergeSort(i, mid);
        mergeSort(mid, j);
        if (BREAK) return;

        workingRangeLock.writeLock().lock();
        curWorkingRange = new Pair<>(i, j - 1);
        workingRangeLock.writeLock().unlock();

        // Merge two halves
        int left = i, right = mid;
        for (int arrIndex = i; arrIndex < j; arrIndex++) {
            if (left < mid && (right >= j || compare(arr.get(left), arr.get(right), "Comparing items for merge") < 0)) {
                // Either right ran out or left is less than right
                moveToAuxil(left, arrIndex, true, "Copy item from left");
                left++;
            } else {
                // Either left ran out or right is less than left
                moveToAuxil(right, arrIndex, true, "Copy item from right");
                right++;
            }

            if (BREAK) return;
        }

        synchronized (this.vertLines) {
            vertLines.remove(Integer.valueOf(mid));
        }

        // Copy to arr
        for (int arrIndex = i; arrIndex < j; arrIndex++) {
            moveToArr(arrIndex, arrIndex, true, "Set merged array item");
        }

        removeEmptyAuxilItems();
    }

    /**
     * Merge sort (iterative).
     * A non-recursive version of the typical merge sort.
     */
    @Sort(dispName = "Merge Sort (iterative)")
    public void iterativeMergeSort() {
        this.changeLayout("auxil");

        for (int curBucketSize = 1; curBucketSize < arr.size(); curBucketSize *= 2) {
            for (int i = 0; i < arr.size() - curBucketSize; i += 2 * curBucketSize) {
                int j = Math.min(arr.size(), i + 2 * curBucketSize);
                int mid = i + curBucketSize;
                int left = i;
                int right = mid;

                workingRangeLock.writeLock().lock();
                curWorkingRange = new Pair<>(i, j - 1);
                workingRangeLock.writeLock().unlock();

                // Merge two buckets
                for (int arrIndex = i; arrIndex < j; arrIndex++) {
                    if (left < mid && (right >= j || compare(arr.get(left), arr.get(right)) < 0)) {
                        // Either right ran out or left is less than right
                        moveToAuxil(left, arrIndex, true, "Copy item from left");
                        left++;
                    } else {
                        // Either left ran out or right is less than left
                        moveToAuxil(right, arrIndex, true, "Copy item from right");
                        right++;
                    }

                    if (BREAK) return;
                }

                // Copy to arr
                for (int arrIndex = i; arrIndex < j; arrIndex++) {
                    moveToArr(arrIndex, arrIndex, true, "Set merged array item");

                    if (BREAK) return;
                }

                removeEmptyAuxilItems();
            }
        }

        this.changeLayout("normal");

        workingRangeLock.writeLock().lock();
        curWorkingRange = null;
        workingRangeLock.writeLock().unlock();
    }

    /**
     * Quick sort.
     * Picks a pivot, divides array into a sets of items less than
     * and greater than the pivot, recursing on each.
     * Uses median-of-3 pivot choice, swapping pivot to the right.
     */
    @Sort(dispName = "Quick Sort")
    public void quickSort() {
        quickSort(0, arr.size() - 1);

        workingRangeLock.writeLock().lock();
        curWorkingRange = null;
        workingRangeLock.writeLock().unlock();

        changingVertLineItemIndex = -1;
    }

    private void quickSort(int i, int j) {
        // Sorts from [i, j]; note inclusive i and j
        if (i >= j) {
            return;
        }

        if (BREAK) return;

        workingRangeLock.writeLock().lock();
        curWorkingRange = new Pair<>(i, j);
        workingRangeLock.writeLock().unlock();

        // Partition
        int mid = (i + j) / 2;

        if (compare(arr.get(i), arr.get(mid), "Finding pivot") > 0) {
            swap(i, mid, "Finding pivot");
        }
        if (compare(arr.get(i), arr.get(j), "Finding pivot") > 0) {
            swap(i, j, "Finding pivot");
        }
        if (compare(arr.get(mid), arr.get(j), "Finding pivot") < 0) {
            swap(mid, j, "Finding pivot");
        }

        synchronized (this.horizLines) {
            horizLines.add(arr.get(j));
        }

        int low = i;
        changingVertLineItemIndex = low;
        for (int cur = i; cur <= j; cur++) {
            if (compare(arr.get(cur), arr.get(j), "Compare to pivot") < 0) {
                swap(low, cur, "Swap around pivot");
                low++;
                changingVertLineItemIndex = low;
            }

            if (BREAK) return;
        }
        changingVertLineItemIndex = -1;

        synchronized (this.horizLines) {
            horizLines.remove(arr.get(j));
        }

        // Swap pivot back to middle
        swap(low, j, "Swap pivot to correct position");

        // Quicksort halves
        quickSort(i, low - 1);
        quickSort(low + 1, j);
    }

    /**
     * Heapsort (in-place).
     * Creates a max-heap from the array, dequeuing the root node
     * and repairing the heap.
     */
    @Sort(dispName = "Heapsort")
    public void heapSort() {
        // Set heap color
        for (Item it : arr) {
            it.setColor(Settings.HEAPCOLOR);
        }

        // Create heap
        heapify();

        // Sort
        for (int end = arr.size() - 1; end >= 0; end--) {
            // Largest element of heap is at the beginning; swap to end
            swap(0, end, "Dequeue from heap");
            arr.get(end).setColor(Settings.ITEMCOLOR);  // Item no longer in heap

            // Rebuild heap
            siftDown(0, end - 1);

            if (BREAK) return;
        }
    }

    private void heapify() {
        for (int start = getHeapParent(arr.size() - 1); start >= 0; start--) {
            siftDown(start, arr.size() - 1);

            if (BREAK) return;
        }
    }

    private void siftDown(int start, int end) {
        int root = start;

        while (getHeapLeftChild(root) <= end) {  // While has child
            int child = getHeapLeftChild(root);

            // If right child exists and is greater, use that instead
            if (child + 1 <= end && compare(arr.get(child + 1), arr.get(child), "Get max child") > 0) {
                child++;  // left child = 2i+1, right child = 2i+2, so to point to right child, just +1
            }

            // If the greatest child is greater than the parent (root), then swap
            if (compare(arr.get(child), arr.get(root), "Compare max child to root; check heap validity") > 0) {
                swap(child, root, "Swap child to root");
                // Sift down tree to test children
                root = child;
            } else {
                // Already a heap, so don't need to do any more
                return;
            }

            if (BREAK) return;
        }
    }

    private int getHeapParent(int i) {
        return (int) Math.floor((i - 1) / 2d);
    }

    private int getHeapLeftChild(int i) {
        return 2 * i + 1;
    }

    /**
     * Draws line segments denoting the tree structure of the built heap.
     */
    public void drawMaxHeap(PApplet win) {
        int maxDepth = (int) (Math.log(arr.size()) / Math.log(2));
        float contentWidth = win.width - Settings.padLeft - Settings.padRight;
        int r = (int) (contentWidth / Math.pow(2, maxDepth)) / 2;

        Settings.NODERADIUS = Math.max(r, 1);

        drawMaxHeapNode(win, Settings.padLeft, win.width - Settings.padRight, 0, 0);
    }

    private void drawMaxHeapNode(PApplet win, float left, float right, int level, int itemIndex) {
        int parentIndex = getHeapParent(itemIndex);
        float maxDepth = (float) (Math.log(arr.size()) / Math.log(2)) + 1;
        float vertGap = (win.height - Settings.padTop - Settings.padBottom) / maxDepth;
        float centerx = (left + right) / 2f;
        float centery = Settings.padTop + level * vertGap + Settings.NODERADIUS;
        float pcenterx = (getHeapLeftChild(getHeapParent(itemIndex)) == itemIndex) ? right : left;
        float pcentery = centery - vertGap;

        if (arr.get(itemIndex).c == Settings.ITEMCOLOR) {
            return;
        }

        if (itemIndex > 0) {
            win.stroke(Settings.HEAPLINECOLOR);
            if (arr.get(itemIndex).c == arr.get(parentIndex).c && arr.get(itemIndex).c != Settings.HEAPCOLOR) {
                win.stroke(arr.get(itemIndex).c);
            }
            win.noFill();
            win.strokeWeight((float) Math.max(1, Math.sqrt(Settings.NODERADIUS)));
            double parentDeg = Math.atan2(pcentery - centery, pcenterx - centerx);
            win.line(centerx + Settings.NODERADIUS * (float) Math.cos(parentDeg),
                    centery + Settings.NODERADIUS * (float) Math.sin(parentDeg),
                    pcenterx - Settings.NODERADIUS * (float) Math.cos(parentDeg),
                    pcentery - Settings.NODERADIUS * (float) Math.sin(parentDeg));
        }

        win.noStroke();
        win.fill(arr.get(itemIndex).c);
        win.ellipse(centerx, centery, 2 * Settings.NODERADIUS, 2 * Settings.NODERADIUS);

        if (Settings.NODERADIUS > 10) {
            win.fill(0xFF666666);
            win.textSize(12);
            win.textAlign(CENTER, CENTER);
            win.text(arr.get(itemIndex).getVal(), centerx, centery);
        }

        int leftChild = getHeapLeftChild(itemIndex);
        if (leftChild < arr.size()) {   // Left child
            drawMaxHeapNode(win, left, centerx, level + 1, leftChild);
        }

        if (leftChild + 1 < arr.size()) {   // Right child
            drawMaxHeapNode(win, centerx, right, level + 1, leftChild + 1);
        }
    }

    /**
     * LSD radix sort.
     * Non-comparison, out-of-place sort, sorts digits into "buckets"
     * from least significant to most significant, recursing through the digits.
     */
    @Sort(dispName = "Radix Sort (LSD)")
    public void lsdRadixSort() {
        // Get maximum value of arr
        int maxVal = getItemVal(0);
        for (Item it : arr) {
            maxVal = Math.max(maxVal, it.getVal());
        }

        // Get the most amount of digits in array values
        int maxDigits = ("" + maxVal).length();

        List<List<Integer>> buckets = new ArrayList<>(10);  // Values with each digit
        for (int i = 0; i < 10; i++) {
            buckets.add(new ArrayList<>());
        }

        // Radix sort
        for (int exp = 0; exp < maxDigits; exp++) {

            // Get counts
            for (int i = 0; i < arr.size(); i++) {
                int curVal = getItemVal(i);
                int curDigit = (int) (curVal / (Math.pow(10, exp))) % 10;

                buckets.get(curDigit).add(curVal);
            }

            // Set array values
            int curBucket = 0;
            int arrIndex = 0;
            while (curBucket < 10) {
                if (buckets.get(curBucket).size() > 0) {
                    setItemVal(arrIndex, buckets.get(curBucket).remove(0),
                            "Set items for digit " + curBucket + " in " + (int) Math.pow(10, exp) + "s place");
                    arrIndex++;
                } else {
                    if (BREAK) return;
                    curBucket++;
                }
            }

            // Buckets are now all empty, so don't need to reset buckets
        }
    }

    /**
     * MSD radix sort.
     * Non-comparison, out-of-place sort, sorts digits into "buckets"
     * from most significant to least significant, recursing through the digits.
     */
    @Sort(dispName = "Radix Sort (MSD)")
    public void msdRadixSort() {
        int maxVal = getItemVal(0);

        for (int i = 0; i < arr.size(); i++) {
            maxVal = Math.max(maxVal, getItemVal(i));
        }

        // Get the most amount of digits in array values
        int maxDigits = ("" + maxVal).length();

        msdSortBucket(0, arr.size(), maxDigits - 1);
    }

    /**
     * Sort a bucket recursively with MSD sort
     *
     * @param left  bucket start in arr (inclusive)
     * @param right bucket end in arr (exclusive)
     * @param exp   digit to sort
     * @see Sorter#msdRadixSort()
     */
    private void msdSortBucket(int left, int right, int exp) {
        if (BREAK) return;
        List<List<Integer>> buckets = new ArrayList<>(10);  // Values with each digit
        for (int i = 0; i < 10; i++) {
            buckets.add(new ArrayList<>());
        }

        for (int i = left; i < right; i++) {
            int curVal = getItemVal(i);
            int curDigit = (int) (curVal / (Math.pow(10, exp))) % 10;
            buckets.get(curDigit).add(curVal);
        }

        int curBucket = 0;
        int arrIndex = left;
        int[] bucketStartIndices = new int[11];
        bucketStartIndices[0] = left;      // lower index bound of first bucket
        bucketStartIndices[10] = right;    // upper index bound of last bucket
        while (curBucket < 10) {  // Set original bucket vals
            if (buckets.get(curBucket).size() > 0) {
                setItemVal(arrIndex, buckets.get(curBucket).remove(0),
                        "Set items for digit " + curBucket + " in " + (int) Math.pow(10, exp) + "s place");
                arrIndex++;
            } else {
                if (BREAK) return;
                curBucket++;
                bucketStartIndices[curBucket] = arrIndex;
            }
        }

        if (exp > 0) {
            for (curBucket = 0; curBucket < 10; curBucket++) {
                int bucketStart = bucketStartIndices[curBucket];
                int bucketEnd = bucketStartIndices[curBucket + 1];

                int curBucketSize = bucketEnd - bucketStart;
                if (curBucketSize > 1) {
                    msdSortBucket(bucketStart, bucketEnd, exp - 1);
                }

                if (BREAK) return;
            }
        }
    }

    /**
     * Smoothsort.
     * Similar to a heapsort, except using Leonardo heaps instead of binary heaps.
     *
     * @see Sorter#heapSort()
     */
    @Sort(dispName = "Smoothsort")
    public void smoothSort() {
        // Build heap
        orders = new ArrayList<>();
        orders.add(1);
        orders.add(1);
        roots = new ArrayList<>();
        roots.add(0);
        roots.add(1);

        arr.get(0).setColor(Settings.HEAPCOLOR);
        arr.get(1).setColor(Settings.HEAPCOLOR);

        for (int i = 2; i < arr.size(); i++) {
            arr.get(i).setColor(Settings.HEAPCOLOR);
            // Add to heap
            if (orders.size() < 2) {
                orders.add(1);
                roots.add(i);
            } else {
                int last = orders.get(orders.size() - 1);
                int secondlast = orders.get(orders.size() - 2);
                if (secondlast - last <= 1) { // Merge last two heaps
                    // Remove last size
                    orders.remove(orders.size() - 1);
                    // Remove last two roots
                    roots.remove(roots.size() - 1);
                    // Increase heap number by 1
                    orders.set(orders.size() - 1, secondlast + 1);
                    // Add new root
                    roots.set(roots.size() - 1, i);
                } else {
                    // Can't merge, so add singleton
                    orders.add(1);
                    roots.add(i);
                }
            }

            updateDisp(0);
            markStep("Adding new item to heap");

            // Repair heap
            leonardo_balance(orders, roots);

            if (BREAK) return;
        }

        // Dequeue
        for (int i = arr.size() - 1; i >= 0; i--) {
            arr.get(i).setColor(Settings.ITEMCOLOR);
            if (orders.get(orders.size() - 1) <= 1) {
                // Singleton; just remove
                orders.remove(orders.size() - 1);
                roots.remove(roots.size() - 1);
            } else { // Break apart heap, stringify left, stringify right
                int lastSizeIndex = orders.size() - 1;
                orders.set(lastSizeIndex, orders.get(lastSizeIndex) - 1);
                orders.add(orders.get(lastSizeIndex) - 1);

                // Remove last root
                int lastRoot = roots.remove(roots.size() - 1);
                // Left heap
                int prevRoot = (roots.size() > 0) ? roots.get(roots.size() - 1) : -1;
                roots.add(prevRoot + leonardo.get(orders.get(orders.size() - 2)));
                // Right heap
                roots.add(lastRoot - 1);

                updateDisp(0);
                markStep("Removing item from heap");

                // Stringify left
                leonardo_balance(orders.subList(0, orders.size() - 1), roots.subList(0, roots.size() - 1));
                // Stringify right
                leonardo_balance(orders, roots);
            }

            if (BREAK) return;
        }
    }

    /**
     * Swaps roots of Leonardo heaps until valid; calls <code>heapify</code> on
     *
     * @param orders list of orders of leoardo heaps
     * @param roots  root indices of each heap
     * @see Sorter#leonardo_heapify
     */
    private void leonardo_balance(List<Integer> orders, List<Integer> roots) {
        for (int root_i = roots.size() - 1; root_i >= 0; root_i--) {
            if (root_i == 0) {
                leonardo_heapify(orders.get(root_i), 0);
            } else {
                int curRoot = roots.get(root_i);
                int prevRoot = roots.get(root_i - 1);
//                int curRootStart = prevRoot + 1;

                if (compare(arr.get(prevRoot), arr.get(curRoot), "Checking heap root validity") > 0) {
                    int order = orders.get(root_i);
                    if (order > 1) {
                        // Has children
                        int leftChild = prevRoot + leonardo.get(order - 1);
                        int rightChild = curRoot - 1;
                        if (compare(arr.get(prevRoot), arr.get(leftChild),
                                "Checking child for possibility of swapping roots") > 0 &&
                                compare(arr.get(prevRoot), arr.get(rightChild),
                                        "Checking child for possibility of swapping roots") > 0) {
                            // Larger than children; swap and continue
                            swap(prevRoot, curRoot, "Swap roots: previous root larger than current children");
                        } else {
                            // Heapify current heap and we're done
                            leonardo_heapify(orders.get(root_i), prevRoot + 1);
                            break;
                        }
                    } else {
                        // Swap and continue
                        swap(prevRoot, curRoot, "Swap roots: no children");
                    }
                } else {
                    // Heapify current heap and we're done
                    leonardo_heapify(orders.get(root_i), prevRoot + 1);
                    break;
                }
            }

            if (BREAK) return;
        }
    }

    /**
     * Repairs Leonardo heaps by trickling down recursively.
     *
     * @param order order of current heap
     * @param start index at which the current heap begins
     */
    private void leonardo_heapify(int order, int start) {
        if (order <= 1 || BREAK) return;
        int root = start + leonardo.get(order) - 1;
        int left = start + leonardo.get(order - 1) - 1;
        int right = start + leonardo.get(order) - 2;
        int maxChild = (compare(arr.get(left), arr.get(right), "Finding max child") > 0) ? left : right;

        if (compare(arr.get(root), arr.get(maxChild), "Checking for heap validity") <= 0) {
            swap(maxChild, root, "Swap root and max child");
            if (maxChild == left && order > 2) {
                leonardo_heapify(order - 1, start);
            } else if (order > 3) {
                leonardo_heapify(order - 2, start + leonardo.get(order - 1));
            }
        }
    }

    public synchronized List<Integer> getOrders() {
        return new ArrayList<>(orders);
    }

    public synchronized List<Integer> getRoots() {
        return new ArrayList<>(roots);
    }

    /**
     * Draws graph corresponding to the leonardo heap from Smoothsort
     *
     * @see Sorter#smoothSort
     */
    public void drawLeonardoHeap() {
        List<Integer> orders = this.getOrders();
        List<Integer> roots = this.getRoots();
        for (int i = 0; i < orders.size(); i++) {
            drawBar(orders.get(i), (i == 0) ? 0 : roots.get(i - 1) + 1, 0);
        }
    }

    private void drawBar(int order, int start, int level) {
//        System.out.println("order = " + order + ", start = " + start + ", level = " + level);
        app.fill(Settings.HEAPBARCOLORS[order % Settings.HEAPBARCOLORS.length]);
        app.noStroke();
        float tlx = arr.get(start).x;
        float tly = Settings.padTop + level * Settings.vertScale * 0.75f;
        float w = leonardo.get(order) * Settings.horizScale;
        float h = Settings.vertScale / 2f;
        app.rect(tlx, tly, w, 0.5f * Settings.vertScale);
        // Write bar value if big enough
        if (Settings.vertScale > 20) {
            app.fill(0xFFFFFFFF);
            app.noStroke();
            app.textSize(12);
            app.textAlign(CENTER, CENTER);
            app.text(arr.get(start + leonardo.get(order) - 1).getVal(), tlx + w / 2, tly + h / 2);
        }
        if (order > 1) {
            drawBar(order - 1, start, level + 1);
            drawBar(order - 2, start + leonardo.get(order - 1), level + 1);
        }
    }

    /**
     * Shellsort, using Tokuda's gap seqeuence.
     *
     * @see ShellsortGaps#tokudaNext(int, int)
     */
    @Sort(dispName = "Shellsort (Tokuda)")
    public void shellSort_Tokuda() {
        shellSort("tokuda");
    }

    /**
     * Shellsort, using Ciura's gap seqeuence.
     *
     * @see ShellsortGaps#ciuraNext(int, int)
     */
    @Sort(dispName = "Shellsort (Ciura)")
    public void shellSort_Ciura() {
        shellSort("ciura");
    }

    private void shellSort(String seqName) {
        List<Integer> gaps = ShellsortGaps.getGaps(seqName, arr.size());
        for (int gap : gaps) {
            for (int i = gap; i < arr.size(); i++) {
                for (int j = i; j >= gap && compare(arr.get(j - gap), arr.get(j), "Compare for gap " + gap) > 0; j -= gap) {
                    swap(j, j - gap, "Gap-sort for gap " + gap);

                    if (BREAK) return;
                }

                if (BREAK) return;
            }
        }
    }

    @Sort(dispName = "Cycle Sort")
    public void cycleSort() {
        for (int cycleStart = 0; cycleStart < arr.size(); cycleStart++) {
            int pos = cycleStart;
            setTempItemVal(arr.get(pos).getVal());
            for (int i = cycleStart + 1; i < arr.size(); i++) {
                if (compare(arr.get(i), this.tempItem, "Find correct item position") < 0) {
                    pos += 1;
                }
                if (BREAK) break;
            }

            if (BREAK) break;

            if (pos == cycleStart) {
                arr.get(pos).setColor(Settings.SORTEDITEMCOLOR);
                continue;
            }

            swapTemp(pos, "Swap item into correct position");
            arr.get(pos).setColor(Settings.SORTEDITEMCOLOR);

            while (pos != cycleStart) {
                pos = cycleStart;
                for (int i = cycleStart + 1; i < arr.size(); i++) {
                    if (compare(arr.get(i), this.tempItem, "Find correct item position") < 0) {
                        pos += 1;
                    }
                    if (BREAK) break;
                }

                while (compare(arr.get(pos), this.tempItem, "Place after any duplicates") == 0) {
                    pos += 1;
                    if (BREAK) break;
                }
                swapTemp(pos, "Swap item into correct position");
                arr.get(pos).setColor(Settings.SORTEDITEMCOLOR);

                if (BREAK) break;
            }

            if (BREAK) break;
        }

        this.resetTempItem();
    }

    @Sort(dispName = "Cocktail Shaker Sort")
    public void cocktailShakerSort() {
        int start = 0;
        int end = arr.size() - 1;
        boolean swapped;
        do {
            swapped = false;
            for (int i = start; i < end; i++) {
                if (compare(arr.get(i), arr.get(i + 1)) > 0) {
                    swap(i, i + 1);
                    swapped = true;
                }
                if (BREAK) break;
            }
            if (BREAK) break;
            arr.get(end).setColor(Settings.SORTEDITEMCOLOR);
            end -= 1;
            if (!swapped) {
                break;
            }
            swapped = false;
            for (int i = end - 1; i >= start; i--) {
                if (compare(arr.get(i), arr.get(i + 1)) > 0) {
                    swap(i, i + 1);
                    swapped = true;
                }
                if (BREAK) break;
            }
            if (BREAK) break;
            arr.get(start).setColor(Settings.SORTEDITEMCOLOR);
            start += 1;
        } while (swapped);
    }

    @Sort(dispName = "Bead Sort")
    public void beadSort() {
        this.changeLayout("auxil");
        updateDisp(0);
        for (int i = 0; i < arr.size(); i++) {
            splitToAuxil(i);
            if (BREAK) break;
        }

        for (int i = arr.size() - 1; i >= 0; i--) {
            stripFromAuxil(i);
            if (BREAK) break;
        }
        this.changeLayout("normal");
    }

    private void splitToAuxil(int itemIndex) {
        assert layoutMode.contains("auxil");  // must have an auxil to split to it

        Item arrItem = arr.get(itemIndex);
        int arrVal = arrItem.getVal();
        float itemX = arrItem.calcX(itemIndex);
        arrItem.setVal(0);

        if (Settings.animateMovement) {
            // Split item into blocks of val 1
            List<Item> splitList = new ArrayList<>();
            List<Double> dx_list = new ArrayList<>();
            List<Double> dy_list = new ArrayList<>();
            List<Double> dh_list = new ArrayList<>();
            animItemsLock.writeLock().lock();
            for (int v = 0; v < arrVal; v++) {
                // Split up bar into multiple items of value 1, stacked on top of each other
                Item newItem = new Item(
                        this.app, 1,
                        itemX,
                        arrItem.y - v * Settings.getLayout(layoutMode).get("arrHeightScale") *
                                (Settings.vertScale + Settings.itemSpace),
                        arrItem.getDispType()
                );
                newItem.setColor(Settings.SWAPCOLOR);
                splitList.add(newItem);
                dx_list.add((double) Item.calcDX(itemIndex, v));

                float destY = Settings.getLayout(layoutMode).get("auxilBottom");
                if (v < auxil.size()) {  // item y-coord in auxil, accounting for index in stack
                    destY -= auxil.get(v).h * Settings.getLayout(layoutMode).get("arrHeightScale");
                }
                dy_list.add((double) (destY - newItem.y));

                dh_list.add(0d);
                if (Settings.animateMovement) {
                    animItems.add(newItem);
                }
            }
            animItemsLock.writeLock().unlock();

            animateItems(splitList, Settings.SWAPCOLOR, dx_list, dy_list, dh_list);

            animItemsLock.writeLock().lock();
            for (Item animItem : splitList) {
                animItems.remove(animItem);
            }
            animItemsLock.writeLock().unlock();
        }

        // Do actual split into auxil
        for (int i = 0; i < arrVal; i++) {
            if (i >= auxil.size()) {
                appendUntilSize(auxil, i + 1,
                        Settings.getLayout(layoutMode).get("auxilBottom"));
            }
            auxil.get(i).setVal(auxil.get(i).getVal() + 1);
        }
        updateDisp(0);
    }

    private void stripFromAuxil(int arrItemIndex) {
        if (auxil.isEmpty()) {
            return;
        }
        if (Settings.animateMovement) {
            Item emptyRow = new Item(this.app, 1,
                    Settings.getContentLeft(),
                    Settings.getLayout(layoutMode).get("auxilBottom"),
                    Settings.VALID_DISPTYPES[curDispType]
            );
            emptyRow.setColor(Settings.BACKGROUNDCOLOR);
            emptyRow.w = auxil.get(auxil.size() - 1).calcX(auxil.size() - 1) + Settings.horizScale
                    - auxil.get(0).calcX(0);
            animItemsLock.writeLock().lock();
            animItems.add(emptyRow);
            animItemsLock.writeLock().unlock();

            List<Item> splitList = new ArrayList<>();
            List<Double> dx_list = new ArrayList<>();
            List<Double> dy_list = new ArrayList<>();
            List<Double> dh_list = new ArrayList<>();
            auxilLock.readLock().lock();
            animItemsLock.writeLock().lock();
            for (int i = 0; i < auxil.size(); i++) {
                Item newItem = new Item(
                        this.app, 1,
                        auxil.get(i).calcX(i),
                        auxil.get(i).y,
                        auxil.get(i).getDispType()
                );
                newItem.setColor(Settings.SWAPCOLOR);
                splitList.add(newItem);
                dx_list.add((double) Item.calcDX(i, arrItemIndex));

                float destY = arr.get(arrItemIndex).y  // Final item y-value
                        - i * Settings.getLayout(layoutMode).get("auxilHeightScale")
                        * (Settings.vertScale + Settings.itemSpace);  // Adjust for split item value
                dy_list.add((double) (destY - auxil.get(i).y));

                dh_list.add(0d);
                if (Settings.animateMovement) {
                    animItems.add(newItem);
                }
            }
            animItemsLock.writeLock().unlock();
            auxilLock.readLock().unlock();

            animateItems(splitList, Settings.SWAPCOLOR, dx_list, dy_list, dh_list);

            animItemsLock.writeLock().lock();
            for (Item animItem : splitList) {
                animItems.remove(animItem);
            }
            animItems.remove(emptyRow);
            animItemsLock.writeLock().unlock();
        }

        // Do actual split into auxil
        int finalArrItemVal = 0;
        auxilLock.readLock().lock();
        for (int i = auxil.size() - 1; i >= 0; i--) {
            int auxilVal = auxil.get(i).getVal();
            if (auxilVal >= 1) {
                auxil.get(i).setVal(auxil.get(i).getVal() - 1);
                finalArrItemVal++;
            }
            if (auxil.get(i).getVal() == 0) {
                auxil.remove(i);
            }
        }
        auxilLock.readLock().unlock();
        arr.get(arrItemIndex).setVal(finalArrItemVal);
        updateDisp(0);
    }
}
