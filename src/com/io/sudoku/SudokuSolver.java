package com.io.sudoku;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.util.Pair;

import com.io.sudoku.Sudoku.UnitType;

/**
 * This class solve a Sudoku puzzle by searching over the domain space and by
 * enforcing constraints on the cell legal values
 * 
 * See known techniques for Sudoku solving:
 * https://www.kristanix.com/sudokuepic/sudoku-solving-techniques.php
 * 
 * Free online Sudoku puzzles to experiment with:
 * http://lipas.uwasa.fi/~timan/sudoku/
 * 
 * @author david.carmel@gmail.com
 *
 */

public class SudokuSolver {

	/**
	 * identify legal values for each non-assigned cell based on its mates in RCB -
	 * remove all values already assigned to other RCB cells
	 * 
	 * @param Sudoku puzzle
	 * @return false when assignment is illegal - no value can be assigned to one of
	 *         the cells, otherwise returns true
	 * @throws IOException
	 */
	static boolean candidateReduction(Sudoku sudoku) throws IOException {
		boolean nonStable = true;
		while (nonStable) {
			nonStable = false;
			List<Pair<Integer, Integer>> nonAssignedCells = sudoku.getNonAssignedCells();
			if (nonAssignedCells == null) {
				return false; // one of the non-assigned cells has no legal candidates left - no solution
			}
			if (nonAssignedCells.size() == 0)
				// no cells to assign - puzzle solved
				return true;

			for (Pair<Integer, Integer> cell : nonAssignedCells) {
				int i = cell.getKey();
				int j = cell.getValue();
				for (UnitType unit : UnitType.values()) {
					for (Pair<Integer, Integer> p : sudoku.getUnitCells(i, j, unit)) {
						// remove all (i,j) candidates already set in this unit
						if (sudoku.isSet(p.getKey(), p.getValue())) {
							sudoku.removeCellCandidate(i, j, sudoku.getCellValue(p.getKey(), p.getValue()));
						}
					}
				}
				if (sudoku.isEmpty(i, j)) {
					// if no candidates left for this cell, there is no solution for the puzzle
					return false;
				} else if (sudoku.getCellCandidates(i, j).size() == 1) {
					// sole candidate - nail it to this cell
					sudoku.setCellValue(i, j, sudoku.getCellCandidates(i, j).get(0));
					nonStable = true; // any setting requires a new pass
				}
			}
		}
		return true;
	}

	/**
	 * Unique Candidate: If a value can only be set in one cell within a unit, then
	 * it is guaranteed to fit there.
	 * 
	 * @param Sudoku puzzle
	 * @return false when assignment is illegal - no value can be assigned to one of
	 *         the entries, otherwise returns true
	 * @throws IOException
	 */
	static boolean uniqueCandidate(Sudoku sudoku) throws IOException {
		boolean nonStable = true;
		while (nonStable) {
			nonStable = false;
			List<Pair<Integer, Integer>> nonAssignedCells = sudoku.getNonAssignedCells();
			if (nonAssignedCells == null) {
				return false; // one of the non-assigned cells has no legal candidates left - no solution
			}
			if (nonAssignedCells.size() == 0)
				return true; // no cells to assign - puzzle solved

			for (Pair<Integer, Integer> cell : nonAssignedCells) {
				int i = cell.getKey();
				int j = cell.getValue();
				List<Integer> vals = sudoku.getCellCandidates(i, j);
				for (Integer val : vals) {
					// check value uniqueness in one of the (i,j) cell's units
					for (UnitType unit : UnitType.values()) {
						List<Pair<Integer, Integer>> unitCells = sudoku.getUnitCells(i, j, unit);
						boolean contained = false;
						for (Iterator<Pair<Integer, Integer>> it = unitCells.iterator(); it.hasNext() && !contained;) {
							Pair<Integer, Integer> up = it.next();
							if (sudoku.contains(up.getKey(), up.getValue(), val))
								contained = true;
						}
						if (!contained) { // val does not appear in other unit cells, nail it to (i,j).
							sudoku.setCellValue(i, j, val);
							nonStable = true;
							break;
						}
					}
				}
			}
			// complete the scan by validating the new assignments with
			// candidate reduction
			if (nonStable) {// at least one entry was assigned - apply candidate reduction
				boolean assignment = candidateReduction(sudoku);
				if (!assignment)
					return false; // no solution!
			}
		}
		return true;
	}

