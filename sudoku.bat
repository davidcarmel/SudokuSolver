#!/bin/bash
#
# Usage: ./sudoku.bat ./data/95puzzles/p1.txt 3 "runh"
# parameters: 
#	$1 - file name 
#	$2 - BlockSize  (3,4, etc.)
#	$3 strategies "[ruhn]"
#		"" no strategies - only backtracking (run might take a long time)
#		r - Candidate Reduction
#		u - Uniqueness
#		h - Hidden Pairs
#		n - NamkedPairs
#
#
java -cp sudokuSolver.jar com.io.sudoku.SudokuSolver -f $1 -b $2 -s $3
