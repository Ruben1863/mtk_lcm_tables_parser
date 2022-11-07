import org.apache.commons.cli.*;

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
    static String header = "";
    static String filename = "";

    // String builders
    static StringBuilder result = new StringBuilder();

    // Arrays
    static ArrayList<LCM_setting_table> table = new ArrayList<>();
    static ArrayList<Integer> headerIndexesMatches = new ArrayList<>();

    // Struct like. This simulates LCM_setting_table struct of lcm
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
                return "{" + cmd + ", " + Integer.parseInt(count, 16) + ", {" + format_params() + "}}";
            } else {
                return "{0x" + cmd + ", " + Integer.parseInt(count, 16) + ", {" + format_params() + "}}";
            }
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
            return null;

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

        // Convert file to hex
        result = new StringBuilder();
        String hex;
        int value;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(binaryFile))) {
            while ((value = bis.read()) != -1) {
                hex = Integer.toHexString(value);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                result.append(hex);
                result.append(" ");
            }
            result = new StringBuilder(result.toString().toUpperCase());
        } catch (IOException e) {
            System.out.println("[ERROR] toHexFile: There was an error converting " + binaryFile.getName() + " to hex!\n" + "Exiting");
            System.exit(1);
        }

        // Write to hex file
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(hexFile, true))) {
            bufferedWriter.append(result);
        } catch (IOException e) {
            System.out.println("[ERROR] toHexFile: There was an error writing hex bytes!\n" + "Exiting");
            System.exit(1);
        }
        result = new StringBuilder(result.toString().replaceAll(" ", ""));
    }

    public static void parse() {
        header = header.toUpperCase();
        if(header.contains("000000")) { // For now, I have only seen tables with this offset, or no offset
            offset = 6;
        }

        debug("[INFO] Table/s offset is: " + offset/2); // Because we count 0s instead of 00s
        debug("[INFO] Searching for headers");
        for (int index = result.indexOf(header); index >= 0;
             index = result.indexOf(header, index + 1)) { // Search for header occurrences
            headerIndexesMatches.add(index);
        }

        if(headerIndexesMatches.size() != 0){
            debug("[INFO] Total headers found: " + headerIndexesMatches.size());
        } else {
            System.out.println("[ERROR] No headers found\n" + "Exiting");
            System.exit(1);
        }

        debug("[INFO] Starting to parse table/s!");
        for (int k = 0; k < headerIndexesMatches.size(); k++){
            table = new ArrayList<>();
            try {
                out = new File("output/out" + k + ".c");
                debug("[DEBUG] Parse: create new file " + out.getName() + " -> " + out.createNewFile());

                PrintStream tableOut = new PrintStream(new FileOutputStream(out));

                // Because there are fake header indexes, we need all bytes and then check them for finding "end of table"
                // Then we split the string into array of 2 chars
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
                        endOfTableCmd = arr[i+72]; // End of table = jump 72 bytes
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

                // check for DELAYS. May be buggy
                LCM_setting_table lastDataBeforeEnd = table.get(table.size()-2);
                String delay = "";
                if(!lastDataBeforeEnd.cmd.equals("0x29")) {
                    delay = lastDataBeforeEnd.cmd;
                }

                if(!delay.equals("")) {
                    for(int j = 0; j < table.size(); j++) {
                        if(table.get(j).cmd.equals(delay)) {
                            LCM_setting_table newLine = table.get(j);
                            newLine.cmd = "REGFLAG_DELAY";
                            newLine.params_list = new String[]{""};
                            table.set(j, newLine);
                        }
                    }
                    // Write delay to output files
                    tableOut.println("//REGFLAG_DELAY = 0x" + delay);
                }
                // Write table to output files
                tableOut.println("//REGFLAG_END_OF_TABLE = 0x" + endOfTableCmd + "\n");
                tableOut.print(arrayToString(table));
            } catch (NullPointerException | IOException e) {
                System.out.println("[ERROR] Parse: Error while creating "  + out.getName() + " file!\n" + "Exiting");
                System.exit(1);
            }
        }
        debug("[INFO] Parse: Successfully parsed all available headers! Check output directory\n" + "Exiting :)");
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option header = new Option("i", "header", true, "input header");
        header.setRequired(false);
        options.addOption(header);

        Option filename = new Option("f", "filename", true, "input file name");
        filename.setRequired(false);
        options.addOption(filename);

        Option silent = new Option("s", "silent", false, "suppresses text output");
        silent.setRequired(false);
        options.addOption(silent);

        Option help = new Option("h", "help", false, "prints this menu");
        help.setRequired(false);
        options.addOption(help);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if(cmd.hasOption("help")){
                System.out.println("[INFO] LCM TABLES Parser tool (v0.2) by Ruben1863");
                formatter.printHelp("java -jar Parser.jar", options);
                System.exit(0);
            } else if(cmd.hasOption("silent")) {
                Parser.silent = true;
            }

            if(cmd.hasOption("header") && cmd.hasOption("filename")) {
                Parser.header = cmd.getOptionValue("header");
                Parser.filename = cmd.getOptionValue("filename");
            } else {
                if(!cmd.hasOption("header") && cmd.hasOption("filename")) {
                    throw new ParseException("[ERROR] Missing required options: -i");
                } else if(!cmd.hasOption("filename") && cmd.hasOption("header")) {
                    throw new ParseException("[ERROR] Missing required options: -f");
                } else {
                    throw new ParseException("[ERROR] Missing required options: -i, -f");
                }
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar Parser.jar", options);
            System.exit(1);
        }

        convertFileToHex();
        parse();
    }
}