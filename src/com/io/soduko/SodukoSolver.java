package com.io.soduko;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;

/**
 * This cell comparator is used for sorting non-assigned cells in the puzzle
 * according to their number of legal values A (key, val) pair represents a cell
 * where key is the cell index and val is its number of legal values
 *
 */
class CellComparator implements Comparator<Pair<Integer, Integer>> {
	@Override
	public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
		int val1 = o1.getValue();
		int val2 = o2.getValue();
		if (val1 == val2)
			return o1.getKey() - o2.getKey();
		else
			return val1 - val2;
	}
}

/**
 * This class solve a Soduko puzzle by search over the domain space and by
 * enforcing constraints on the cell legal values
 * 
 * Some known techniques for Soduko solving:
 * https://www.kristanix.com/sudokuepic/sudoku-solving-techniques.php
 * 
 * Some free online Soduko puzzles to experiment with:
 * http://lipas.uwasa.fi/~timan/sudoku/
 * 
 * @author david.carmel@gmail.com
 *
 */

public class SodukoSolver {

	/**
	 * identify legal values for each non-assigned cell based on its mates in RCB -
	 * remove all values already assigned to other RCB cells
	 * 
	 * @param Soduko puzzle
	 * @return false when assignment is illegal - no value can be assigned to one of
	 *         the cells, otherwise returns true
	 */
	static boolean assignLegalVals(Soduko soduko) {
		boolean stable = false;
		while (!stable) {
			stable = true;
			for (int i = 1; i <= soduko.N; i++) {
				for (int j = 1; j <= soduko.N; j++) {
					if (soduko.constraints[i][j].cardinality() > 1) {
						// a non-assigned cell
						for (int k = 1; k <= soduko.N; k++) {
							// remove all legal values covered in the row
							if (k != j && soduko.constraints[i][k].cardinality() == 1)
								soduko.removeCellValue(i, j, soduko.constraints[i][k].nextSetBit(0));
							// remove all values covered in the col
							if (k != i && soduko.constraints[k][j].cardinality() == 1)
								soduko.removeCellValue(i, j, soduko.constraints[k][j].nextSetBit(0));
						}
						// remove all values covered in the block
						// find the top left corner of the block of cell (i,j)
						int cr = ((i - 1) / soduko.B) * soduko.B + 1;
						int cc = ((j - 1) / soduko.B) * soduko.B + 1;
						for (int k = cr; k < cr + soduko.B; k++) {
							for (int l = cc; l < cc + soduko.B; l++) {
								if ((k != i || l != j) && soduko.constraints[k][l].cardinality() == 1)
									soduko.removeCellValue(i, j, soduko.constraints[k][l].nextSetBit(0));
							}
						}
						if (soduko.constraints[i][j].isEmpty()) {
							// if no candidates left for this cell, there is no solution for the puzzle
							return false;
						} else if (soduko.constraints[i][j].cardinality() == 1) {
							// sole candidate - only one valid value left for this cell - set it
							soduko.setCellValue(i, j, soduko.constraints[i][j].nextSetBit(0));
							stable = false; // any setting in a puzzle cell requires a new pass
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Unique Candidate: If a value can only be set in one cell within a RCB, then
	 * it is guaranteed to fit there.
	 * 
	 * @param Soduko puzzle
	 * @return false when assignment is illegal - no value can be assigned to one of
	 *         the entries, otherwise returns true
	 */
	static boolean uniqueCandidate(Soduko soduko) {
		boolean scanCompleted = false;
		while (!scanCompleted) {
			scanCompleted = true;
			for (int i = 1; i <= soduko.N; i++) {
				for (int j = 1; j <= soduko.N; j++) {
					if (soduko.puzzle[i][j] == 0) {
						// we check only non-fixed entries
						List<Integer> vals = soduko.getCellLegalValues(i, j);
						// check value uniqueness in a RCB
						for (Integer val : vals) {
							// check uniqueness in row
							int counter = 0;
							for (int k = 1; k <= soduko.N; k++) {
								if (soduko.constraints[i][k].get(val))
									counter++;
							}
							if (counter == 1) { // val appears only once in the row, assign it.
								soduko.setCellValue(i, j, val);
								scanCompleted = false;
								break;
							}
							// check uniqueness in col
							counter = 0;
							for (int k = 1; k <= soduko.N; k++) {
								if (soduko.constraints[k][j].get(val))
									counter++;
							}
							if (counter == 1) { // val appears only once in the col, assign it.
								soduko.setCellValue(i, j, val);
								scanCompleted = false;
								break;
							}
							// check uniqueness in block
							int cr = ((i - 1) / soduko.B) * soduko.B + 1;
							int cc = ((j - 1) / soduko.B) * soduko.B + 1;
							counter = 0;
							for (int k = cr; k < cr + soduko.B; k++) {
								for (int l = cc; l < cc + soduko.B; l++) {
									if (soduko.constraints[k][l].get(val))
										counter++;
								}
							}
							if (counter == 1) { // val appears only once in the square, assign it.
								soduko.setCellValue(i, j, val);
								scanCompleted = false;
								break;
							}
						}
					}
				}
			}
			// complete the scan by validating the new assignments with
			// assignLegalVals
			if (!scanCompleted) {// at least one entry was assigned - the puzzle should be revalidated by
									// assignLegalVals
				boolean assignment = assignLegalVals(soduko);
				if (!assignment)
					return false; // no solution!
			}
		}
		return true;
	}

	/**
	 * Hidden pair: A pair of values is called hidden if it occurs exactly twice in two
	 * RCB cells (and none of its values is legal in the other cells). Then, all other
	 * candidates in these two cells can be dropped
	 * 
	 * @param Soduko puzzle
	 * @return false when assignment fails - no value can be assigned to one of the
	 *         entries, otherwise returns true
	 */
	static boolean hiddenPair(Soduko soduko) {
		boolean assignement = false;
		for (int i = 1; i <= soduko.N; i++) {
			for (int j = 1; j <= soduko.N; j++) {
				if (soduko.constraints[i][j].cardinality() < 3)
					continue; //examine only non-assigned entries with at least three vals
				List<Integer> vals = soduko.getCellLegalValues(i, j);
				for (int p1 = 0; p1 < vals.size() - 1; p1++) {
					for (int p2 = p1 + 1; p2 < vals.size(); p2++) {
						int v1 = vals.get(p1);
						int v2 = vals.get(p2);

						// check uniqueness in row
						int v1counter = 0, v2counter = 0, paircounter = 0;
						List<Integer> coords = new ArrayList<>();//holds all row cells containing the pairs
						for (int k = 1; k <= soduko.N; k++) {
							if (soduko.constraints[i][k].get(v1) && soduko.constraints[i][k].get(v2)) {
								v1counter++;
								v2counter++;
								paircounter++;
								coords.add(k);
							} else if (soduko.constraints[i][k].get(v1))
								v1counter++;
							else if (soduko.constraints[i][k].get(v2))
								v2counter++;
						}
						if (v1counter == 2 && v2counter == 2 && paircounter == 2) {
							// the pair appears exactly twice in the row, prune all other values in
							// the other cells.
							for (Integer k : coords) {
								soduko.constraints[i][k].clear();
								soduko.constraints[i][k].set(v1, true);
								soduko.constraints[i][k].set(v2, true);
							}
							assignement = true;
						}

						// check uniqueness in col
						v1counter = 0;
						v2counter = 0;
						paircounter = 0;
						coords.clear();
						for (int k = 1; k <= soduko.N; k++) {
							if (soduko.constraints[k][j].get(v1) && soduko.constraints[k][j].get(v2)) {
								v1counter++;
								v2counter++;
								paircounter++;
								coords.add(k);
							} else if (soduko.constraints[k][j].get(v1))
								v1counter++;
							else if (soduko.constraints[k][j].get(v2))
								v2counter++;
						}
						if (v1counter == 2 && v2counter == 2 && paircounter == 2) {
							// the pair appears exactly twice in the col, prune all other values in
							// the other cells.
							for (Integer k : coords) {
								soduko.constraints[k][j].clear();
								soduko.constraints[k][j].set(v1, true);
								soduko.constraints[k][j].set(v2, true);
							}
							assignement = true;
						}

						// check uniqueness in block
						int cr = ((i - 1) / soduko.B) * soduko.B + 1;
						int cc = ((j - 1) / soduko.B) * soduko.B + 1;
						v1counter = 0;
						v2counter = 0;
						paircounter = 0;
						coords.clear();
						for (int k = cr; k < cr + soduko.B; k++) {
							for (int l = cc; l < cc + soduko.B; l++) {
								if (soduko.constraints[k][l].get(v1) && soduko.constraints[k][l].get(v2)) {
									v1counter++;
									v2counter++;
									paircounter++;
									coords.add((k - 1) * soduko.N + l);
								} else if (soduko.constraints[k][l].get(v1))
									v1counter++;
								else if (soduko.constraints[k][l].get(v2))
									v2counter++;

							}
						}
						if (v1counter == 2 && v2counter == 2 && paircounter == 2) {
							// the pair appears exactly twice in the block, prune all other values in
							// the other cells.
							for (Integer coord : coords) {
								int k = (coord - 1) / (soduko.N) + 1;
								int l = (coord - 1) % (soduko.N) + 1;
								soduko.constraints[k][l].clear();
								soduko.constraints[k][l].set(v1, true);
								soduko.constraints[k][l].set(v2, true);
							}
							assignement = true;
						}
					}
				}
			}
		}
		if (assignement) {
			assignement = assignLegalVals(soduko);
			return assignement && uniqueCandidate(soduko);
		} else
			return true;
	}

	/**
	 * Naked pair: A pair is called naked if it is lonely in a cell. If a pair is
	 * naked in two cells in a RCB, then it can be dropped from all other
	 * cells in the RCB.
	 * 
	 * @param Soduko puzzle
	 * @return false when assignment fails - no value can be assigned to one of the
	 *         entries, otherwise returns true
	 */
	static boolean nakedPair(Soduko soduko) {
		boolean assignement = false;
		for (int i = 1; i <= soduko.N; i++) {
			for (int j = 1; j <= soduko.N; j++) {
				if (soduko.constraints[i][j].cardinality() != 2)
					continue; // look for naked pairs only
				List<Integer> vals = soduko.getCellLegalValues(i, j);
				int v1 = vals.get(0);
				int v2 = vals.get(1);

				// check nakedness occurrence in row
				int nakedCounter = 0;
				List<Integer> coords = new ArrayList<>();
				for (int k = 1; k <= soduko.N; k++) {
					if (soduko.constraints[i][k].get(v1) && soduko.constraints[i][k].get(v2)) {
						if (soduko.constraints[i][k].cardinality() == 2)
							nakedCounter++;
						else
							coords.add(k);
					}
				}
				if (nakedCounter == 2) {
					// the naked pair appears exactly twice in the row, prune the pair from all other
					// cells containing it.
					for (Integer k : coords) {
						soduko.removeCellValue(i, k, v1);
						soduko.removeCellValue(i, k, v2);
					}
					assignement = true;
				}

				// check nakedness occurrence in col
				nakedCounter = 0;
				coords.clear();
				for (int k = 1; k <= soduko.N; k++) {
					if (soduko.constraints[k][j].get(v1) && soduko.constraints[k][j].get(v2)) {
						if (soduko.constraints[i][k].cardinality() == 2)
							nakedCounter++;
						else
							coords.add(k);
					}
				}
				if (nakedCounter == 2) {
					// the naked pair appears exactly twice in the col, prune the pair from all
					// other cells containing it.
					for (Integer k : coords) {
						soduko.removeCellValue(k, j, v1);
						soduko.removeCellValue(k, j, v2);
					}
					assignement = true;
				}

				// check nakedness occurrence in block
				int cr = ((i - 1) / soduko.B) * soduko.B + 1;
				int cc = ((j - 1) / soduko.B) * soduko.B + 1;
				nakedCounter = 0;
				coords.clear();
				for (int k = cr; k < cr + soduko.B; k++) {
					for (int l = cc; l < cc + soduko.B; l++) {
						if (soduko.constraints[k][l].get(v1) && soduko.constraints[k][l].get(v2)) {
							if (soduko.constraints[k][l].cardinality() == 2)
								nakedCounter++;
							else
								coords.add((k - 1) * soduko.N + l);
						}
					}
				}
				if (nakedCounter == 2) {
					// the naked pair appears exactly twice in the block, prune the pair from all
					// other cells containing it.
					for (Integer coord : coords) {
						int k = (coord - 1) / (soduko.N) + 1;
						int l = (coord - 1) % (soduko.N) + 1;
						soduko.removeCellValue(k, l, v1);
						soduko.removeCellValue(k, l, v2);
					}
					assignement = true;
				}
			}
		}
		if (assignement) {
			assignement = assignLegalVals(soduko);
			return assignement && uniqueCandidate(soduko);
		} else
			return true;
	}

	static int searchCounter = 0; // count the number of call to the solve method

	/**
	 * Recursive Search for valid allocation
	 * 
	 * @param puzzle
	 * @param constraints
	 * @return
	 */
	static Soduko solve(Soduko soduko) {
		searchCounter++;
		if (searchCounter % 100000 == 0)
			System.out.print(searchCounter / 100000 + " ");
		if (searchCounter > 10000000) // 10M
			return null;

		if (soduko.checkPuzzle())
			return soduko;

//		boolean assigned = assignLegalVals(soduko);
//		if (assigned && soduko.checkPuzzle()) {
//			return soduko;
//		}
//
//		boolean prune = uniqueCandidate(soduko);
//		if (prune && soduko.checkPuzzle()) {
//			return soduko;
//		}
//
//		boolean hiddenPrune = hiddenPair(soduko);
//		if (hiddenPrune && soduko.checkPuzzle()) {
//			return soduko;
//		}
//
//		boolean nakedPrune = nakedPair(soduko);
//		if (nakedPrune && soduko.checkPuzzle()) {
//			return soduko;
//		}

		// collect all non-assigned cells in the puzzle
		List<Pair<Integer, Integer>> nonAssignedCells = new ArrayList<>();
		for (int i = 1; i <= soduko.N; i++) {
			for (int j = 1; j <= soduko.N; j++) {
				if (soduko.constraints[i][j].isEmpty())
					return null; // no solution
				if (soduko.puzzle[i][j] == 0)
					nonAssignedCells.add(
							new Pair<Integer, Integer>((i - 1) * soduko.N + j, soduko.constraints[i][j].cardinality()));
			}
		}
		if (nonAssignedCells.size() == 0)
			// no cells to assign - puzzle solved
			return soduko;

		// sort the non-assigned cell according to their cardinality
		nonAssignedCells.sort(new CellComparator());
		
		for (Pair<Integer, Integer> pair : nonAssignedCells) {
			int i = (pair.getKey() - 1) / (soduko.N) + 1;
			int j = (pair.getKey() - 1) % (soduko.N) + 1;
			List<Integer> vals = soduko.getCellLegalValues(i, j);
			for (Integer val : vals) {
				if (soduko.isLegalAssignemnt(i, j, val)) {
					Soduko newSoduko = soduko.clone();
					newSoduko.setCellValue(i, j, val);
					Soduko solved = solve(newSoduko);
					if (solved != null && solved.checkPuzzle()) {
						return solved;
					}
				}
			} // next val
			return null; // no value can be assigned to (i,j)- the pass reaches dead-end - return null the the upper level
		} // next cell
		return null;
	}

	public static void main(String[] args) {

		try {
			String[] ext = { "txt" };
			File f = new File("./data");
			List<File> files = (List<File>) FileUtils.listFiles(f, ext, true);
			Collections.sort(files);
			int[] counters = new int[files.size()];
			int puzzleCount = 0;
			// File[] files = { new File("./data/s09b.txt") };
			// File[] files = { new File("./data/s16.txt") };

			for (File file : files) {

				Soduko soduko = new Soduko(file);
				soduko.printPuzzle(file.getName() + " Original", false);

				searchCounter = 0;
				Soduko solvedSoduko = solve(soduko);
				if (solvedSoduko != null) {
					solvedSoduko.printPuzzle(file.getName() + " S-Solved: " + searchCounter, false);
					counters[puzzleCount++] = searchCounter;
				} else {
					counters[puzzleCount++] = 0;
					System.out.println(file.getName() + ": NO SOLUTION!\n");
				}
			}
			for (File file : files)
				System.out.printf("\t%10s", file.getName());
			System.out.println();
			for (int i = 0; i < puzzleCount; i++)
				System.out.printf("\t%10d", counters[i]);
			System.out.println();
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}

	}

}
