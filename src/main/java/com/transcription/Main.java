package com.transcription;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        File file = new File("src/main/resources/Faster-Whisper-XXL/faster-whisper-xxl.exe");
        List<String> CommandInput = new ArrayList<>();
        String Whisper = file.getAbsolutePath();
        CommandInput.add(Whisper);
        System.out.println("Input folder:");
        String MainFolder = scanner.nextLine();
        System.out.println("Input file name:");
        CommandInput.add(MainFolder+scanner.nextLine());
        System.out.println("Input Language:");
        CommandInput.add("--language " + scanner.nextLine());
        System.out.println("Input Model:");
        CommandInput.add("--model " + scanner.nextLine());
        String temp="";
        for(String ln :CommandInput){
            temp=temp.concat(ln+" ");
        }
        temp = temp.concat("--output_dir source");
        try {
            // Command to execute (e.g., list files in current directory)
            String command = "cmd.exe /c " + temp;
            Process process = Runtime.getRuntime().exec(command);
            // Read the output of the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            // Check for any errors
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }
            // Wait for the process to complete and get the exit code
            int exitCode = process.waitFor();
            System.out.println("Command exited with code: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Would you like to Translate to another language? y/n");
        String Translate = scanner.nextLine();
        if(Translate.equalsIgnoreCase("y")){
            System.out.println("Input the Language");
            String Language = scanner.nextLine();
            File folder = new File(MainFolder.substring(0,MainFolder.length()-1));
            // Get all .srt files
            File[] srtFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".srt"));
            if (srtFiles != null) {
                //if there are srt files translate them one by one
                for (File srt : srtFiles) {
                    String SrtTemp = srt.toString();
                    OllamaTranslator.translateSubtitleFile(SrtTemp,
                            SrtTemp.substring(0,SrtTemp.length()-4).concat("_translated.srt"),
                            Language);
                }
            }
        }
    }
}
