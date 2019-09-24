package lowosos;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class GUI extends Application{

    private static String curDir;
    private static PropertiesUtil propUtil;
    private static TableView<FileWrapper> table;
    private static Process p;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop(){
        System.exit(0);
    }

    @Override
    public void start(Stage stage) throws IOException, URISyntaxException {
        envInit();
        tableInit();
        Button b1 = b1Init();
        HBox hbox = new HBox(b1);
        hbox.setPadding(new Insets(10));
        hbox.setAlignment(Pos.CENTER);

        final VBox vbox = new VBox();
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.getChildren().setAll(table, hbox);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.maxWidthProperty().bind(vbox.heightProperty());

        Scene scene = new Scene(vbox);

        stage.setTitle("Tomb Raider 2 Save Manager");
        stage.setWidth(700);
        stage.setHeight(800);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    private static Button b1Init() {
        Button b1 = new Button();
        b1.setText("Clean run");
        b1.setOnAction(event -> {
            if (p == null || !p.isAlive()) {
                Util.deleteSaves(curDir);
                runTR();
                if (propUtil.isCloseAfterCleanRun()) System.exit(0);
            }
            else Util.gameRunningAlert();
        });
        return b1;
    }

    private static void envInit() throws URISyntaxException, IOException {
        setCurDir();
        propUtil = new PropertiesUtil(curDir + "\\config.properties");
    }

    private static void setCurDir() throws URISyntaxException {
        if (GUI.class.getResource("GUI.class").toString().startsWith("jar")) {
            curDir = new File(GUI.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getPath();
        }
        else curDir = System.getProperty("user.dir");
    }

    static void refreshTable() {
        table.refresh();
        table.sort();
    }

    private static TableView tableInit() {
        TableColumn<FileWrapper, String> folderName = new TableColumn("Folder Name");
        folderName.setComparator(getFolderNameComparator());
        folderName.setSortType(TableColumn.SortType.ASCENDING);
        TableColumn<FileWrapper, String> levels = new TableColumn("Levels included");
        levels.setComparator(getLevelsComparator());
        TableColumn<FileWrapper, String> descs = new TableColumn("Description");
        folderName.setMaxWidth(1f * Integer.MAX_VALUE * 30);
        levels.setMaxWidth(1f * Integer.MAX_VALUE * 20);
        descs.setMaxWidth(1f * Integer.MAX_VALUE * 50);
        folderName.setCellValueFactory(new PropertyValueFactory<>("folderName"));
        levels.setCellValueFactory(new PropertyValueFactory<>("levels"));
        descs.setCellValueFactory(new PropertyValueFactory<>("description"));
        table = new TableView<>();
        table.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(folderName, descs, levels);
        table.setOnMousePressed(GUI::tableCellClickedHandler);
        ObservableList<FileWrapper> sfList = listInit();
        table.setItems(sfList);
        table.getSortOrder().add(folderName);
        return table;
    }

    private static ObservableList<FileWrapper> listInit() {
        File savemgrFolder = new File(curDir + "\\savemgr");
        if (!savemgrFolder.isDirectory()) {
            Util.alert("No savemgr folder in the Tomb Raider 2 directory.");
            System.exit(1);
        }
        File[] files = savemgrFolder.listFiles();
        ObservableList<FileWrapper> wList;
        if (files != null) {
             wList = FXCollections.observableArrayList(Arrays.stream(files).filter(File::isDirectory).map(FileWrapper::new).toArray(FileWrapper[]::new));
        } else wList = FXCollections.observableArrayList();
        ObservableList<FileWrapper> sfList = FXCollections.observableArrayList(wList.stream().filter(f -> Util.isSaveFolder(f.getFile())).collect(Collectors.toList()));
        WatcherThread wt = new WatcherThread(curDir + "\\savemgr", wList, sfList);
        wt.start();
        return sfList;
    }

    private static Comparator<String> getLevelsComparator() {
        return (s1, s2) -> {
            int[] arr1 = Arrays.stream(s1.split(", ")).mapToInt(Integer::parseInt).toArray();
            int[] arr2 = Arrays.stream(s2.split(", ")).mapToInt(Integer::parseInt).toArray();
            for (int i = 0; i < Math.min(arr1.length, arr2.length); i++) {
                if (arr1[i] != arr2[i]) return Integer.compare(arr1[i], arr2[i]);
            }
            return Integer.compare(arr1.length, arr2.length);
        };
    }

    private static Comparator<String> getFolderNameComparator() {
        return (s1, s2) -> {
            if (s1.startsWith("0") || s2.startsWith("0")) return s1.compareTo(s2);
            String sn1 = "";
            for (Character c1 : s1.toCharArray()) {
                if (Character.isDigit(c1)) sn1 += c1;
                else break;
            }
            if (sn1.equals("")) return s1.compareTo(s2);
            String sn2 = "";
            for (Character c2 : s2.toCharArray()) {
                if (Character.isDigit(c2)) sn2 += c2;
                else break;
            }
            if (sn2.equals("")) return s1.compareTo(s2);
            Integer n1 = Integer.parseInt(sn1);
            Integer n2 = Integer.parseInt(sn2);
            return Integer.compare(Integer.parseInt(sn1), Integer.parseInt(sn2));
        };
    }

    private static void tableCellClickedHandler(MouseEvent event) {
            if (event.isPrimaryButtonDown() && event.getClickCount() == 2) {
                File folder = table.getSelectionModel().getSelectedItem().getFile();
                if (p == null || !p.isAlive()) {
                    Util.deleteSaves(curDir);
                    Util.copySaves(folder, curDir);
                    runTR();
                }
                else Util.gameRunningAlert();
            }
    }

    private static void runTR() {
        String exe = propUtil.getExe();
        if (exe.isEmpty()) {
            File[] files = new File(curDir).listFiles();
            if (files == null) {
                Util.alert("There is no files in the directory");
                System.exit(1);
            }
            files = Arrays.stream(files).filter(f -> f.getName().endsWith(".exe")).toArray(File[]::new);
            if (files.length == 0) {
                Util.alert("There is no executable in the directory.");
                System.exit(1);
            }
            if (files.length == 1) {
                try {
                    p = Runtime.getRuntime().exec(files[0].getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {
                Util.alert("There is more than one .exe file in the Tomb Raider 2 home directory. You need to specify the one to be used in config.properties.");
                System.exit(1);
            }
        } else {
            try {
                p = Runtime.getRuntime().exec(curDir + "\\" + exe);
            } catch (IOException e) {
                Util.alert("The Tomb Raider 2 exe file specified in config.properties doesn't exist. Please check it again.");
                System.exit(1);
            }
        }
    }
}
