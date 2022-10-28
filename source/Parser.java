import java.io.*;
import java.util.ArrayList;

public class Parser {
    // Debug vars
    static boolean silent = false;
    static boolean extraDebug = false;

    // Files
    static File binaryFile;
    static File hexFile;
    static File out;

    // Table related
    static int offset = 0;
    static String header = null;
    static String filename = null;

    // String builders
    static StringBuilder result = new StringBuilder();

    // Arrays
    static ArrayList<LCM_setting_table> table = new ArrayList<>();
    static ArrayList<Integer> headerIndexesMatches = new ArrayList<>();

    // Struct like. This simulates LCM_setting_table struct of lcms
    static class LCM_setting_table {
        String cmd;
        String count;
        String[] params_list;

        public LCM_setting_table(String cmd, String count, String[] params_list) {
            this.cmd = cmd;
            this.count = count;
            this.params_list = params_list;
        }

        public String format_params() {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < params_list.length; i++) {
                if(!params_list[i].equals(""))
                    if(i == params_list.length-1) {
                        out.append("0x").append(params_list[i]);
                    } else {
                        out.append("0x").append(params_list[i]).append(", ");
                    }
            }
            return out.toString();
        }

        @Override
        public String toString() {
            if(cmd.equals("REGFLAG_END_OF_TABLE") || cmd.equals("REGFLAG_DELAY")) {
                return "{" + cmd + ", " + Integer.parseInt(count,16) + ", {" + format_params() + "}}";
            } else {
                return "{0x" + cmd + ", " + Integer.parseInt(count,16) + ", {" + format_params() + "}}";
            }
        }
    }

    public static void argParser(String[] args) {
        System.out.println("[INFO] LCM TABLES Parser tool (v0.2) by Ruben1863");
        if(args == null || args.length == 0) {
            System.out.println("[ERROR] No arguments provided!\n" + "Exiting");
            System.exit(1);
        }

        for(int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    System.out.println("# ------------------------------------------------- #");
                    System.out.println("#                                                   #");
                    System.out.println("#  Help menu                                        #");
                    System.out.println("#                                                   #");
                    System.out.println("#  List of available arguments:                     #");
                    System.out.println("#                                                   #");
                    System.out.println("#      Required arguments:                          #");
                    System.out.println("#      -i, --input: Specify input header            #");
                    System.out.println("#      -f, --file: Specify input file               #");
                    System.out.println("#                                                   #");
                    System.out.println("#      Optional arguments:                          #");
                    System.out.println("#      -s, --silent: Suppresses text output         #");
                    System.out.println("#      -h, --help: Prints this menu                 #");
                    System.out.println("#                                                   #");
                    System.out.println("#  Example of correct execution:                    #");
                    System.out.println("#  java -jar parser.jar -i FF0000000101 -f kernel   #");
                    System.out.println("#                                                   #");
                    System.out.println("# ------------------------------------------------- #");
                    System.exit(0);
                case "-s":
                case "--silent":
                    silent = true;
                case "-i":
                case "--input":
                    if(i+1 < args.length)
                        header = args[i+1];
                    else
                        break;
                case "-f":
                case "--file":
                    if(i+1 < args.length)
                        filename = args[i+1];
                    else
                        break;
                default:
                    break;
            }
        }

        if(header == null || filename == null) {
            System.out.println("[ERROR] Required arguments aren't provided! Use -h or --help and try again\n" + "Exiting");
            System.exit(1);
        }
    }

    public static void debug(String str) {
        if(!silent)
            if(extraDebug)
                System.out.println(str);
            else
                if(!str.contains("[DEBUG]"))
                    System.out.println(str);
    }

    public static String arrayToString(ArrayList<LCM_setting_table> a) {
        if (a == null)
            return "null";

        int iMax = a.size() - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append(a.get(i));
            if (i == iMax)
                return b.toString();
            b.append(",\n");
        }
    }

    public static void convertFileToHex() {
        try {
            binaryFile = new File("input/" + filename);
            debug("[INFO] Binary size is " + binaryFile.length() + " bytes");
        } catch (NullPointerException e) {
            System.out.println("[ERROR] toHexFile: There was an error opening " + binaryFile.getName() + "!\n" + "Exiting");
            System.exit(1);
        }
        debug("[INFO] Converting " + binaryFile.getName() + " to hex, wait some seconds");
        try {
            File outDirectory = new File("output");
            if (!outDirectory.exists()) {
                if (outDirectory.mkdirs()) {
                    debug("[INFO] toHexFile: created output directory");
                } else {
                    System.out.println("[ERROR] toHexFile: error while creating output directory\n" + "Exiting");
                    System.exit(1);
                }
            } else {
                File[] files = outDirectory.listFiles();
                if (files != null) {
                    for(File f : files) {
                        debug("[DEBUG] toHexFile: deleted " + f.getName() + " -> " + f.delete());
                    }
                }
            }
            hexFile = new File("output/hex");
            if (!hexFile.createNewFile()) {
                debug("[INFO] toHexFile: 'hex' file already exists, deleting it before starting!");
                boolean b = hexFile.delete();
                boolean b2 = hexFile.createNewFile();
                debug("[DEBUG] toHexFile: delete " + b);
                debug("[DEBUG] toHexFile: create new file " + b2);
            }
        } catch (NullPointerException | IOException e) {
            System.out.println("[ERROR] toHexFile: There was an error creating 'hex' file!\n" + "Exiting");
            System.exit(1);
        }

        result = new StringBuilder();
        String hex;

        // Convert file to hex and write to hex file
        int value;
        try {
            FileInputStream fis = new FileInputStream(binaryFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            while ((value = bis.read()) != -1) {
                hex = Integer.toHexString(value);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                result.append(hex);
                result.append(" ");
            }
            bis.close();
            FileWriter fileWriter = new FileWriter(hexFile, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.append(result.toString().toUpperCase());
            result = new StringBuilder(result.toString().replaceAll(" ", "").toUpperCase());
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("[ERROR] toHexFile: There was an error converting " + binaryFile.getName() + " to hex!\n" + "Exiting");
            System.exit(1);
        }
    }

    public static void parse() {
        header = header.toUpperCase();
        if(header.contains("000000")) { // For now, I have only seen tables with this offset, or no offset
            offset = 6;
        }

        debug("[INFO] Table/s offset is: " + offset/2); // Because we count 0s instead of 00s
        debug("[INFO] Searching for headers");
        for (int index = result.indexOf(header); index >= 0;
             index = result.indexOf(header, index + 1)) { // Search for header indexes
            headerIndexesMatches.add(index);
        }

        if(headerIndexesMatches.size() != 0){
            debug("[INFO] Total headers found: " + headerIndexesMatches.size());
        } else {
            System.out.println("No headers found\n" + "Exiting");
            System.exit(1);
        }

        debug("[INFO] Starting to parse table/s!");
        for (int k = 0; k < headerIndexesMatches.size(); k++){
            table = new ArrayList<>();
            try {
                out = new File("output/out" + k + ".c");
                debug("[DEBUG] parse: create new file " + out.getName() + " -> " + out.createNewFile());

                OutputStream outputStream = new PrintStream(out);
                PrintStream tableOut = new PrintStream(outputStream);

                // Because there are fake header indexes, we need all bytes and then check them for finding "end of table"
                // Then Split the string into array of 2 chars
                String[] arr = result.substring(headerIndexesMatches.get(k)).split("(?<=\\G.{" + 2 + "})");
                String endOfTableCmd = "";
                boolean isEndOfTable = false;
                for(int i = 0; i < arr.length; i++) {
                    String cmd = arr[i], count = arr[i+(offset/2)+1];
                    if(isEndOfTable) {
                        String[] params = new String[Integer.parseInt(count,16)];
                        for(int j = 0; j < Integer.parseInt(count,16); j++) {
                            params[j] = arr[i + (offset/2) + 2 + j];
                        }
                        table.add(new LCM_setting_table(cmd, count, params));
                        table.add(new LCM_setting_table("REGFLAG_END_OF_TABLE", "00", new String[]{}));
                        endOfTableCmd = arr[i+72]; // Jump 72 bytes
                        break;
                    } else {
                        if (count.equals("00")) {
                            if (cmd.equals("29")) {
                                isEndOfTable = true;
                            }
                            table.add(new LCM_setting_table(cmd, count, new String[]{""}));
                            i += 71; // Jump 71 bytes
                        } else if (cmd.equals("29") && count.equals("01") && arr[i + (offset / 2) + 2].equals("00")) {
                            isEndOfTable = true;
                            table.add(new LCM_setting_table(cmd, count, new String[]{"00"}));
                            i += 71; // Jump 71 bytes
                        } else {
                            String[] params = new String[Integer.parseInt(count,16)];
                            for (int j = 0; j < Integer.parseInt(count,16); j++) {
                                params[j] = arr[i + (offset / 2) + 2 + j];
                            }
                            table.add(new LCM_setting_table(cmd, count, params));
                            i += 71; // Jump 71 bytes
                        }
                    }
                }

                // check for DELAYS? May be buggy
                LCM_setting_table lastDataBeforeEnd = table.get(table.size()-2);
                String delay = "";
                if(!lastDataBeforeEnd.cmd.equals("0x29")) {
                    delay = lastDataBeforeEnd.cmd;
                }

                for(int j = 0; j < table.size(); j++) {
                    if(table.get(j).cmd.equals(delay)) {
                        LCM_setting_table newLine = table.get(j);
                        newLine.cmd = "REGFLAG_DELAY";
                        newLine.params_list = new String[]{""};
                        table.set(j, newLine);
                    }
                }

                // Write table to output files
                tableOut.println("//REGFLAG_DELAY = 0x" + delay);
                tableOut.println("//REGFLAG_END_OF_TABLE = 0x" + endOfTableCmd + "\n");
                tableOut.print(arrayToString(table));
            } catch (NullPointerException | IOException e) {
                System.out.println("[ERROR] parse: Error while creating "  + out.getName() + " file!\n" + "Exiting");
                System.exit(1);
            }
        }
        debug("[INFO] parse: Successfully parsed all available headers! Check output directory\n" + "Exiting :)");
    }

    public static void main(String[] args) {
        argParser(args);
        convertFileToHex();
        parse();
    }
}