	/**
	 * Hidden pair: A pair of values is called hidden if it occurs exactly twice in
	 * two unit cells (and none of its values occurs in the other unit cells). Then,
	 * all other candidates in these two cells can be dropped
	 * 
	 * @param Sudoku puzzle
	 * @return false when assignment fails - no value can be assigned to one of the
	 *         entries, otherwise returns true
	 * @throws IOException
	 */
	static boolean hiddenPair(Sudoku sudoku) throws IOException {
		boolean nonStable = true;
		List<Pair<Integer, Integer>> nonAssignedCells = sudoku.getNonAssignedCells();
		if (nonAssignedCells == null)
			return false; // one of the non-assigned cells has no legal candidates left - no solution
		if (nonAssignedCells.size() == 0)// no cells to assign - puzzle solved
			return true;

		for (Pair<Integer, Integer> cell : nonAssignedCells) {
			int i = cell.getKey();
			int j = cell.getValue();
			List<Integer> vals = sudoku.getCellCandidates(i, j);
			if (vals.size() < 3)
				continue; // examine only non-assigned entries with at least three vals
			for (int p1 = 0; p1 < vals.size() - 1; p1++) {
				for (int p2 = p1 + 1; p2 < vals.size(); p2++) {
					int v1 = vals.get(p1);
					int v2 = vals.get(p2);
					// check pair occurrence in the cell units
					for (UnitType unit : UnitType.values()) {
						List<Pair<Integer, Integer>> unitCells = sudoku.getUnitCells(i, j, unit);
						// check pair occurrence in unit
						int v1counter = 0, v2counter = 0, paircounter = 0;
						List<Pair<Integer, Integer>> coords = new ArrayList<>();// keep unit cells containing
																				// the pairs
						for (Pair<Integer, Integer> p : unitCells) {
							if (sudoku.contains(p.getKey(), p.getValue(), v1)
									&& sudoku.contains(p.getKey(), p.getValue(), v2)) {
								v1counter++;
								v2counter++;
								paircounter++;
								coords.add(p);
							} else if (sudoku.contains(p.getKey(), p.getValue(), v1))
								v1counter++;
							else if (sudoku.contains(p.getKey(), p.getValue(), v2))
								v2counter++;
						}
						if (v1counter == 1 && v2counter == 1 && paircounter == 1) {
							// the pair appears exactly twice in the unit, prune all other values in
							// these two unit cells.
							sudoku.setCellCandidates(i, j, v1, v2);
							Pair<Integer, Integer> pair = coords.get(0);
							sudoku.setCellCandidates(pair.getKey(), pair.getValue(), v1, v2);
							nonStable = true;
						}
					}
				}
			}
		}

		if (nonStable) {// at least one entry was assigned - lets continue with candidate reduction
			boolean assignment = candidateReduction(sudoku);
			if (!assignment)
				return false; // no solution!
		}
		return true;
	}

