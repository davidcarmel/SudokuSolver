package com.io.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Normalize the puzzles from Norvig 95puzzles file to a standard form
 * The file appears in: ./data/top95.text
 * @author david.carmel@gmail.com
 *
 */
public class PuzzleNormalization {
	
	public static void main(String[] args)  {
		
		File puzzles = new File(args[0]);
		File outDir = new File(args[1]);
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(puzzles));
			String line = null;
			int row = 1;
			while ((line = br.readLine()) != null) {
				File out = new File(outDir, "e" + row + ".txt");
				BufferedWriter bw = new BufferedWriter(new FileWriter(out));
				String newLine = "";
				for (int i = 0; i < line.length(); i++) {
					if (Character.isDigit(line.charAt(i)))						
						newLine = newLine.concat(line.charAt(i)+" ");
					else
						newLine = newLine.concat("0 ");
					if ((i+1) % 9 == 0)
						newLine = newLine.concat("\n");
				}
				System.out.println(newLine);
				bw.write(newLine);
				bw.close();		
				row++;
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
