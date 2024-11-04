package comp512st.tests;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class aggregateResultsTool {
    public static void main(String[] args){
        aggregateAllFolders();
    }
    private static double aggregateOneFile(String fname){
        int lineCount = 0;
        int total = 0;
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(fname)));
            //System.out.println("File contents:");
            //System.out.println(content);
        } catch (Exception e){}

        for (String line : content.split("\n")){
            String num = line.split(": ")[1];
            Integer ms = Integer.valueOf(num.substring(0, num.length()-3));
            total += ms;
            lineCount++;
        }

        return (double) total / lineCount;
    }

    private static String aggregateOneFolder(String folderName, String lineName){
        File directory = new File(folderName);
        String[] folderNames = directory.list((file, name) -> new File(file, name).canRead());

        StringBuilder sb = new StringBuilder();
        sb.append(lineName).append(": ");

        for (String s : folderNames){
            if (s.equals("game.log")) continue;
            String player = s.split("-")[2];
            player = player.substring(0, player.length() - 4);
            sb.append(player).append(": ");
            sb.append(aggregateOneFile(folderName + "/" + s));
            sb.append("\t");
        }

        sb.append("\n");

        return sb.toString();
    }

    private static void aggregateAllFolders(){
        //System.out.println(System.getProperty("user.dir"));
        String path = System.getProperty("user.dir") + "/comp512st/tests/results";
        File directory = new File(path);
        String[] folderNames = directory.list((file, name) -> new File(file, name).isDirectory());
        System.out.println(Arrays.toString(folderNames));

        StringBuilder sb = new StringBuilder();

        for (String s : folderNames) sb.append(aggregateOneFolder(path + "/" + s, s));

        System.out.println(sb);
    }
}