	/**
	 * Naked pair: A pair is called naked if it is lonely in a cell. If a pair is
	 * naked in two cells in a unit, then it can be dropped from all other cells in
	 * the unit.
	 * 
	 * @param Sudoku puzzle
	 * @return false when assignment fails - no value can be assigned to one of the
	 *         entries, otherwise returns true
	 * @throws IOException
	 */
	static boolean nakedPair(Sudoku sudoku) throws IOException {
		boolean nonStable = false;
		List<Pair<Integer, Integer>> nonAssignedCells = sudoku.getNonAssignedCells();
		if (nonAssignedCells == null) {
			return false; // one of the non-assigned cells has no legal candidates left - no solution
		}
		if (nonAssignedCells.size() == 0)
			// no cells to assign - puzzle solved
			return true;

		for (Pair<Integer, Integer> pair : nonAssignedCells) {
			int i = pair.getKey();
			int j = pair.getValue();
			List<Integer> vals = sudoku.getCellCandidates(i, j);
			if (vals.size() != 2)
				continue; // look for naked pairs only
			int v1 = vals.get(0);
			int v2 = vals.get(1);
			// check nakedness occurrence in the cell units
			for (UnitType unit : UnitType.values()) {
				List<Pair<Integer, Integer>> unitCells = sudoku.getUnitCells(i, j, unit);
				// check nakedness occurrence in unit
				boolean nakedFound = false;
				List<Pair<Integer, Integer>> coords = new ArrayList<>();
				for (Pair<Integer, Integer> p : unitCells) {
					if (sudoku.contains(p.getKey(), p.getValue(), v1)
							&& sudoku.contains(p.getKey(), p.getValue(), v2)) {
						if (sudoku.getCellCandidates(p.getKey(), p.getValue()).size() == 2)
							nakedFound = true;
						else
							coords.add(p);
					}
				}
				if (nakedFound) {
					// the naked pair appears exactly twice in the unit, prune the pair from all
					// other
					// cells containing it.
					for (Pair<Integer, Integer> p : coords) {
						sudoku.removeCellCandidate(p.getKey(), p.getValue(), v1);
						sudoku.removeCellCandidate(p.getKey(), p.getValue(), v2);
					}
					nonStable = true;
				}
			}
		}
		if (nonStable) {
			boolean assignment = candidateReduction(sudoku);
			if (!assignment)
				return false; // no solution!
		}
		return true;
	}

	static int searchCounter = 0; // count the number of call to the solve method

	/**
	 * DFS Search for valid allocation
	 * 
	 * @param puzzle
	 * @param constraints
	 * @return
	 * @throws IOException
	 */
	static Sudoku solve(Sudoku sudoku, String heuristics) throws IOException {
		searchCounter++;
		if (searchCounter % 1000000 == 0)
			System.out.print(searchCounter / 1000000 + " ");
		if (searchCounter > 10000000) // 10M/50/100M
			return null;

		if (sudoku.isPuzzleSolved())
			return sudoku;

		if (heuristics != null && heuristics.contains("r")) {
			boolean assigned = candidateReduction(sudoku);
			if (assigned && sudoku.isPuzzleSolved()) {
				return sudoku;
			}
		}

		if (heuristics != null && heuristics.contains("u")) {
			boolean prune = uniqueCandidate(sudoku);
			if (prune && sudoku.isPuzzleSolved()) {
				return sudoku;
			}
		}

		if (heuristics != null && heuristics.contains("h")) {
			boolean hiddenPrune = hiddenPair(sudoku);
			if (hiddenPrune && sudoku.isPuzzleSolved()) {
				return sudoku;
			}
		}

		if (heuristics != null && heuristics.contains("n")) {
			boolean nakedPrune = nakedPair(sudoku);
			if (nakedPrune && sudoku.isPuzzleSolved()) {
				return sudoku;
			}
		}

		// collect all non-assigned cells in the puzzle
		List<Triple<Integer, Integer, Integer>> nonAssignedCells = sudoku.getNonAssignedCellsWithCardinality();
		if (nonAssignedCells == null)
			return null; // no soluiton)
		if (nonAssignedCells.size() == 0)
			// no cells to assign - puzzle solved
			return sudoku;

		// sort the non-assigned cell according to their cardinality
		nonAssignedCells.sort(new CellComparator());

		for (Triple<Integer, Integer, Integer> tr : nonAssignedCells) {
			List<Integer> vals = sudoku.getCellCandidates(tr.getLeft(), tr.getMiddle());
			for (Integer val : vals) {
				if (sudoku.isLegalAssignment(tr.getLeft(), tr.getMiddle(), val)) {
					Sudoku newSudoku = sudoku.clone();
					newSudoku.setCellValue(tr.getLeft(), tr.getMiddle(), val);
					Sudoku solved = solve(newSudoku, heuristics);
					if (solved != null && solved.isPuzzleSolved()) {
						return solved;
					}
				}
			} // next val
			return null; // no value can be assigned to (i,j)- the pass reaches dead-end - return null
							// the the upper level
		} // next cell
		return null;
	}

