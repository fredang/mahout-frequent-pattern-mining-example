package com.chimpler.example.fpm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.Text;
import org.apache.mahout.common.Pair;
import org.apache.mahout.fpm.pfpgrowth.convertors.string.TopKStringPatterns;

public class ResultReader {
	public static Map<Integer, Long> readFrequency(Configuration configuration, String fileName) throws Exception {
		FileSystem fs = FileSystem.get(configuration);
		Reader frequencyReader = new SequenceFile.Reader(fs, 
				new Path(fileName), configuration);
		Map<Integer, Long> frequency = new HashMap<Integer, Long>();
		Text key = new Text();
		LongWritable value = new LongWritable();
		while(frequencyReader.next(key, value)) {
			frequency.put(Integer.parseInt(key.toString()), value.get());
		}
		return frequency;
	}
	
	public static Map<Integer, String> readMapping(String fileName) throws Exception {
		Map<Integer, String> itemById = new HashMap<Integer, String>();
		BufferedReader csvReader = new BufferedReader(new FileReader(fileName));
		while(true) {
			String line = csvReader.readLine();
			if (line == null) {
				break;
			}
			
			String[] tokens = line.split(",", 2);
			itemById.put(Integer.parseInt(tokens[1]), tokens[0]);
		}
		return itemById;
	}
	
	public static void readFrequentPatterns(
			Configuration configuration,
			String fileName,
			int transactionCount,
			Map<Integer, Long> frequency,
			Map<Integer, String> itemById,
			double minSupport, double minConfidence) throws Exception {
		FileSystem fs = FileSystem.get(configuration);

		Reader frequentPatternsReader = new SequenceFile.Reader(fs, 
				new Path(fileName), configuration);
		Text key = new Text();
		TopKStringPatterns value = new TopKStringPatterns();

		while(frequentPatternsReader.next(key, value)) {
			long firstFrequencyItem = -1;
			String firstItemId = null;
			List<Pair<List<String>, Long>> patterns = value.getPatterns();
			int i = 0;
			for(Pair<List<String>, Long> pair: patterns) {
				List<String> itemList = pair.getFirst();
				Long occurrence = pair.getSecond();
				if (i == 0) {
					firstFrequencyItem = occurrence;
					firstItemId = itemList.get(0);
				} else {
					double support = (double)occurrence / transactionCount;
					double confidence = (double)occurrence / firstFrequencyItem;
					if (support > minSupport
							&& confidence > minConfidence) {
						List<String> listWithoutFirstItem = new ArrayList<String>();
						for(String itemId: itemList) {
							if (!itemId.equals(firstItemId)) {
								listWithoutFirstItem.add(itemById.get(Integer.parseInt(itemId)));
							}
						}
						String firstItem = itemById.get(Integer.parseInt(firstItemId));
						listWithoutFirstItem.remove(firstItemId);
						System.out.printf(
							"%s => %s: supp=%.3f, conf=%.3f",
							listWithoutFirstItem,
							firstItem,
							support,
							confidence);

						if (itemList.size() == 2) {
							// we can easily compute the lift and the conviction for set of
							// size 2, so do it
							int otherItemId = -1;
							for(String itemId: itemList) {
								if (!itemId.equals(firstItemId)) {
									otherItemId = Integer.parseInt(itemId);
									break;
								}
							}
							long otherItemOccurrence = frequency.get(otherItemId);
							double lift = (double)occurrence / (firstFrequencyItem * otherItemOccurrence);
							double conviction = (1.0 - (double)otherItemOccurrence / transactionCount) / (1.0 - confidence);
							System.out.printf(
								", lift=%.3f, conviction=%.3f",
								lift, conviction);
						}
						System.out.printf("\n");
					}
				}
				i++;
			}
		}
		frequentPatternsReader.close();
		
	}
	
	public static void main(String args[]) throws Exception {
		if (args.length != 6) {
			System.err.println("Arguments: [transaction count] [mapping.csv path] [fList path] "
					+ "[frequentPatterns path] [minSupport] [minConfidence]");
			return;
		}

		int transactionCount = Integer.parseInt(args[0]);
		String mappingCsvFilename = args[1];
		String frequencyFilename = args[2];
		String frequentPatternsFilename = args[3];
		double minSupport = Double.parseDouble(args[4]);
		double minConfidence = Double.parseDouble(args[5]);

		Map<Integer, String> itemById = readMapping(mappingCsvFilename);

		Configuration configuration = new Configuration();
		Map<Integer, Long> frequency = readFrequency(configuration, frequencyFilename);
		readFrequentPatterns(configuration, frequentPatternsFilename, 
				transactionCount, frequency, itemById, minSupport, minConfidence);
		
	}
}
