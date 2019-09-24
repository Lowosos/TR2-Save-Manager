package lowosos;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

class PropertiesUtil {

    private Properties p = new Properties();
    private boolean closeAfterCleanRun;
    private String exe;

    PropertiesUtil(String filename) throws IOException, URISyntaxException {
        loadProperties(filename);
        checkIntegrity(filename);
    }

    private void loadProperties(String filename) throws IOException, URISyntaxException {
        File file = new File(filename);
        if (!file.exists()) {
            Util.alert("Properties file not found, it will be created and default values will be used.");
            copyDefConf(filename);
        }
        InputStream input = new FileInputStream(filename);
        p.load(input);
        input.close();
    }

    private void copyDefConf(String filename) throws IOException, URISyntaxException {
        InputStream in = getClass().getResourceAsStream("resources/default-config.properties");
        Files.copy(in,
                new File(filename).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private void checkIntegrity(String filename) throws IOException, URISyntaxException {
        if (!allProps()) {
            if (useDefaultConfigPrompt()) {
                boolean success = new File(filename).delete();
                if (success) {
                    copyDefConf(filename);
                    loadProperties(filename);
                }
                else {
                    Util.alert("The config file couldn't be deleted. This is likely a programmer's fault.");
                    System.exit(1);
                }
            } else System.exit(-1);
        }
        setPropVals(filename);
    }

    private boolean allProps() {
        return p.keySet().containsAll(Arrays.asList("exe", "closeAfterCleanRun"));
    }

    private void setPropVals(String filename) {
        if (!(setClose() && setExe(new File(filename).getParentFile().getAbsolutePath()))) {
            System.exit(1);
        }
    }

    private boolean setClose() {
        String s = p.getProperty("closeAfterCleanRun");
        if (!(s.equals("true") || s.equals("false"))) {
            Util.alert("Invalid property value - property = closeAfterCleanRun, value = \"" + s + "\"");
            return false;
        }
        closeAfterCleanRun = Boolean.parseBoolean(s);
        return true;
    }

    private boolean setExe(String gameDir) {
        String base = p.getProperty("exe");
        if (base.equals("")) {
            exe = "";
            return true;
        }
        String exe = base.endsWith(".exe") ? base : base + ".exe";
        if (!new File(gameDir + "\\" + exe).isFile()) {
            Util.alert("The exe file doesn't exist.");
            return false;
        }
        this.exe = exe;
        return true;
    }

    private boolean useDefaultConfigPrompt() {
        ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.WARNING, "Config file corrupted. Do you want to replace it with the default one?\n" +
                "Click yes to replace, cancel to quit.",
                yes, cancel);
        alert.setTitle("Config corrupted");
        Optional<ButtonType> result = alert.showAndWait();
        return result.orElse(cancel) == yes;
    }

    public boolean isCloseAfterCleanRun() {
        return closeAfterCleanRun;
    }

    public String getExe() {
        if (exe.equals("") || new File(exe).isFile()) {
            return exe;
        }
        Util.alert("The exe file has been removed.");
        System.exit(1);
        return "";
    }
}