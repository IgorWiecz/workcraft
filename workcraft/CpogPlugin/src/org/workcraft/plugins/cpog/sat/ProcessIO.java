package org.workcraft.plugins.cpog.sat;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ProcessIO {

    public static String runViaStreams(String[] process, String input) {
        Process limboole;
        try {
            File inputFile = File.createTempFile("stream-", ".in");
            inputFile.deleteOnExit();
            File outputFile = File.createTempFile("stream-", ".out");
            outputFile.deleteOnExit();

            writeFile(input, inputFile);

            limboole = Runtime.getRuntime().exec(process);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(limboole.getOutputStream(), StandardCharsets.UTF_8));
            writer.write(input);
            writer.close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(limboole.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                result.append(line).append('\n');
            }

            String output = result.toString();
            writeFile(output, outputFile);
            return output;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String runViaStreams(String command, String input) {
        String result;
        try {
            File inputFile = File.createTempFile("stream-", ".in");
            inputFile.deleteOnExit();
            File outputFile = File.createTempFile("stream-", ".out");
            outputFile.deleteOnExit();

            writeFile(input, inputFile);

            Process process = Runtime.getRuntime().exec(new String[]{command, inputFile.getAbsolutePath(), outputFile.getAbsolutePath()});
            process.getOutputStream().close();
            while (true) {
                int r = process.getInputStream().read();
                if (r == -1) {
                    break;
                }
            }
            process.getInputStream().close();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            result = readFile(outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        reader.close();
        return sb.toString();
    }

    private static void writeFile(String text, File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
        writer.write(text);
        writer.close();
    }

}

