package com.io.sudoku;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.util.Pair;

/**
 * A Sudoku class maintains all operations on the Sudoku puzzle
 * 
 * @author david.carmel@gmail.com
 *
 */

public class Sudoku {

	public static enum UnitType {
		Row, Column, Block
	};

	public int B = 3; // default block dimension
	public int N = B * B; // default puzzle dimension

	private String fileName = null;

	public String getFileName() {
		return fileName;
	}

	/**
	 * main table - at the end should be fully assigned
	 */
	private int[][] puzzle = null;

	/**
	 * An auxiliary table - used for validating the legality of a new assignment,
	 * and for holding all available assignments per cell. constraints[i][j] - holds
	 * the legal assignments for cell (i,j); constraints[0][j] - holds all final
	 * assignments in column j; constraints[i][0] - holds all final assignments in
	 * row i; constraints[N+1][k] - holds all final assignments in the k'th block of
	 * (i,j)
	 */
	private BitSet[][] constraints = null;

	/**
	 * A list of all non-assigned cells in the puzzle
	 */
	private List<Pair<Integer, Integer>> nonAssignedCells = null;

	/**
	 * Construct an empty Sudoku puzzle of size BlockSize^2 x BlockSize^2
	 * 
	 * @param BlockSize - the size of the basic table block.
	 */
	private Sudoku(int BlockSize) {
		B = BlockSize;
		N = B * B;
		puzzle = new int[N + 1][N + 1];
		constraints = new BitSet[N + 2][N + 1];
		nonAssignedCells = new ArrayList<Pair<Integer, Integer>>();
	}

	/**
	 * Construct a Sudoku puzzle by reading it from a file
	 * 
	 * @param BlockSize
	 * @param File      f
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public Sudoku(int BlockSize, File f) throws NumberFormatException, IOException {
		this(BlockSize);
		fileName = f.getName();
		puzzle = readPuzzle(f);
		if (puzzle == null) {
			throw new IOException("Non-valid file! " + f.getName());
		}
		// set the constraints table
		for (int i = 0; i <= N + 1; i++) {
			for (int j = 0; j <= N; j++) {
				constraints[i][j] = new BitSet();
			}
		}
		for (int i = 1; i <= N; i++) {
			for (int j = 1; j <= N; j++) {
				if (puzzle[i][j] == 0) {
					constraints[i][j].set(1, N + 1); // set to all possible values for empty cells
					nonAssignedCells.add(new Pair<Integer, Integer>(i, j));
				} else {
					// a clue - set the fixed value
					constraints[i][j].set(puzzle[i][j], true);
					// set the clue val in RCB to enable assignment validation
					constraints[i][0].set(puzzle[i][j], true);
					constraints[0][j].set(puzzle[i][j], true);
					constraints[N + 1][getBlockIndex(i, j)].set(puzzle[i][j], true);
				}
			}
		}
	}

	/**
	 * return a cloned Sudoku object
	 */
	public Sudoku clone() {
		Sudoku s = new Sudoku(this.B);
		s.fileName = this.getFileName();
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
		// clone the nonAssignedCells
		s.nonAssignedCells.addAll(this.nonAssignedCells);
		return s;
	}

