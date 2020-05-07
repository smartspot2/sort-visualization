package options;

import processing.core.PApplet;

public class OptionsWindow extends PApplet {
    private final PApplet mainApp;

    public OptionsWindow(PApplet mainApp) {
        super();
        this.mainApp = mainApp;
        PApplet.runSketch(new String[]{this.getClass().getSimpleName()}, this);
    }

    public void draw() {
        background(255);
        noStroke();

        //
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
}
