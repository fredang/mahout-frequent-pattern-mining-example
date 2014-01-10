package com.chimpler.example.fpm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class ConvertCsvToDat {
	public static void main(String args[]) throws Exception {
		if (args.length != 1) {
			System.err.println("Arguments: [marketbasket.csv path]");
			return;
		}
		String csvFilename = args[0];

		BufferedReader csvReader = new BufferedReader(new FileReader(csvFilename));

		// with the first line we can get the mapping id, text
		String line = csvReader.readLine();
		String[] tokens = line.split(",");
		FileWriter mappingWriter = new FileWriter("mapping.csv");
		int i = 0;
		for(String token: tokens) {
			mappingWriter.write(token.trim() + "," + i + "\n");
			i++;
		}
		mappingWriter.close();
		
		FileWriter datWriter = new FileWriter("output.dat");
		int transactionCount = 0;
		while(true) {
			line = csvReader.readLine();
			if (line == null) {
				break;
			}
			
			tokens = line.split(",");
			i = 0;
			boolean isFirstElement = true;
			for(String token: tokens) {
				if (token.trim().equals("true")) {
					if (isFirstElement) {
						isFirstElement = false;
					} else {
						datWriter.append(",");
					}
					datWriter.append(Integer.toString(i));
				}
				i++;
			}
			datWriter.append("\n");
			transactionCount++;
		}
		datWriter.close();
		System.out.println("Wrote " + transactionCount + " transactions.");
	}
}
