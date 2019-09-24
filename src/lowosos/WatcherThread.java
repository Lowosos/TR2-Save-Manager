package lowosos;


import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

class WatcherThread extends Thread {

    private Path pathToDir;
    private ObservableList<FileWrapper> wList;
    private ObservableList<FileWrapper> sfList;
    private WatchService watchService;

    WatcherThread(String dirPath, ObservableList<FileWrapper> wList, ObservableList<FileWrapper> sfList) {
        this.pathToDir = Paths.get(dirPath);
        this.wList = wList;
        this.sfList = sfList;
    }

    @Override
    public void run() {
        watchService = null;
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            pathToDir.register(watchService, ENTRY_CREATE, ENTRY_DELETE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (FileWrapper fw : wList) {
            try {
                fw.register(watchService);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        startListening();
    }

    private void startListening() {
        WatchKey key;
        try {
            while (true) {
                if ((key = watchService.take()) != null) {
                    final Path dir = (Path) key.watchable();
                    key.pollEvents().forEach(event -> {
                        //System.out.println("event " + event.kind() + " on file " + event.context());
                        Path path = dir.resolve(event.context().toString());
                        Path relative = pathToDir.relativize(path);
                        int length = relative.toString().split("\\\\").length;
                        if (length == 1) { // change in root dir
                            FileWrapper folder = new FileWrapper(path.toFile());
                            if ((event.kind() == ENTRY_CREATE)&& folder.getFile().isDirectory()) { // if a folder was created
                                try {
                                    folder.register(watchService);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                if (Util.isSaveFolder(path.toFile())) { // if a save folder was created
                                    sfList.add(folder);
                                }
                                wList.add(folder);
                            } else if (event.kind() == ENTRY_DELETE) {// if a folder was deleted
                                sfList.remove(folder);
                                int wIndex = wList.indexOf(folder);
                                wList.get(wIndex).cancelWatchKey();
                                wList.remove(wIndex);
                            }
                        }
                        else if (length == 2) { // change in direct subdir of root, changed is a file
                            File f = path.toFile();
                            if (f.getName().equals("description.txt")) { // if a description was changed
                                FileWrapper fw = wList.get(wList.indexOf(new FileWrapper(new File(f.getParent()))));
                                if ((event.kind() == ENTRY_CREATE || event.kind() == ENTRY_MODIFY) && f.isFile()) { // if a description was created
                                    fw.descriptionProperty().set(Util.loadDescriptionIn(f.getParent()));
                                }
                                else if (event.kind() == ENTRY_DELETE) { // if a description was deleted
                                    fw.descriptionProperty().set("");
                                }
                            }
                            else if (Util.isSave(f)) {
                                FileWrapper parent = new FileWrapper(new File(f.getParent()));
                                if ((event.kind() == ENTRY_CREATE) && f.isFile()) { // if a save file has been added
                                    FileWrapper folder = wList.get(wList.indexOf(parent));
                                    folder.levelsAndDescInit();
                                    if (!sfList.contains(parent)) { // if the save folder is not in the table yet
                                        sfList.add(folder);
                                    }
                                }
                                else if (event.kind() == ENTRY_DELETE) { // if a save file has been deleted
                                    if (!Util.isSaveFolder(parent.getFile()) || !Util.getSaveLevelsInFolder(parent.getFile()).isEmpty()) {
                                        sfList.remove(parent);
                                    }
                                }
                                wList.get(wList.indexOf(parent)).levelsProperty().set(Util.getSaveLevelsInFolder(parent.getFile()));
                            }
                        }
                    });
                    GUI.refreshTable();
                    key.reset();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
