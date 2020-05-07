import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import options.Settings;
import options.sortSelection.Events;
import options.sortSelection.SortSelectionWindowFX;
import options.yaml.SortInfo;
import processing.core.PApplet;
import processing.core.PSurface;
import processing.javafx.PSurfaceFX;
import sorter.GraphWindow;
import sorter.Sort;
import sorter.Sorter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;

public class S27_SortingV2 extends PApplet {

    final ArrayList<Method> randomizeMethods = new ArrayList<>();
    final ArrayList<Method> sortMethods = new ArrayList<>();
    // ----- GLOBAL VARIABLES -----
    volatile boolean IN_THREAD = false;
    int curSortIndex = 0;
    Sorter sorter;
    private GraphWindow graphWin;
    private boolean FLAG_SELECTEDTOKUDA = false;
    private PSurfaceFX FXSurface;
    private Canvas canvas;
    private Stage stage;

    public static void main(String[] args) {
        PApplet.main("S27_SortingV2");
    }

    public void c() {

    }

    public void exit() {
        super.exit();
        SortSelectionWindowFX.getApp().exit();
        System.out.println("exit called");
    }

    @Override
    protected PSurface initSurface() {

        PSurface surface = super.initSurface();

        FXSurface = (PSurfaceFX) surface;
        canvas = (Canvas) FXSurface.getNative(); // canvas is the processing drawing
        stage = (Stage) canvas.getScene().getWindow(); // stage is the window

        stage.setTitle("Sorting Visualizer");
//        canvas.widthProperty().unbind();
//        canvas.heightProperty().unbind();
        surface.setResizable(true);

        Platform.runLater(() -> {
            SortSelectionWindowFX.getApp().init();
            SortSelectionWindowFX.getApp().start(new Stage());

            SortSelectionWindowFX.getApp().stage.addEventHandler(Events.SORT_SELECT, event -> {
                // Change current sort to mirror selection window

                if (FLAG_SELECTEDTOKUDA) {
                    FLAG_SELECTEDTOKUDA = false;
                    return; // Prevent from selecting Ciura immediately after selecting Tokuda
                }
                SortInfo selectedSortItem = SortSelectionWindowFX.getApp().selectionList
                        .getSelectionModel().getSelectedItem();
                if (selectedSortItem != null) {
                    String selectedSort = selectedSortItem.methodName;

                    for (int i = 0; i < sortMethods.size(); i++) {
                        if (sortMethods.get(i).getName().equals(selectedSort)) {
                            curSortIndex = i;
                        }
                    }
                    redraw();
                }
            });
        });

        stage.setOnCloseRequest(event -> exit());

        canvas.widthProperty().addListener((obs, oldVal, newVal) -> {
            Settings.appWidth = width;
            sorter.updatePositions();
            redraw();
        });

        canvas.heightProperty().addListener((obs, oldVal, newVal) -> {
            Settings.appHeight = height;
            sorter.updatePositions();
            redraw();
        });

        // Set keypress detection with javafx
        this.stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
//            System.out.println("Code: " + event.getCode());
//            System.out.println("Character: " + event.getCharacter());
//            System.out.println("Text: " + event.getText());
            if (event.getCode() == KeyCode.L) {
                SortSelectionWindowFX.getApp().show();
            }

            if (!IN_THREAD) {
                if (event.getCode() == KeyCode.R) {
                    Thread thread;
                    if (event.isShiftDown()) {
                        thread = new Thread(this::sorter_randomizeClose);
                    } else if (event.isControlDown()) {
                        thread = new Thread(this::sorter_reverse);
                    } else {
                        thread = new Thread(this::sorter_randomize);
                    }
                    thread.start();
                }

                if (event.getCode() == KeyCode.DIGIT1) {
                    presetSettings1();
                } else if (event.getCode() == KeyCode.DIGIT2) {
                    presetSettings2();
                } else if (event.getCode() == KeyCode.DIGIT3) {
                    presetSettings3();
                } else if (event.getCode() == KeyCode.DIGIT4) {
                    presetSettings4();
                }

                if (event.getCode() == KeyCode.UP) {
                    Settings.totalItems += event.isShiftDown() ? 5 : 1;
                    sorter.createItems();
                } else if (event.getCode() == KeyCode.DOWN && Settings.totalItems > 5) {
                    Settings.totalItems -= event.isShiftDown() ? 5 : 1;
                    sorter.createItems();
                } else if (event.getCode() == KeyCode.LEFT) {
                    curSortIndex = ((curSortIndex - 1) % sortMethods.size() + sortMethods.size()) % sortMethods.size();
                    updateSelectionWindowSort();
                } else if (event.getCode() == KeyCode.RIGHT) {
                    curSortIndex = ((curSortIndex + 1) % sortMethods.size() + sortMethods.size()) % sortMethods.size();
                    updateSelectionWindowSort();
                } else if (event.getCode() == KeyCode.ENTER) {
                    sorter.resetTempItem();
                    Thread later = new Thread(() -> {
                        try {
                            sort_current();
                            redraw();
                        } catch (InvocationTargetException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    });
                    later.start();
//                thread("sort_current");
                } else if (event.getCode() == KeyCode.TAB) {
                    sorter.curDispType = ((sorter.curDispType + 1) % Settings.VALID_DISPTYPES.length + Settings.VALID_DISPTYPES.length) % Settings.VALID_DISPTYPES.length;
                    sorter.createItems();
                }
            }

            if (event.getCode() == KeyCode.OPEN_BRACKET) {
                if (event.isShiftDown() && Settings.frameSkip > 0) {  // {
                    Settings.frameSkip--;
                } else if (event.isControlDown() && Settings.swapFrames > 0) {
                    Settings.swapFrames--;
                    if (Settings.swapFrames == 0) {
                        Settings.animateSwap = false;
                    }
                } else if (!event.isShiftDown() && !event.isControlDown() && Settings.frameDelay > 0) {  // [
                    Settings.frameDelay--;
                }
            } else if (event.getCode() == KeyCode.CLOSE_BRACKET) {
                if (event.isShiftDown()) {  // {
                    Settings.frameSkip++;
                } else if (event.isControlDown()) {
                    Settings.swapFrames++;
                    if (Settings.swapFrames > 0) {
                        Settings.animateSwap = true;
                    }
                } else {  // [
                    Settings.frameDelay++;
                }
            } else if (event.getCode() == KeyCode.DIGIT9 && event.isShiftDown() && Settings.compDelay > 0) {
                Settings.compDelay--;
            } else if (event.getCode() == KeyCode.DIGIT9 && event.isShiftDown() && Settings.compDelay == 0) {
                Settings.showComparisons = false;
            } else if (event.getCode() == KeyCode.DIGIT0 && event.isShiftDown()) {
                if (!Settings.showComparisons) {
                    Settings.showComparisons = true;
                } else {
                    Settings.compDelay++;
                }
            } else if (event.getCode() == KeyCode.S) {
                Settings.BYSTEP = !Settings.BYSTEP;
            }

            if (event.getCode() == KeyCode.SPACE && Settings.BYSTEP) {
                sorter.NEXTSTEP = true;
            }


            if (event.getCode() == KeyCode.BACK_SPACE) {
                sorter.BREAK = true;
            }

            redraw();
        });
        return surface;
    }

    public void settings() {
        size(Settings.appWidth, Settings.appHeight, FX2D);

        graphWin = new GraphWindow(this);

        this.sorter = new Sorter(this);
        graphWin.setSorter(sorter);
        redraw();

        Method[] methods = sorter.getClass().getMethods();
        for (Method method : methods) {
            String methodName = method.getName();

            if (methodName.toLowerCase().contains("sort")) {
                sortMethods.add(method);
            } else if (methodName.toLowerCase().contains("randomize")) {
                randomizeMethods.add(method);
            }
        }

        sortMethods.sort(Comparator.comparing(a -> a.getAnnotation(Sort.class).dispName()));

        noLoop();
        redraw();
    }

    public void setup() {
        Platform.runLater(() -> {
            double horizInsetSum = stage.getWidth() - stage.getScene().getWidth();
            double vertInsetSum = stage.getHeight() - stage.getScene().getHeight();

            stage.setMinHeight(vertInsetSum + Settings.padTop + Settings.padBottom);
            stage.setMinWidth(horizInsetSum + Settings.padLeft + Settings.padRight);
        });
    }

    public void draw() {
        background(Settings.BACKGROUNDCOLOR);

        sorter.draw();

//        stroke(255);
//        noFill();
//        line(0, Settings.padTop + Settings.padBottom, width, Settings.padTop + Settings.padBottom);
//        line(0, Settings.padTop, width, Settings.padTop);
//        line(0, height - Settings.padBottom, width, height - Settings.padBottom);

        drawText();
    }

    public void drawText() {
        noStroke();
        fill(255);
        textSize(12);
        textAlign(LEFT);
        text(String.format("Total Items: %d", Settings.totalItems), 25, height - (Settings.padBottom / 2f + 18));
        text(String.format("Total Swaps: %d", sorter.totalSwaps), 25, height - (Settings.padBottom / 2f + 6));
        text(String.format("Total Comparisons: %d", sorter.totalComps), 25, height - (Settings.padBottom / 2f - 6));
        text(String.format("Total Array Accesses: %d", sorter.totalAccesses), 25, height - (Settings.padBottom / 2f - 18));
        textAlign(RIGHT);
        text(String.format("Frame Delay: %d", Settings.frameDelay), width - 25, height - (Settings.padBottom / 2f + 18));
        text(String.format("Frames to Skip: %d", Settings.frameSkip), width - 25, height - (Settings.padBottom / 2f + 6));
        if (Settings.showComparisons) {
            text(String.format("Compare Delay: %d", Settings.compDelay), width - 25, height - (Settings.padBottom / 2f - 6));
        } else {
            text("Comparisons OFF", width - 25, height - (Settings.padBottom / 2f - 6));
        }
        text(String.format("Swap Frames: %d", Settings.swapFrames), width - 25, height - (Settings.padBottom / 2f - 18));
        text("(Press numbers 1-2 to select a preset)", width - 25, height - (Settings.padBottom / 2f - 36));
        textAlign(CENTER);
        textSize(20);
        text(sortMethods.get(curSortIndex)
                        .getAnnotation(Sort.class).dispName(),
                width / 2f, height - (Settings.padBottom / 2f));
        textSize(12);
        if (IN_THREAD) {
//            if (!sorter.NEXTSTEP) {
            text(sorter.caption, width / 2f, height - (3 * Settings.padBottom / 4f));
//            }
            text("Press BACKSPACE to break", width / 2f, height - (Settings.padBottom / 2f - 20));
        } else {
            text("Press ENTER to select.", width / 2f, height - (Settings.padBottom / 2f - 18));
            text("Press LEFT or RIGHT arrow keys to switch.", width / 2f, height - (Settings.padBottom / 2f - 36));
        }
        if (sorter.getTempItem() != null) {
            textSize(12);
            textAlign(CENTER);
            text("temp", Settings.padLeft / 2f, height - Settings.padBottom + 15);
        }
    }

    private void updateSelectionWindowSort() {
//        SortSelectionWindowFX.getApp().selectionModel.select(curSortIndex);
        ListView<SortInfo> listView = SortSelectionWindowFX.getApp().selectionList;
        String curSortName = sortMethods.get(curSortIndex).getName();
        for (SortInfo sort : listView.getItems()) {
            if (curSortName.equals(sort.methodName) ||
                    (curSortName.equals("shellSort_Tokuda") && sort.name.equals("Shellsort"))) {
                if (curSortName.equals("shellSort_Tokuda") && sort.name.equals("Shellsort")) {
                    FLAG_SELECTEDTOKUDA = true;
                }
                Platform.runLater(() -> listView.getSelectionModel().select(sort));
            }
        }
    }

