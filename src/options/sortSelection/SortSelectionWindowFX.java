package options.sortSelection;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import options.yaml.InfoWrapper;
import options.yaml.RandomizerInfo;
import options.yaml.SortInfo;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

public class SortSelectionWindowFX extends Application {
    public static boolean initDone = false;
    private static SortSelectionWindowFX APP;
    public volatile Stage stage;
    public ListView<SortInfo> selectionList;
    private List<RandomizerInfo> randomizerInfoList;
    private List<SortInfo> sortInfoList;

    public SortSelectionWindowFX() {
        APP = this;
    }

    /**
     * Get application instance, and launch application if it doesn't exist.
     *
     * @return current application instance
     */
    public static synchronized SortSelectionWindowFX getApp() {
        if (APP == null) {
            new SortSelectionWindowFX();
        }
        return APP;
    }

    public void init() {
        Yaml yaml = new Yaml(new Constructor(InfoWrapper.class));
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File("src/options/yaml/SortDescriptions.yaml"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        InfoWrapper wrapper = yaml.load(inputStream);
        wrapper.randomizers.sort(Comparator.comparing(a -> a.name));
        this.randomizerInfoList = wrapper.randomizers;
        ObservableList<String> preferredOrder = FXCollections.observableArrayList(
                "bubbleSort", "insertionSort", "selectionSort", "mergeSort", "iterativeMergeSort",
                "quickSort", "gnomeSort", "oddEvenSort", "shellSort_Ciura", "stoogeSort", "slowSort",
                "lsdRadixSort", "msdRadixSort", "heapSort", "smoothSort"
        );
        wrapper.sorts.sort(Comparator.comparing(a -> preferredOrder.indexOf(a.methodName)));
        this.sortInfoList = wrapper.sorts;
    }

    @Override
    public void start(Stage stage) {
        synchronized (Stage.class) {
            this.stage = stage;
        }

        // Selection
        StackPane leftPane = new StackPane();
        ObservableList<SortInfo> lst = FXCollections.observableArrayList(sortInfoList);
        this.selectionList = new ListView<>(lst);
        this.selectionList.setCellFactory(param -> new ListCell<SortInfo>() {
                    @Override
                    protected void updateItem(SortInfo item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            // Set custom text to sort name
                            setText(item.name);
                            setFont(new Font(16));
                        }
                    }
                }
        );
        leftPane.getChildren().add(this.selectionList);

        // Description
        WebView rightPane = new WebView();
        // TODO: remove right-click "Reload Page" option
        rightPane.getEngine().load(new File("src/options/sortSelection/SortDescription.html").toURI().toString());

        this.selectionList.getSelectionModel().selectedItemProperty().addListener((observable, oldSort, newSort) -> {
                    WebEngine engine = rightPane.getEngine();
                    engine.executeScript("document.setSortObject(" + newSort.asJSObject() + ")");
                    Event sortSelect = new Events.SortSelect(Events.SORT_SELECT);
                    this.stage.fireEvent(sortSelect);  // Signal to mirror in processing sketch
                }
        );

        // Split window, assign panes
        SplitPane split = new SplitPane();
        split.getItems().addAll(leftPane, rightPane);
        split.setDividerPositions(0.25);
        SplitPane.setResizableWithParent(leftPane, false);

        // Scene init
        Scene scene = new Scene(split, 900, 750);
        Platform.setImplicitExit(false);

        stage.setScene(scene);
        // Flag that everything is done initializing
        initDone = true;
    }

    public void show() {
        Platform.runLater(() -> stage.show());
    }

    public void exit() {
        Platform.exit();
    }
}
