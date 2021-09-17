package com.io.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.io.sudoku.Sudoku;

public class PuzzleColectionValidation {
	
	/**
	 * read all puzzle and validate that all are unique
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	static void validateAllPuzzlesUniqueness(int BlockSize, File f) throws NumberFormatException, IOException {
		boolean uniqueness = true;
		String[] ext = {"txt"};
		List<File> files = (List<File>) FileUtils.listFiles(f,ext,true);
		List<Sudoku> puzzles = new ArrayList<>();
		for (File file : files) {
			Sudoku s = new Sudoku(BlockSize, file);
			for (Sudoku p : puzzles) {
				if (s.isEqual(p)) {
					System.out.println(file.getName() + " already exist! "+ p.getFileName());
					uniqueness = false;
				}
			}
			puzzles.add(s);
		}
		if (uniqueness) System.out.println("All files are unique!");
	}

	
	public static void main(String[] args) {

		File f = new File("./data");
		try {
			validateAllPuzzlesUniqueness(3, f);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