	static Properties getArgs(String[] args) {
		Options options = new Options();
		Option opStrategies = new Option("s", "strategies", true,
				"Strategies [r - cand reduction, u - uniquenss in unit, h - hidden pairs, n - naked pairs]");
		opStrategies.setOptionalArg(true);
		options.addOption(opStrategies);

		Option blockSize = new Option("b", "block size", true, "Block Size");
		blockSize.setOptionalArg(true);
		options.addOption(blockSize);

		Option opFile = new Option("f", "file", true, "Puzzle file (or dir)");
		opFile.setOptionalArg(false);
		options.addOption(opFile);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("Commands:", options);
			System.exit(1);
			return null;
		}

		Properties p = new Properties();
		String s = cmd.getOptionValue("strategies");
		if (s != null)
			p.setProperty("strategies", s);
		String b = cmd.getOptionValue("block size");
		if (b != null)
			p.setProperty("block size", b);
		String f = cmd.getOptionValue("file");
		if (f != null)
			p.setProperty("file", f);

		return p;
	}

	private static void printHelp() {
		System.out.println("Usage: SudokuSolver -f <file-name> -b <block-size> -s [ruhn]");
		System.out
				.println("Strategies:\nr - candidate reduction\n u- uninquness\n h - hidden pairs\n n - naked pairs\n");
	}

	public static void main(String[] args) {

		try {
			Properties prop = getArgs(args);
			String strategies = (String) prop.getProperty("strategies");

			String argBlockSize = (String) prop.getProperty("block size");
			int B = argBlockSize != null ? Integer.parseInt(argBlockSize) : 3;

			String fileName = (String) prop.getProperty("file");
			if (fileName == null) {
				System.out.println("No file name!");
				printHelp();
				System.exit(1);
			}
			File f = new File(fileName);
			if (!f.isDirectory() && !f.isFile()) {
				System.out.println("Wrong file name!");
				printHelp();
				System.exit(1);
			}

			System.out.printf("Args:%n Strategies = %s%n Block = %d%n file = %s%n%n", strategies, B, f.getPath());

			String[] ext = { "txt" };
			List<File> files = (List<File>) FileUtils.listFiles(f, ext, true);
			Collections.sort(files);

			int[] counters = new int[files.size()];
			int callsSum = 0;
			int puzzleCount = 0;
			int puzzleSolved = 0;
			int maxNumCalls = 0;

			long time = System.currentTimeMillis();

			for (File file : files) {

				Sudoku sudoku = new Sudoku(B, file);
				sudoku.printPuzzle(file.getName() + " Original", false);

				searchCounter = 0;
				Sudoku solvedSudoku = solve(sudoku, strategies);
				if (solvedSudoku != null) {
					solvedSudoku.printPuzzle(file.getName() + " S-Solved: " + searchCounter, false);
					counters[puzzleCount++] = searchCounter;
					callsSum += searchCounter;
					puzzleSolved++;
					maxNumCalls = Math.max(maxNumCalls, searchCounter);
				} else {
					counters[puzzleCount++] = 0;
					System.out.println(file.getName() + ": NO SOLUTION!\n");
				}
			}
			time = System.currentTimeMillis() - time;

			for (File file : files)
				System.out.printf("\t%10s", file.getName());
			System.out.println();
			for (int i = 0; i < puzzleCount; i++)
				System.out.printf("\t%10d", counters[i]);
			System.out.println();
			System.out.printf(
					"#Solved puzzles = %d, Avg. Running time = %5.3f seconds, Avg #calls = %5.3f, Max #calls %d%n",
					puzzleSolved, ((float) time / (1000)) / puzzleSolved, ((float) callsSum) / puzzleSolved,
					maxNumCalls);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}

	}

}

/**
 * This cell comparator is used for sorting unset cells in the puzzle according
 * to the number of their legal candidates. (key, val) pair represents a cell
 * where key is the cell index and val is its number of legal candidates
 *
 */
class CellComparator implements Comparator<Triple<Integer, Integer, Integer>> {
	@Override
	public int compare(Triple<Integer, Integer, Integer> o1, Triple<Integer, Integer, Integer> o2) {
		if (o1.getRight() == o1.getRight())
			return o1.getLeft() - o2.getLeft();
		else
			return o1.getRight() - o1.getRight();
	}
}
