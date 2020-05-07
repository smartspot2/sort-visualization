package sorter;

import options.Settings;
import processing.core.PApplet;

public class GraphWindow extends PApplet {
    private final PApplet mainApp;
    public volatile String heapSortName = "";
    private Sorter sorter;
    private boolean isVisible = false;

    public GraphWindow(PApplet mainApp) {
        super();
        this.mainApp = mainApp;
        PApplet.runSketch(new String[]{this.getClass().getSimpleName()}, this);
    }

    public void settings() {
//        size(900, 950);
        size(700, 700);
    }

    public void setup() {
        this.surface.setVisible(false);
        this.surface.setResizable(true);
    }

    public void draw() {
        if (!heapSortName.equals("")) {
            background(0);
            noStroke();
            fill(255);
            textSize(12);
            textAlign(CENTER, CENTER);
            text(sorter.caption, width / 2f, height - Settings.padBottom / 2f);

            if (!this.isVisible) {  // ensure it only runs once
                this.surface.setVisible(true);
                this.isVisible = true;
            }
        } else {
            if (this.isVisible) {   // ensure it only runs once
                this.surface.setVisible(false);
                this.isVisible = false;
            }
        }

        if (heapSortName.equals("Heapsort")) {
            sorter.drawMaxHeap(this);
        } else if (heapSortName.equals("Smoothsort")) {

        }
    }

    public void setSorter(Sorter newSorter) {
        this.sorter = newSorter;
    }

    public void keyPressed() {
        this.mainApp.key = key;
        this.mainApp.keyCode = keyCode;
        this.mainApp.keyPressed();
    }

    public void keyReleased() {
        this.mainApp.key = key;
        this.mainApp.keyCode = keyCode;
        this.mainApp.keyReleased();
    }

    public void exit() {
        this.surface.setVisible(false);
    }
}
