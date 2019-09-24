package lowosos;

import javafx.scene.control.Alert;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Util {

    public static boolean isSave(File f) {
        String[] name = f.getName().split("\\.");
        return name.length == 2 && name[0].equals("savegame") && isSaveNum(name[1]);
    }

    public static boolean isSaveFolder(File f) {
        File[] files = f.listFiles();
        if (files == null) return false;
        for (File file : files) {
            if (isSave(file) && file.isFile()) return true;
        }
        return false;
    }

    private static boolean isSaveNum(String s) {
        Integer n;
        try {
            n = Integer.parseInt(s);
        } catch (Exception e) {
            return false;
        }
        return n <= 18 && n >= 0;
    }

    public static int getSaveLevel(File f) {
        if (!isSave(f)) return 0;
        String line = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            line = reader.readLine();
            reader.close();
        } catch (FileNotFoundException e) {
            System.err.println("File not found, perhaps already deleted.");
            return -1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classifyLevel(line);
    }

    public static String getSaveLevelsInFolder(File folder) {
        ArrayList<Integer> list = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null) return "";
        for (File f : Arrays.stream(files).filter(Util::isSave).toArray(File[]::new)) {
            int level = getSaveLevel(f);
            if (level != -1 && !list.contains(level)) list.add(level);
        }
        Collections.sort(list);
        if (list.size() == 0) return "";
        String result = "";
        for (int i : list) result += i + ", ";
        return result.substring(0, result.length() - 2);
    }

    private static int classifyLevel(String s) {
        if (s.startsWith("The Great Wall")) return 1;
        if (s.startsWith("Venice")) return 2;
        if (s.startsWith("Bartoli's Hideout")) return 3;
        if (s.startsWith("Opera House")) return 4;
        if (s.startsWith("Offshore Rig")) return 5;
        if (s.startsWith("Diving Area")) return 6;
        if (s.startsWith("40 Fathoms")) return 7;
        if (s.startsWith("Wreck of the Maria Doria")) return 8;
        if (s.startsWith("Living Quarters")) return 9;
        if (s.startsWith("The Deck")) return 10;
        if (s.startsWith("Tibetan Foothills")) return 11;
        if (s.startsWith("Barkhang Monastery")) return 12;
        if (s.startsWith("Catacombs of the Talion")) return 13;
        if (s.startsWith("Ice Palace")) return 14;
        if (s.startsWith("Temple of Xian")) return 15;
        if (s.startsWith("Floating Islands")) return 16;
        if (s.startsWith("Dragon's Lair")) return 17;
        if (s.startsWith("Home Sweet Home")) return 18;
        else return 0;
    }

    public static void deleteSaves(String curDir) {
        Arrays.stream(new File(curDir).listFiles()).filter(f -> isSave(f)).forEach(f -> f.delete());
    }

    public static void copySaves(File folder, String curDir) {
        Arrays.stream(folder.listFiles()).filter(f -> isSave(f)).forEach(f -> {
            try {
                Files.copy(f.toPath(), new File(curDir + "\\" + f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void gameRunningAlert() {
        alert("Game is still running!");
    }

    public static void alert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static String loadDescriptionIn(String absolutePath) {
        String retval = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(absolutePath + "\\description.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                retval += line + " ";
            }
            reader.close();
        } catch (IOException e) {
        }
        return retval;
    }

}
