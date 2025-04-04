package com.example.xiangqi.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class MoveValidator {
    public static boolean isValidMove(String[][] board, Long redPlayerId, Long blackPlayerId, Long turn,
                                      int fromRow, int fromCol, int toRow, int toCol) {
        String piece = board[fromRow][fromCol];

        // Check if the piece exists
        if (piece.isEmpty()) {
            return false;
        }

        // Check if the piece move out of board
        if (isOutOfBoard(fromRow, fromCol, toRow, toCol)){
            return false;
        }

        // Check if the piece belongs to the current player
        if (!isCorrectTurn(piece, redPlayerId, blackPlayerId, turn)) {
            return false;
        }

        // Ensure the destination is not occupied by the player's own piece
        if (!board[toRow][toCol].isEmpty() && isSameColor(piece, board[toRow][toCol])) {
            return false;
        }

        // Simulate the move on a temporary board
        String[][] tempBoard = DeepClone.clone(board); // Create a copy of the board
        tempBoard[toRow][toCol] = tempBoard[fromRow][fromCol]; // Move the piece
        tempBoard[fromRow][fromCol] = ""; // Empty the old position

        // Check if kings face each other after the move
        if (areKingsFacing(tempBoard)) {
            return false; // Move is invalid if kings face each other
        }

        // Validate move based on the piece type
        return switch (Character.toLowerCase(piece.charAt(0))) {
            case 'r' -> isValidRookMove(board, fromRow, fromCol, toRow, toCol);
            case 'h' -> isValidHorseMove(board, fromRow, fromCol, toRow, toCol);
            case 'c' -> isValidCannonMove(board, fromRow, fromCol, toRow, toCol);
            case 'e' -> isValidElephantMove(board, fromRow, fromCol, toRow, toCol, piece);
            case 'a' -> isValidAdvisorMove(fromRow, fromCol, toRow, toCol, piece);
            case 'k' -> isValidKingMove(fromRow, fromCol, toRow, toCol, piece);
            case 'p' -> isValidPawnMove(fromRow, fromCol, toRow, toCol, piece);
            default -> false;
        };
    }

    private static boolean isOutOfBoard(int fromRow, int fromCol, int toRow, int toCol) {
        // Out of range
        if (fromRow < 0 || fromRow > 9 || toRow < 0 || toRow > 9)
            return true;
        if (fromCol < 0 || fromCol > 8 || toCol < 0 || toCol > 8)
            return true;

        return false;
    }

    private static boolean isCorrectTurn(String piece, Long redPlayerId, Long blackPlayerId, Long turn) {
        // Get Jwt token from Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Get userId from token
        Long userId = jwt.getClaim("uid");

        return (Character.isUpperCase(piece.charAt(0)) && userId.equals(redPlayerId) && userId.equals(turn)) ||
                (Character.isLowerCase(piece.charAt(0)) && userId.equals(blackPlayerId) && userId.equals(turn));
    }

    private static boolean isSameColor(String piece1, String piece2) {
        return (Character.isUpperCase(piece1.charAt(0)) && Character.isUpperCase(piece2.charAt(0))) ||
                (Character.isLowerCase(piece1.charAt(0)) && Character.isLowerCase(piece2.charAt(0)));
    }

    private static boolean isValidPawnMove(int fromRow, int fromCol, int toRow, int toCol, String piece) {
        int direction = Character.isUpperCase(piece.charAt(0)) ? -1 : 1; // Red moves up, Black moves down
        boolean isAcrossRiver = (Character.isUpperCase(piece.charAt(0)) && fromRow <= 4) ||
                (Character.isLowerCase(piece.charAt(0)) && fromRow >= 5);

        // Moving forward
        if (toRow == fromRow + direction && toCol == fromCol) {
            return true;
        }

        // Moving sideways after crossing the river
        return isAcrossRiver && toRow == fromRow && Math.abs(toCol - fromCol) == 1;
    }

    private static boolean isValidRookMove(String[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow == toRow) {
            return isPathClear(board, fromRow, fromCol, toCol, true);
        } else if (fromCol == toCol) {
            return isPathClear(board, fromCol, fromRow, toRow, false);
        }
        return false;
    }

    private static boolean isPathClear(String[][] board, int fixed, int start, int end, boolean isRowFixed) {
        int min = Math.min(start, end);
        int max = Math.max(start, end);

        for (int i = min + 1; i < max; i++) {
            if (!(isRowFixed ? board[fixed][i] : board[i][fixed]).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidHorseMove(String[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);

        if (rowDiff == 2 && colDiff == 1) {
            return board[(fromRow + toRow) / 2][fromCol].isEmpty();
        } else if (rowDiff == 1 && colDiff == 2) {
            return board[fromRow][(fromCol + toCol) / 2].isEmpty();
        }
        return false;
    }

    private static boolean isValidElephantMove(String[][] board, int fromRow, int fromCol, int toRow, int toCol, String piece) {
        // Must move exactly 2 diagonally
        if (Math.abs(toRow - fromRow) != 2 || Math.abs(toCol - fromCol) != 2) {
            return false;
        }

        // Cannot cross the river
        boolean isRed = Character.isUpperCase(piece.charAt(0));
        if ((isRed && toRow < 5) || (!isRed && toRow > 4)) {
            return false;
        }

        // Midpoint check
        int midRow = (fromRow + toRow) / 2;
        int midCol = (fromCol + toCol) / 2;
        if (!board[midRow][midCol].isEmpty()) {
            return false; // Jumping over a piece is not allowed
        }

        return true;
    }

    private static boolean isValidAdvisorMove(int fromRow, int fromCol, int toRow, int toCol, String piece) {
        // Must move exactly 1 diagonally
        if (Math.abs(toRow - fromRow) != 1 || Math.abs(toCol - fromCol) != 1) {
            return false;
        }

        // Must stay inside the palace
        if (toCol < 3 || toCol > 5) {
            return false;
        }

        boolean isRed = Character.isUpperCase(piece.charAt(0));
        if ((isRed && (toRow < 7 || toRow > 9)) || (!isRed && (toRow < 0 || toRow > 2))) {
            return false;
        }

        return true;
    }

    private static boolean isValidKingMove(int fromRow, int fromCol, int toRow, int toCol, String piece) {
        // Must move exactly 1 step (vertically or horizontally)
        if ((Math.abs(toRow - fromRow) + Math.abs(toCol - fromCol)) != 1) {
            return false;
        }

        // Must stay inside the palace
        if (toCol < 3 || toCol > 5) {
            return false;
        }

        boolean isRed = Character.isUpperCase(piece.charAt(0));
        if ((isRed && (toRow < 7 || toRow > 9)) || (!isRed && (toRow < 0 || toRow > 2))) {
            return false;
        }

        return true;
    }

    private static boolean isValidCannonMove(String[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        boolean isCapture = !board[toRow][toCol].isEmpty();
        int count = countPiecesBetween(board, fromRow, fromCol, toRow, toCol);

        return (isCapture && count == 1) || (!isCapture && count == 0);
    }

    private static int countPiecesBetween(String[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        int count = 0;
        if (fromRow == toRow) {
            for (int i = Math.min(fromCol, toCol) + 1; i < Math.max(fromCol, toCol); i++) {
                if (!board[fromRow][i].isEmpty()) count++;
            }
        } else if (fromCol == toCol) {
            for (int i = Math.min(fromRow, toRow) + 1; i < Math.max(fromRow, toRow); i++) {
                if (!board[i][fromCol].isEmpty()) count++;
            }
        }
        return count;
    }

    private static boolean areKingsFacing(String[][] board) {
        int redKingRow = -1, redKingCol = -1;
        int blackKingRow = -1, blackKingCol = -1;

        // Find the positions of the two kings
        for (int row = 0; row < 10; row++) {
            for (int col = 3; col <= 5; col++) { // Kings are always in these columns
                if (board[row][col].equals("K")) {
                    redKingRow = row;
                    redKingCol = col;
                } else if (board[row][col].equals("k")) {
                    blackKingRow = row;
                    blackKingCol = col;
                }
            }
        }

        // Ensure both kings were found
        if (redKingRow == -1 || blackKingRow == -1) {
            return false;
        }

        // Kings must be in the same column
        if (redKingCol != blackKingCol) {
            return false;
        }

        // Check if there are any pieces between them
        for (int row = redKingRow - 1; row > blackKingRow; row--) {
            if (!board[row][redKingCol].equals("")) { // If there's a piece between them
                return false;
            }
        }

        // If no piece is blocking, kings are facing each other (illegal move)
        return true;
    }

}
