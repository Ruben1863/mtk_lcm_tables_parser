import org.apache.commons.cli.*;
import java.io.*;
import java.util.ArrayList;

public class Parser {
    static StringBuilder result = new StringBuilder();

    static class LCM_setting_table {
        String cmd;
        String count;
        String[] params_list;

        public LCM_setting_table(String cmd, String count, String[] params_list) {
            this.cmd = cmd;
            this.count = count;
            this.params_list = params_list;
        }

        public String formatParams() {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < params_list.length; i++) {
                if (!params_list[i].isEmpty()) {
                    out.append("0x").append(params_list[i]);
                    if (i < params_list.length - 1) {
                        out.append(", ");
                    }
                }
            }
            return out.toString();
        }

        @Override
        public String toString() {
            String prefix = (cmd.equals("REGFLAG_END_OF_TABLE") || cmd.equals("REGFLAG_DELAY")) ? "" : "0x";
            return "{" + prefix + cmd + ", " + Integer.parseInt(count, 16) + ", {" + formatParams() + "}}";
        }
    }

    public static String arrayToString(ArrayList<LCM_setting_table> a) {
        if (a == null || a.isEmpty()) {
            return "";
        }

        StringBuilder b = new StringBuilder();
        for (int i = 0; i < a.size(); i++) {
            b.append(a.get(i));
            if (i < a.size() - 1) {
                b.append(",\n");
            }
        }
        return b.toString();
    }

    private static void messageHandler(int i, String message) {
        switch (i) {
            case 1:
                System.out.printf("[DEBUG] %s\n", message);
                break;
            case 2:
                System.out.printf("[INFO] %s\n", message);
                break;
            case 3:
                System.out.printf("[ERROR] %s\nExiting!\n", message);
                System.exit(1);
                break;
            default:
                break;
        }
    }

    public static void convertFileToHex(String filename) {
        messageHandler(2, "LCM TABLES Parser tool (v0.2.5) by Ruben1863");
        try {
            File binaryFile = new File("input/" + filename);
            messageHandler(2, String.format("Binary size is %s bytes\n       Converting file '%s' to hex. Wait some seconds", binaryFile.length(), filename));

            result = new StringBuilder();
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(binaryFile))) {
                int value;
                while ((value = bis.read()) != -1) {
                    String hex = Integer.toHexString(value);
                    if (hex.length() == 1) {
                        hex = "0" + hex;
                    }
                    result.append(hex).append(" ");
                }
                // Format result
                result = new StringBuilder(result.toString().toUpperCase().replaceAll(" ", ""));
            } catch (IOException e) {
                messageHandler(3, String.format("toHex: There was an error converting %s to hex!", binaryFile.getName()));
            }
        } catch (NullPointerException e) {
            messageHandler(3, String.format("toHex: There was an error opening %s!", filename));
        }
    }

    private static int calculateOffset(String header) {
        int offset = 0;
        if (header.contains("000000")) {
            offset = 6;
        }
        return offset;
    }

    private static void printOffsetMessage(int offset) {
        messageHandler(2, String.format("Table/s offset is: %s", offset / 2));
    }

    private static ArrayList<Integer> findHeaderOccurrences(String header) {
        ArrayList<Integer> headerIndexesMatches = new ArrayList<>();
        for (int index = result.indexOf(header); index >= 0; index = result.indexOf(header, index + 1)) {
            headerIndexesMatches.add(index);
        }
        return headerIndexesMatches;
    }

    private static void printTotalHeadersMessage(int totalHeaders) {
        messageHandler(2, String.format("Total headers found: %s", totalHeaders));
    }

    private static void prepareOutputDirectory() {
        File outDirectory = new File("output");
        if (!outDirectory.exists()) {
            if (outDirectory.mkdirs()) {
                messageHandler(2, "Parse: created output directory");
            } else {
                messageHandler(3, "Parse: error while creating output directory");
            }
        } else {
            File[] files = outDirectory.listFiles();
            if (files != null) {
                messageHandler(2, "Parse: deleting files of output folder");
                for (File f : files) {
                    f.delete();
                }
            }
        }
    }

    private static void processTable(int headerIndex, int offset, int k) {
        ArrayList<LCM_setting_table> table = new ArrayList<>();
        try {
            File out = new File("output/out" + k + ".c");

            // Because there are fake header indexes, we need all bytes and then check them for finding "end of table"
            // Then we split the string into an array of 2 chars
            PrintStream tableOut = new PrintStream(new FileOutputStream(out));
            String[] arr = result.substring(headerIndex).split("(?<=\\G.{" + 2 + "})");
            String endOfTableCmd = "";
            boolean isEndOfTable = false;
            for (int i = 0; i < arr.length; i++) {
                String cmd = arr[i], count = arr[i + (offset / 2) + 1];
                if (isEndOfTable) {
                    if (!count.equals("00")) {
                        String[] params = new String[Integer.parseInt(count, 16)];
                        for (int j = 0; j < Integer.parseInt(count, 16); j++) {
                            params[j] = arr[i + (offset / 2) + 2 + j];
                        }
                        // End of table
                        if (arr[i + 73].equals("00")) {
                            endOfTableCmd = arr[i + 72];
                        } else {
                            if (arr[i + 73].contains("0")) {
                                endOfTableCmd = arr[i + 72] + arr[i + 73].replace("0", "");
                            } else {
                                endOfTableCmd = arr[i + 72] + arr[i + 73];
                            }
                            table.add(new LCM_setting_table(cmd, count, params));
                            table.add(new LCM_setting_table("REGFLAG_END_OF_TABLE", "00", new String[]{}));
                        }
                    } else {
                        // End of table
                        if (arr[i + 1].equals("00")) {
                            endOfTableCmd = arr[i];
                        } else {
                            if (arr[i + 1].contains("0")) {
                                endOfTableCmd = arr[i] + arr[i + 1].replace("0", "");
                            } else {
                                endOfTableCmd = arr[i] + arr[i + 1];
                            }
                            table.add(new LCM_setting_table("REGFLAG_END_OF_TABLE", "00", new String[]{}));
                        }
                    }
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
                        String[] params = new String[Integer.parseInt(count, 16)];
                        for (int j = 0; j < Integer.parseInt(count, 16); j++) {
                            params[j] = arr[i + (offset / 2) + 2 + j];
                        }
                        table.add(new LCM_setting_table(cmd, count, params));
                        i += 71; // Jump 71 bytes
                    }
                }
            }

            // Check for DELAYS. May be buggy
            LCM_setting_table data1 = table.get(table.size() - 2);
            LCM_setting_table data2 = table.get(table.size() - 3);
            LCM_setting_table data3 = table.get(table.size() - 4);

            String delay = "";
            if (data1.cmd.equals(data3.cmd)) {
                delay = data1.cmd;
            } else if (data1.cmd.equals("29")) {
                delay = data2.cmd;
            }

            if (!delay.equals("")) {
                for (int j = 0; j < table.size(); j++) {
                    if (table.get(j).cmd.equals(delay)) {
                        LCM_setting_table newLine = table.get(j);
                        newLine.cmd = "REGFLAG_DELAY";
                        newLine.params_list = new String[]{""};
                        table.set(j, newLine);
                    }
                }
                // Write delay to output files
                tableOut.printf("//REGFLAG_DELAY = 0x%s\n", delay);
            }
            // Write table to output files
            tableOut.printf("//REGFLAG_END_OF_TABLE = 0x%s\n\n", endOfTableCmd);
            tableOut.print(arrayToString(table));
        } catch (NullPointerException | IOException e) {
            messageHandler(3, String.format("Parse: Error while creating out %s.c file!", k));
        }
    }

    public static void parse(String header) {
        header = header.toUpperCase();
        int offset = calculateOffset(header);
        printOffsetMessage(offset);
        ArrayList<Integer> headerIndexesMatches = findHeaderOccurrences(header);

        if (!headerIndexesMatches.isEmpty()) {
            printTotalHeadersMessage(headerIndexesMatches.size());
        } else {
            messageHandler(3, "No headers found");
        }

        prepareOutputDirectory();

        for (int k = 0; k < headerIndexesMatches.size(); k++) {
            processTable(headerIndexesMatches.get(k), offset, k);
        }

        messageHandler(2, "Parse: Successfully parsed all available headers! Check output directory\nExiting :)");
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option header = new Option("i", "header", true, "input header");
        header.setRequired(false);
        options.addOption(header);

        Option filename = new Option("f", "filename", true, "input file name");
        filename.setRequired(false);
        options.addOption(filename);

        Option help = new Option("h", "help", false, "prints this menu");
        help.setRequired(false);
        options.addOption(help);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        String helpMessage = "usage: java -jar Parser.jar\n" +
                " -f,--filename <arg>   input file name\n" +
                " -h,--help             prints this menu\n" +
                " -i,--header <arg>     input header";

        String headerStr = "", filenameStr = "";

        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                messageHandler(2, String.format("LCM TABLES Parser tool (v0.2.5) by Ruben1863\n%s\n", helpMessage));
                System.exit(0);
            }

            if (cmd.hasOption("header") && cmd.hasOption("filename")) {
                headerStr = cmd.getOptionValue("header");
                filenameStr = cmd.getOptionValue("filename");
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
            messageHandler(3, String.format("%s\n%s", helpMessage, e.getMessage()));
        }

        convertFileToHex(filenameStr);
        parse(headerStr);
    }
}
