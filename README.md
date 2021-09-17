SudokuSolver - Solving a Soduko puzzle by Heuristic Search

The package is described in the following blog: 
https://davidcarmel.org/2021/09/13/solving-sudoku-by-heuristic-search/

Usage: ./sudoku.bat \<file-name\> \<block-size\> \<strategies\>

e.g. ./sudoku.bat ./data/95puzzles/p1.txt  3  "runh"

Parameters: 
  $1 - file name 
  $2 - BlockSize  (3,4, etc.)
  $3 strategies "ruhn"
      "" no strategies - only backtracking (run might take a long time)
      r - Candidate Reduction
      u - Uniqueness
      h - Hidden Pairs
      n - NakedPairs
