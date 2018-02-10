/*
 *
 * Copyright 2015-2018 magiclen.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.magiclen.imageresizerlight;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.magiclen.magicimage.ImageBuffer;
import org.magiclen.magicimage.ImageExport;
import org.magiclen.magicimage.ImageInterlacer;
import org.magiclen.magicimage.ImageResize;

/**
 * Image Resizer Light
 *
 * @author Magic Len
 */
public class ImageResizerLight extends Application {

    // -----Class Constant-----
    /**
     * Use the max side length of the current screen to set the defalut value of
     * picture's max side.
     */
    private static final int DEFAULT_MAX_SIDE;
    private static final int SCREEN_WIDTH, SCREEN_HEIGHT;
    /**
     * The list of filename extensions that Image Resizer Light supports.
     */
    private static final String[] SUPPORT_IMAGE_EXTENDS = {".jpg", ".jpeg", ".png", ".bmp", ".tiff", ".gif"};

    // -----Initial Static-----
    static {
        final Screen mainScreen = Screen.getPrimary();
        final Rectangle2D screenRectangle = mainScreen.getBounds();
        SCREEN_WIDTH = (int) screenRectangle.getWidth();
        SCREEN_HEIGHT = (int) screenRectangle.getHeight();
        DEFAULT_MAX_SIDE = Math.max(SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    // -----Object Constant-----
    /**
     * The default value of width.
     */
    private final int WIDTH = 500;
    /**
     * The default value of Height.
     */
    private final int HEIGHT = 500;
    /**
     * The default distance of controls.
     */
    private final int GAP = 3;
    /**
     * The default padding of the main frame.
     */
    private final int PADDING_GAP = 10;
    /**
     * The default size of text.
     */
    private final float FONT_SIZE = 18f;
    /**
     * The default height of progress bar.
     */
    private final float PROGRESS_HEIGHT = 50;
    /**
     * The list of image files.
     */
    private final ArrayList<File> imageFileList = new ArrayList<>();
    /**
     * The list of folders.
     */
    private final ArrayList<File> folderList = new ArrayList<>();

    // -----Object Variable-----
    /**
     * The font of text.
     */
    private Font font;
    /**
     * The margin of controls.
     */
    private Insets insets;
    /**
     * The padding of the main frame.
     */
    private Insets padding;
    /**
     * The stage of this application.
     */
    private Stage MAIN_STAGE;
    /**
     * The scene of this application.
     */
    private Scene MAIN_SCENE;
    /**
     * The root panel of controls.
     */
    private VBox MAIN_ROOT;
    private Label lSource, lDestination, lMaxSide, lQuality, lAuthor;
    private TextField tfSource, tfDestination, tfMaxSide, tfInterlace;
    private Slider sQuality;
    private Button bStartOrStop;
    private BorderPane bpInterlace;
    private CheckBox cbOnlyShrink, cbInterlace;
    private ProgressBar pbProgress;
    private DirectoryChooser directoryChooser;
    private FileChooser fcChooser;

    private boolean running, stopping;

    // -----Object Method-----
    /**
     * Lock or unclock controls.
     *
     * @param disable whether to disable controls
     */
    private void lockOrUnlock(final boolean disable) {
        tfSource.setDisable(disable);
        tfDestination.setDisable(disable);
        tfMaxSide.setDisable(disable);
        tfInterlace.setDisable(disable);
        sQuality.setDisable(disable);
        cbOnlyShrink.setDisable(disable);
        cbInterlace.setDisable(disable);
        bStartOrStop.setText(disable ? "Stop" : "Start");
    }

    /**
     * Run tasks.
     */
    private void run() {
        if (running) {
            return;
        }
        running = true;
        stopping = false;
        lockOrUnlock(true);
        folderList.clear();
        imageFileList.clear();
        pbProgress.setProgress(-1);

        final String sourcePath = tfSource.getText().trim();
        final String destinationPath = tfDestination.getText().trim();
        final String sideValue = tfMaxSide.getText().trim();
        try {
            if (cbInterlace.isSelected()) {
                final String magickPath = tfInterlace.getText().trim();
                if (magickPath.length() == 0) {
                    showAlertDialog(AlertType.INFORMATION, "Hint", null, "You need input the path of a ImageMagick executable binary file usually named 'magick'.");
                    throw new Exception();
                }
                ImageInterlacer.MAGICK_PATH = magickPath;
                if (!ImageInterlacer.isAvailable()) {
                    showAlertDialog(AlertType.WARNING, "Hint", null, "You need input the path of a ImageMagick executable binary file usually named 'magick' correctly.");
                    throw new Exception();
                }
            }
            if (sourcePath.length() == 0) {
                showAlertDialog(AlertType.INFORMATION, "Hint", null, "You need input the path of a source folder.");
                throw new Exception();
            }
            if (destinationPath.length() == 0) {
                showAlertDialog(AlertType.INFORMATION, "Hint", null, "You need input the path of a destination folder.");
                throw new Exception();
            }
            if (sideValue.length() == 0) {
                showAlertDialog(AlertType.INFORMATION, "Hint", null, "You need to input the length of the max side of your output picture.");
                throw new Exception();
            }
            final File source = new File(sourcePath);
            if (!source.exists() || !source.isDirectory()) {
                showAlertDialog(AlertType.INFORMATION, "Hint", null, "You need to input the path of a source folder correctly.");
                throw new Exception();
            }
            folderList.add(source);

            final File destination = new File(destinationPath).getAbsoluteFile();
            if (folderList.contains(destination)) {
                showAlertDialog(AlertType.WARNING, "Hint", null, "You cannot set your destination in the same folder as the source.");
                throw new Exception();
            }
            final File destinationParent = destination.getParentFile();
            if (destinationParent != null && destinationParent.getAbsolutePath().startsWith(source.getAbsolutePath())) {
                showAlertDialog(AlertType.WARNING, "Hint", null, "You cannot set your destination in the subfolders of the source.");
                throw new Exception();
            }
            if (!destination.exists()) {
                if (!destination.mkdirs()) {
                    showAlertDialog(AlertType.ERROR, "Hint", null, "Cannot create folder: ".concat(destination.getAbsolutePath()));
                    throw new Exception();
                }
            } else if (!destination.isDirectory()) {
                showAlertDialog(AlertType.INFORMATION, "Hint", null, "You need to input the path of a destination folder correctly.");
                throw new Exception();
            } else if (!destination.canWrite()) {
                showAlertDialog(AlertType.ERROR, "Hint", null, "Cannot write files in the folder: ".concat(destination.getAbsolutePath()));
                throw new Exception();
            }

            final int maxSideSize;
            try {
                maxSideSize = Integer.parseInt(sideValue);
                if (maxSideSize <= 0) {
                    throw new Exception();
                }
            } catch (Exception ex) {
                showAlertDialog(AlertType.INFORMATION, "Hint", null, "You need to input the length of the max side of your output picture correctly.");
                throw new Exception();
            }

            final boolean onlyShrink = cbOnlyShrink.isSelected();
            final boolean interlace = cbInterlace.isSelected();

            final float quality = (float) (sQuality.getValue() / 100.0f);

            fetchFiles(source, true);
            final int fileCount = imageFileList.size();

            if (fileCount > 0) {
                if (showConfirmDialog("Hint", String.format("%d file(s) to convert.", fileCount), "Do you want to continue?")) {
                    new Thread() {
                        @Override
                        public void run() {
                            int doneCount = 0;
                            int successCount = 0;
                            final String sourcePath = source.getAbsolutePath();
                            final int sourcePathLength = sourcePath.length();
                            for (final File file : imageFileList) {
                                if (stopping) {
                                    break;
                                }
                                ++doneCount;
                                try {
                                    final String imgPath = file.getAbsolutePath();
                                    final File newFile = new File(destination, imgPath.substring(sourcePathLength, imgPath.length()));
                                    newFile.getAbsoluteFile().getParentFile().mkdirs();
                                    final BufferedImage bi = ImageBuffer.getBufferedImages(file)[0];
                                    final BufferedImage result = ImageResize.resize(bi, maxSideSize, -1, onlyShrink, true);
                                    if (ImageExport.exportToJPEG(result, newFile, quality, false) != null) {
                                        if (interlace) {
                                            if (ImageInterlacer.setInterlace(newFile, ImageInterlacer.Interlace.PLANE, false)) {
                                                successCount++;
                                            } else {
                                                newFile.delete();
                                            }
                                        } else {
                                            successCount++;
                                        }
                                    }
                                } catch (final Exception ex) {
                                    //do nothing
                                }
                                final double p = doneCount * 1f / fileCount;
                                Platform.runLater(() -> {
                                    pbProgress.setProgress(p);
                                });
                            }
                            final int dc = doneCount;
                            final int sc = successCount;
                            Platform.runLater(() -> {
                                if (dc < fileCount) {
                                    showAlertDialog(AlertType.INFORMATION, "Hint", null, String.format("Stopped!(%d/%d/%d)", sc, dc, fileCount));
                                } else {
                                    showAlertDialog(AlertType.INFORMATION, "Hint", null, String.format("Finished!(%d/%d)", sc, fileCount));
                                }

                                lockOrUnlock(false);
                            });
                            running = false;
                        }
                    }.start();
                } else {
                    throw new Exception();
                }
            } else {
                showAlertDialog(AlertType.INFORMATION, "Hint", null, "Nothing to resize.");
                throw new Exception();
            }
        } catch (final Exception ex) {
            lockOrUnlock(false);
            running = false;
            pbProgress.setProgress(0);
        }
    }

    /**
     * Stop running tasks.
     */
    private void stopDoing() {
        if (!running) {
            return;
        }
        if (stopConfirm()) {
            stopping = true;
        }
    }

    /**
     * Fix the height of an alert dialog.
     *
     * @param alert input an alert dialog
     */
    private void fixAlertHeight(final Alert alert) {
        alert.getDialogPane().getChildren().stream().filter(node -> node instanceof Label).forEach(node -> {
            final Label label = ((Label) node);
            label.setFont(font);
            label.setMinHeight(Region.USE_PREF_SIZE);
        });
    }

    /**
     * Confirm whether stop or not.
     *
     * @return yes or no
     */
    private boolean stopConfirm() {
        return showConfirmDialog("Hint", "Do you really want to stop the running tasks?", "If you stop your tasks, they cannot be continued.");
    }

    /**
     * Show an confirm dialog.
     *
     * @param title input the title
     * @param header input the header
     * @param content input the content
     * @return yes or no
     */
    private boolean showConfirmDialog(final String title, final String header, final String content) {
        final Optional<ButtonType> opt = showAlertDialog(AlertType.CONFIRMATION, title, header, content);
        final ButtonType rtn = opt.get();
        return rtn == ButtonType.OK;
    }

    /**
     * Show an alert dialog.
     *
     * @param type input the alert type
     * @param title input the title
     * @param header input the header
     * @param content input the content
     * @return the button that user clicked
     */
    private Optional<ButtonType> showAlertDialog(final AlertType type, final String title, final String header, final String content) {
        final Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        fixAlertHeight(alert);
        return alert.showAndWait();
    }

    /**
     * Recursively add directories and files into lists.
     *
     * @param file input a root folder
     * @param askSubFolderask whether to extend to subfolders
     *
     */
    private void fetchFiles(final File file, boolean askSubFolder) {
        final File subFiles[] = file.listFiles(pathname -> {
            if (pathname.canRead()) {
                if (pathname.isDirectory()) {
                    return true;
                } else if (pathname.isFile()) {
                    for (final String extend : SUPPORT_IMAGE_EXTENDS) {
                        if (pathname.getName().toLowerCase().endsWith(extend)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        });
        if (askSubFolder) {
            boolean subFolder = true;
            for (final File subFile : subFiles) {
                if (subFile.isDirectory()) {
                    if (askSubFolder) {
                        subFolder = showConfirmDialog("Hint", null, "Do you want to input images from subfolders?");
                        askSubFolder = false;
                    }
                    if (subFolder) {
                        folderList.add(subFile);
                        fetchFiles(subFile, false);
                    }
                } else {
                    imageFileList.add(subFile);
                }
            }
        } else {
            for (final File subFile : subFiles) {
                if (subFile.isDirectory()) {
                    folderList.add(subFile);
                    fetchFiles(subFile, false);
                } else {
                    imageFileList.add(subFile);
                }
            }
        }
    }

    /**
     * Choose a ImageMagick executable binary file.
     */
    private void chooseMagickFile() {
        fcChooser.setTitle("Choose a ImageMagick executable binary file usually named 'magick'");
        final File file = fcChooser.showOpenDialog(MAIN_STAGE);
        if (file != null) {
            fcChooser.setInitialDirectory(file.getParentFile());
            tfInterlace.setText(file.getAbsolutePath());
        }
    }

    /**
     * Choose a folder to input images.
     */
    private void chooseInputFolder() {
        directoryChooser.setTitle("Choose a Folder to Input Images");
        final File folder = directoryChooser.showDialog(MAIN_STAGE);
        if (folder != null) {
            directoryChooser.setInitialDirectory(folder);
            tfSource.setText(folder.getAbsolutePath());
        }
    }

    /**
     * Choose a folder to save images.
     */
    private void chooseOutputFolder() {
        directoryChooser.setTitle("Choose a Folder to Save Images");
        final File folder = directoryChooser.showDialog(MAIN_STAGE);
        if (folder != null) {
            directoryChooser.setInitialDirectory(folder);
            tfDestination.setText(folder.getAbsolutePath());
        }
    }

    /**
     * Add events.
     */
    private void addActions() {
        MAIN_STAGE.setOnCloseRequest(e -> {
            if (running) {
                e.consume();
                showAlertDialog(AlertType.WARNING, "Hint", "You cannot close now.", "There are some works need to be done. If you want to close, you should wait for them or stop them.");
            }
        });
        tfSource.setOnMouseClicked(e -> {
            if (e.getClickCount() == 3) {
                chooseInputFolder();
            }
        });
        tfDestination.setOnMouseClicked(e -> {
            if (e.getClickCount() == 3) {
                chooseOutputFolder();
            }
        });
        tfInterlace.setOnMouseClicked(e -> {
            if (tfInterlace.isEditable() && e.getClickCount() == 3) {
                chooseMagickFile();
            }
        });
        tfSource.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                chooseInputFolder();
            }
        });
        tfDestination.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                chooseOutputFolder();
            }
        });
        tfInterlace.setOnKeyPressed(e -> {
            if (tfInterlace.isEditable() && e.getCode() == KeyCode.ENTER) {
                chooseMagickFile();
            }
        });

        bStartOrStop.setOnAction(e -> {
            if (running) {
                stopDoing();
            } else {
                run();
            }
        });
        lAuthor.setOnMouseClicked((e) -> {
            final URI uri = URI.create("https://magiclen.org/");
            new Thread(() -> {
                try {
                    Desktop.getDesktop().browse(uri);
                } catch (final IOException ex) {
                }
            }).start();
        });
    }

