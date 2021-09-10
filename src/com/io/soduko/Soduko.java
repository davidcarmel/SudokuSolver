package com.io.soduko;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class Soduko {

	public final static int B = 3; // block dimension
	public final static int N = B * B; // the main puzzle dimension

	// main tables
	int[][] puzzle = new int[N + 1][N + 1]; // main table - at the end should be fully assigned

	/**
	 * legals: an auxiliary table - used for validating the legality of a new
	 * assignment, and for holding all available assignments per cell. legals[i][j]
	 * - holds the legal assignments for cell (i,j); legals[0][j] - holds all final
	 * assignments per column j; legals[i][0] - holds all final assignments per row
	 * i legals[N+1][k] - holds all final assignments per the k'th block of (i,j)
	 */
	BitSet[][] constraints = new BitSet[N + 2][N + 1];

	protected Soduko() {
	}

	/**
	 * Construct a Soduko puzzle by reading from a file
	 * 
	 * @param f
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public Soduko(File f) throws NumberFormatException, IOException {
		puzzle = readPuzzle(f);
		if (puzzle == null) {
			throw new IOException("Non-valid file! "+ f.getName());
		}
		//set the constraints table
		for (int i = 0; i <= N + 1; i++) {
			for (int j = 0; j <= N; j++) {
				constraints[i][j] = new BitSet();
			}
		}
		for (int i = 1; i <= N; i++) {
			for (int j = 1; j <= N; j++) {
				if (puzzle[i][j] == 0) {
					constraints[i][j].set(1, N + 1); // set to all possible values
				} else {
					// a clue - set the fixed value
					constraints[i][j].set(puzzle[i][j], true);
					// set the clue val in RCB to enable assignment validation
					constraints[i][0].set(puzzle[i][j], true);
					constraints[0][j].set(puzzle[i][j], true);
					constraints[N + 1][blockIndex(i, j)].set(puzzle[i][j], true);
				}
			}
		}
	}

	/**
	 * return a cloned Soduko object
	 */
	public Soduko clone() {
		Soduko s = new Soduko();
		// clone the puzzle
		for (int i = 1; i <= N; i++) {
			for (int j = 1; j <= N; j++) {
				s.puzzle[i][j] = puzzle[i][j];
			}
		}
		// clone the constraints table
		for (int i = 0; i <= N + 1; i++) {
			for (int j = 0; j <= N; j++) {
				s.constraints[i][j] = (BitSet) constraints[i][j].clone();
			}
		}
		return s;
	}
	
	/**
	 * @return the block index ([1..N]) of an (i,j) cell
	 * @param i
	 * @param j
	 */
	protected static int blockIndex(int i, int j) {
		//cr & cc are the coordinates of the block's top left cell
		int cr = ((i - 1) / B) * B + 1;
		int cc = ((j - 1) / B) * B + 1;
		return (cr - 1) / B * B + cc / B + 1; // (cr,cc) --> [1..N]
	}

	/**
	 * read the puzzle from file
	 * 
	 * @throws IOException
	 * @throws NumberFormatException *
	 */
	private static int[][] readPuzzle(File f) throws NumberFormatException, IOException {
		int[][] out = new int[N + 1][N + 1];
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		int row = 1;
		while ((line = br.readLine()) != null) {
			if (line.isBlank() || row > N)
				continue;
			String[] vals = line.split(" ");
			if (vals.length != N) return null;
			for (int j = 0; j < vals.length; j++) {
				out[row][j + 1] = Integer.parseInt(vals[j]);
			}
			row++;
		}
		br.close();
		return out;
	}

	/**
	 * print the puzzle
	 * 
	 * @param puzzle
	 */
	public void printPuzzle(String header, boolean debug) {
		System.out.println(header);
		for (int i = 1; i <= N; i++) {
			for (int j = 1; j <= N; j++) {
				System.out.printf("%3d", puzzle[i][j]);
			}
			if (debug) {
				System.out.print("\t");
				for (int j = 1; j <= N; j++) {
					System.out.printf("%30s", constraints[i][j]);
				}
			}
			System.out.println();
		}
		System.out.println();
	}

	/**
	 * Validate that all RCB in the puzzle are legal - each contains all values between 1
	 * to N
	 * 
	 * @param puzzle
	 * @return true is the puzzle is solved
	 */
	public boolean checkPuzzle() {
		// check rows/col/block )RCB(
		for (int i = 1; i <= N; i++) {
			if ((this.constraints[i][0].nextClearBit(1) < N + 1) || (this.constraints[0][i].nextClearBit(1) < N + 1)
					|| (this.constraints[N + 1][i].nextClearBit(1) < N + 1))
				return false;
		}
		return true;
	}

	/**
	 * Examine that assigning val into cell (i,j) is legal
	 * 
	 * @param i
	 * @param j
	 * @param val
	 * @return
	 */
	public boolean isLegalAssignemnt(int i, int j, int val) {
		// return true iff val is not assigned already in the RCB
		return (!constraints[i][0].get(val) && !constraints[0][j].get(val) && !constraints[N + 1][blockIndex(i, j)].get(val));
	}

	List<Integer> getCellLegalValues(int i, int j) {
		List<Integer> vals = new ArrayList<>();
		this.constraints[i][j].stream().forEach(vals::add);
		// int[] arr = vals.stream().mapToInt(Integer::intValue).toArray();
		return vals;
	}

	void setCellValue(int i, int j, int val) {
		this.puzzle[i][j] = val;
		//set val as a singleton in constraints[i][j]
		this.constraints[i][j].clear();
		this.constraints[i][j].set(val, true);
		//set val in RCB
		this.constraints[i][0].set(val, true);
		this.constraints[0][j].set(val, true);
		this.constraints[N + 1][blockIndex(i, j)].set(val, true);
	}

	void setCellLegalValues(int i, int j, List<Integer> vals) throws IOException {
		if (vals == null || vals.isEmpty())
			throw new IOException("setCellLegalValues: Illegal input! " +vals);
		else if (vals.size() == 1)
			setCellValue(i, j, vals.get(0));
		else {
			this.puzzle[i][j] = 0;
			this.constraints[i][j].clear();
			for (Integer val : vals)
				this.constraints[i][j].set(val, true);
			int index = blockIndex(i, j);
			this.constraints[N + 1][index].clear();
			for (Integer val : vals)
				this.constraints[N + 1][index].set(val, true);
		}
	}

	void removeCellValue(int i, int j, int val) {
		this.constraints[i][j].clear(val);
	}

	/**
	 * @return true if both puzzles are equal
	 * @param s
	 * @return
	 */
	public boolean isEqual(Soduko s) {
		if (this.N != s.N)
			return false;
		for (int i = 1; i <= N; i++)
			for (int j = 1; j <= N; j++)
				if (this.puzzle[i][j] != s.puzzle[i][j])
					return false;
		return true;
	}

}