	/**
	 * @return the block index ([1..N]) of an (i,j) cell. The Block index returned
	 *         is the entry to in constraints table: constraints[N+1]k]
	 * @param i
	 * @param j
	 */
	private int getBlockIndex(int i, int j) {
		// cr & cc are the coordinates of the block's top left cell
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
	private int[][] readPuzzle(File f) throws NumberFormatException, IOException {
		int[][] out = new int[N + 1][N + 1];
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		int row = 1;
		try {
			while ((line = br.readLine()) != null) {
				if (line.isBlank() || row > N)
					continue;
				String[] vals = line.split(" ");
				if (vals.length != N)
					return null;
				for (int j = 0; j < vals.length; j++) {
					out[row][j + 1] = Integer.parseInt(vals[j]);
				}
				row++;
			}
		} finally {
			br.close();
		}
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
	 * Validate that all units of the puzzle are full - each contains all values in
	 * the range 1..N
	 * 
	 * @param puzzle
	 * @return true is the puzzle is solved
	 */
	public boolean isPuzzleSolved() {
		// check rows/col/block )
		for (int i = 1; i <= N; i++) {
			if ((this.constraints[i][0].nextClearBit(1) < N + 1) || (this.constraints[0][i].nextClearBit(1) < N + 1)
					|| (this.constraints[N + 1][i].nextClearBit(1) < N + 1))
				return false;
		}
		return true;
	}

	/**
	 * Examine that assigning val into cell (i,j) is legal - it is not already been
	 * assigned to any of the (i,j) units
	 * 
	 * @param i
	 * @param j
	 * @param val
	 * @return
	 */
	public boolean isLegalAssignment(int i, int j, int val) {
		// return true iff val is not assigned already to one of the units of (i,j)
		return (!constraints[i][0].get(val) && !constraints[0][j].get(val)
				&& !constraints[N + 1][getBlockIndex(i, j)].get(val));
	}

	/**
	 * 
	 * @param i
	 * @param j
	 * @return all legal candidates that can be assigned to cell (i,j)
	 */
	public List<Integer> getCellCandidates(int i, int j) {
		List<Integer> vals = new ArrayList<>();
		this.constraints[i][j].stream().forEach(vals::add);
		return vals;
	}

	/**
	 * Set the cell value, mark the value in all related units, and remove the cell
	 * from the list of nonAssigned cells.
	 * 
	 * @param i
	 * @param j
	 * @param val
	 */
	public void setCellValue(int i, int j, int val) {
		this.puzzle[i][j] = val;
		// set val as a singleton in constraints[i][j]
		this.constraints[i][j].clear();
		this.constraints[i][j].set(val, true);
		// set val in all units
		this.constraints[i][0].set(val, true);
		this.constraints[0][j].set(val, true);
		this.constraints[N + 1][getBlockIndex(i, j)].set(val, true);
		// remove from nonAssignedCells
		nonAssignedCells.removeIf(x -> x.getKey() == i && x.getValue() == j);
	}

	/**
	 * Update the cell's candidates
	 * 
	 * @param i
	 * @param j
	 */
	public void setCellCandidates(int i, int j, int... vals) {
		this.constraints[i][j].clear();
		for (int v : vals)
			this.constraints[i][j].set(v, true);
	}

	/**
	 * Remove candidate val from cell (i,j)
	 * 
	 * @param i
	 * @param j
	 * @param val
	 * @return true if the cell was finally set in the case that only one candidate
	 *         left after removal
	 */
	public void removeCellCandidate(int i, int j, int val) {
		this.constraints[i][j].clear(val);
	}

	/**
	 * 
	 * @param i
	 * @param j
	 * @return the cell value, or 0 is not set
	 */
	public int getCellValue(int i, int j) {
		return this.puzzle[i][j];
	}

	/**
	 * @return true if both puzzles are equal
	 * @param s
	 * @return
	 */
	public boolean isEqual(Sudoku s) {
		if (this.N != s.N)
			return false;
		for (int i = 1; i <= N; i++)
			for (int j = 1; j <= N; j++)
				if (this.puzzle[i][j] != s.puzzle[i][j])
					return false;
		return true;
	}

	/**
	 * 
	 * @return the list of non-assigned cells. We clone the list from concurrency
	 *         considerations
	 */
	public List<Pair<Integer, Integer>> getNonAssignedCells() {
		return new ArrayList<Pair<Integer, Integer>>(nonAssignedCells);
	}

	/**
	 * 
	 * @return a list of Triplets of <i,j,cardinality>
	 */
	public List<Triple<Integer, Integer, Integer>> getNonAssignedCellsWithCardinality() {
		List<Triple<Integer, Integer, Integer>> out = new ArrayList<>();
		for (Pair<Integer, Integer> p : nonAssignedCells) {
			if (this.constraints[p.getKey()][p.getValue()].isEmpty())
				return null; // no solution
			out.add(Triple.of(p.getKey(), p.getValue(), this.constraints[p.getKey()][p.getValue()].cardinality()));

		}
		return out;
	}

	/**
	 * 
	 * @param i
	 * @param j
	 * @return true if the cell (i,j) is already been set
	 */
	public boolean isSet(int i, int j) {
		return this.puzzle[i][j] != 0;
	}

	private Pair<Integer, Integer> getBlockCoord(int i, int j) {
		int cr = ((i - 1) / B) * B + 1;
		int cc = ((j - 1) / B) * B + 1;
		return new Pair<Integer, Integer>(cr, cc);
	}

	public boolean isEmpty(int i, int j) {
		return constraints[i][j].isEmpty();
	}

	public boolean contains(int i, int j, int val) {
		return constraints[i][j].get(val);
	}

	/**
	 * 
	 * @param i
	 * @param j
	 * @param unit of type UnitType (Row/Column/Block)
	 * @return all unit cells of (i,j) (excluding (i,j))
	 */
	public List<Pair<Integer, Integer>> getUnitCells(int i, int j, UnitType unit) {
		List<Pair<Integer, Integer>> out = new ArrayList<>();
		switch (unit) {
		case Row:
			for (int k = 1; k <= N; k++) {
				if (k != j)
					out.add(new Pair<Integer, Integer>(i, k));
			}
			break;
		case Column:
			for (int k = 1; k <= N; k++) {
				if (k != i)
					out.add(new Pair<Integer, Integer>(k, j));
			}
			break;
		case Block:
			Pair<Integer, Integer> block = getBlockCoord(i, j);
			for (int k = block.getKey(); k < block.getKey() + B; k++) {
				for (int l = block.getValue(); l < block.getValue() + B; l++) {
					if (k != i || l != j)
						out.add(new Pair<Integer, Integer>(k, l));
				}
			}
			break;
		}
		return out;
	}
}