// ----- THREAD CALLS TO SORTER -----

    public void sort_current() throws InvocationTargetException, IllegalAccessException {
        sorter.BREAK = false;
        IN_THREAD = true;
        sorter.resetCounts();
        sorter.resetColors();
        if (sortMethods.get(curSortIndex).getName().equals("Heapsort") ||
                sortMethods.get(curSortIndex).getName().equals("Smoothsort")) {
            graphWin.heapSortName = sortMethods.get(curSortIndex).getName();
        } else {
            graphWin.heapSortName = "";
        }
        sortMethods.get(curSortIndex).invoke(sorter);
        sorter.horizLines.clear();
        sorter.vertLines.clear();
        IN_THREAD = false;
        sorter.BREAK = false;
        redraw();
    }

    public void sorter_randomize() {
        sorter.BREAK = false;
        IN_THREAD = true;
        sorter.resetCounts();
        sorter.resetColors();
        sorter.randomize();
        redraw();
        IN_THREAD = false;
        sorter.BREAK = false;
    }

    public void sorter_reverse() {
        sorter.BREAK = false;
        IN_THREAD = true;
        sorter.resetCounts();
        sorter.resetColors();
        sorter.reverseItems();
        redraw();
        IN_THREAD = false;
        sorter.BREAK = false;
    }

    public void sorter_randomizeClose() {
        sorter.BREAK = false;
        IN_THREAD = true;
        sorter.resetCounts();
        sorter.randomizeClose();
        redraw();
        IN_THREAD = false;
        sorter.BREAK = false;
    }

