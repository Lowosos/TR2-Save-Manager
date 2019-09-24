package lowosos;

import javafx.beans.property.SimpleStringProperty;

import java.io.*;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.nio.file.StandardWatchEventKinds.*;

public class FileWrapper {
    private File file;
    private SimpleStringProperty levels;
    private SimpleStringProperty description;
    private SimpleStringProperty folderName;
    private WatchKey watchKey;

    FileWrapper(File file) {
        this.file = file;
        folderName = new SimpleStringProperty();
        levels = new SimpleStringProperty();
        description = new SimpleStringProperty();
        folderName.set(file.getName());
        if (Util.isSaveFolder(file)) {
            levelsAndDescInit();
        }
    }

    public void levelsAndDescInit() {
        levels.set(Util.getSaveLevelsInFolder(file));
        description.set(Util.loadDescriptionIn(file.getAbsolutePath()));
    }

    public void register(WatchService service) throws IOException {
        watchKey = Paths.get(file.getAbsolutePath()).register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    }

    public void cancelWatchKey() {
        watchKey.cancel();
    }

    public SimpleStringProperty levelsProperty() {
        return levels;
    }

    public SimpleStringProperty descriptionProperty() {
        return description;
    }

    public SimpleStringProperty folderNameProperty() {
        return folderName;
    }

    File getFile() {
        return file;
    }

    public boolean equals(Object o) {
        if (o instanceof FileWrapper) {
            return ((FileWrapper) o).file.equals(this.file);
        } else return false;
    }
}
