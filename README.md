# SudokuSolver - Solve a Soduko puzzle by Heuristic Search

The package is described in the following blog: 
https://davidcarmel.org/2021/09/13/solving-sudoku-by-heuristic-search/

# parameters: 
#	$1 - file name 
#	$1 - BlockSize  (3,4, etc.)
#	$2 strategies "ruhn"
#		"" no strategies - only backtracking (run might take a long time)
#		r - Candidate Reduction
#		u - Uniqueness
#		h - Hidden Pairs
#		n - NamkedPairs
# Usage: ./sudoku.bat ./data/95puzzles/p1.txt 3 "runh"
#