// ----- PRESET SETTINGS -----

    public void presetSettings1() {
        // Fast viewing
        Settings.totalItems = 250;
        Settings.frameDelay = 5;
        Settings.frameSkip = 5;
        Settings.itemSpace = 0;
        sorter.curDispType = 0;
        Settings.animateSwap = false;
        Settings.swapFrames = 0;
        Settings.randomItemHeights = false;
        Settings.compDelay = 0;
        sorter.createItems();
    }

    public void presetSettings2() {
        // Slow viewing
        Settings.totalItems = 25;
        Settings.frameDelay = 20;
        Settings.frameSkip = 0;
        Settings.itemSpace = 0;
        sorter.curDispType = 0;
        Settings.animateSwap = true;
        Settings.swapFrames = 10;
        Settings.randomItemHeights = false;
        Settings.compDelay = 120;
        sorter.createItems();
    }

    public void presetSettings3() {
        // Randomized, fast viewing
        Settings.totalItems = 300;
        Settings.frameDelay = 5;
        Settings.frameSkip = 5;
        Settings.itemSpace = 0;
        sorter.curDispType = 0;
        Settings.animateSwap = false;
        Settings.swapFrames = 0;
        Settings.randomItemHeights = true;
        Settings.compDelay = 0;
        sorter.createItems();
    }

    public void presetSettings4() {
        // Randomized, slow viewing
        Settings.totalItems = 25;
        Settings.frameDelay = 20;
        Settings.frameSkip = 0;
        Settings.itemSpace = 0;
        sorter.curDispType = 0;
        Settings.animateSwap = true;
        Settings.swapFrames = 10;
        Settings.randomItemHeights = true;
        Settings.compDelay = 120;
        sorter.createItems();
    }

//    public void drawSecondaryGraph() {
//        if (IN_THREAD) {
//
//            if (sortNames.get(curSort).equals("Heapsort")) {
//                sorter.drawMaxHeap(graphWin);
//            } else if (sortNames.get(curSort).equals("Smoothsort")) {
////                    sorter.drawLeonardoHeap(graphWin);
//            }
//        }
//    }


}