    /**
     * Construct the primary stage.
     *
     * @param primaryStage JavaFX will input a stage instance here
     */
    @Override
    public void start(final Stage primaryStage) {
        font = new Font(FONT_SIZE);
        insets = new Insets(GAP, GAP, GAP, GAP);
        padding = new Insets(PADDING_GAP, PADDING_GAP, PADDING_GAP, PADDING_GAP);

        directoryChooser = new DirectoryChooser();
        fcChooser = new FileChooser();

        lSource = new Label("Source Folder:");
        lDestination = new Label("Destination Folder:");
        lMaxSide = new Label("Side Maximum:");
        lQuality = new Label("Quality:");
        lAuthor = new Label("Powered by magiclen.org");

        lSource.setFont(font);
        lDestination.setFont(font);
        lMaxSide.setFont(font);
        lQuality.setFont(font);
        lAuthor.setFont(font);

        lAuthor.setAlignment(Pos.BASELINE_RIGHT);
        lAuthor.setMaxWidth(Integer.MAX_VALUE);

        tfSource = new TextField();
        tfDestination = new TextField();
        tfMaxSide = new TextField(String.valueOf(DEFAULT_MAX_SIDE));
        tfInterlace = new TextField();

        tfSource.setFont(font);
        tfDestination.setFont(font);
        tfMaxSide.setFont(font);
        tfInterlace.setFont(font);

        tfSource.setPromptText("Click here 3 times or press enter to choose a folder.");
        tfDestination.setPromptText("Click here 3 times or press enter to choose a folder.");
        tfMaxSide.setPromptText("Input a value which must be bigger than zero.");
        tfInterlace.setPromptText("Interlace is disabled.");

        cbOnlyShrink = new CheckBox("Only Shrink");
        cbInterlace = new CheckBox("Interlace");

        cbOnlyShrink.setFont(font);
        cbOnlyShrink.setMaxWidth(Integer.MAX_VALUE);
        cbOnlyShrink.setSelected(true);
        cbInterlace.setFont(font);
        cbInterlace.setSelected(false);

        tfInterlace.editableProperty().bind(cbInterlace.selectedProperty());
        tfInterlace.editableProperty().addListener((e) -> {
            if (tfInterlace.isEditable()) {
                tfInterlace.setPromptText("Click here 3 times or press enter to choose a magick file.");
            } else {
                tfInterlace.setPromptText("Interlace is disabled.");
            }
        });

        sQuality = new Slider(0, 100, 80);
        sQuality.setShowTickLabels(true);
        sQuality.setShowTickMarks(true);
        sQuality.setMajorTickUnit(10);
        sQuality.setMinorTickCount(1);
        sQuality.setStyle(String.format("-fx-font-size: %.0fpx;", FONT_SIZE));
        sQuality.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(final Double d) {
                if (d == 100) {
                    return "High";
                } else if (d == 0) {
                    return "Low";
                }
                return String.valueOf(d.intValue());
            }

            @Override
            public Double fromString(final String string) {
                switch (string) {
                    case "High":
                        return 100d;
                    case "Low":
                        return 0d;
                }
                return Double.parseDouble(string);
            }

        });

        pbProgress = new ProgressBar(0);
        pbProgress.setMaxSize(Double.MAX_VALUE, PROGRESS_HEIGHT);

        bStartOrStop = new Button("Start");
        bStartOrStop.setFont(font);
        bStartOrStop.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        BorderPane.setMargin(cbInterlace, insets);
        BorderPane.setMargin(tfInterlace, insets);
        BorderPane.setAlignment(cbInterlace, Pos.CENTER);
        BorderPane.setAlignment(tfInterlace, Pos.CENTER);
        bpInterlace = new BorderPane(tfInterlace);
        bpInterlace.setLeft(cbInterlace);

        final Tooltip tipInterlace = new Tooltip("It will be user-friendly on web pages.");
        tipInterlace.setFont(font);
        Tooltip.install(cbInterlace, tipInterlace);

        final Tooltip tipQuality = new Tooltip("The higher quality, the bigger file size.");
        tipQuality.setFont(font);
        Tooltip.install(sQuality, tipQuality);

        final Tooltip tipAuthor = new Tooltip("Magic Len");
        tipAuthor.setFont(font);
        Tooltip.install(lAuthor, tipAuthor);

        VBox.setMargin(lSource, insets);
        VBox.setMargin(lDestination, insets);
        VBox.setMargin(lMaxSide, insets);
        VBox.setMargin(lQuality, insets);
        VBox.setMargin(tfSource, insets);
        VBox.setMargin(tfDestination, insets);
        VBox.setMargin(tfMaxSide, insets);
        VBox.setMargin(cbOnlyShrink, insets);
        VBox.setMargin(sQuality, insets);
        VBox.setMargin(pbProgress, insets);
        VBox.setMargin(bStartOrStop, insets);
        VBox.setMargin(lAuthor, insets);

        VBox.setVgrow(pbProgress, Priority.ALWAYS);
        VBox.setVgrow(bStartOrStop, Priority.ALWAYS);

        MAIN_ROOT = new VBox();
        MAIN_ROOT.setAlignment(Pos.TOP_LEFT);
        MAIN_ROOT.setPadding(padding);
        MAIN_ROOT.getChildren().addAll(lSource, tfSource, lDestination, tfDestination, lMaxSide, tfMaxSide, bpInterlace, cbOnlyShrink, lQuality, sQuality, pbProgress, bStartOrStop, lAuthor);

        MAIN_SCENE = new Scene(MAIN_ROOT, WIDTH, HEIGHT);

        primaryStage.setResizable(true);
        primaryStage.setTitle("Image Resizer Light");
        primaryStage.setScene(MAIN_SCENE);
        primaryStage.setX((SCREEN_WIDTH - WIDTH) / 2);
        primaryStage.setY((SCREEN_HEIGHT - HEIGHT) / 2);

        MAIN_STAGE = primaryStage;

        primaryStage.show();

        addActions();
    }

    /**
     * The initiation of this program.
     *
     * @param args not used
     */
    public static void main(final String[] args) {
        launch(args);
    }

}
