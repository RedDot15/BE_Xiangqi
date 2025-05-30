package com.example.xiangqi.util;

import com.example.xiangqi.dto.model.Position;
import com.example.xiangqi.dto.request.MoveRequest;

public class MoveValidator {
    public static boolean hasLegalMoves(String[][] board, boolean isRedPlayer) {
        for (int fromRow = 0; fromRow < 10; fromRow++) {
            for (int fromCol = 0; fromCol < 9; fromCol++) {
                String piece = board[fromRow][fromCol];
                if (!piece.isEmpty() && (isRedPlayer ? isRedPiece(piece) : isBlackPiece(piece))) {
                    // Try every possible destination
                    for (int toRow = 0; toRow < 10; toRow++) {
                        for (int toCol = 0; toCol < 9; toCol++) {
                            // Define move request
                            MoveRequest request = MoveRequest.builder()
                                    .from(new Position(fromRow, fromCol))
                                    .to(new Position(toRow, toCol))
                                    .build();
                            // Check if the move is valid
                            if (isValidMove(board, request)) {
                                return true; // Found at least one legal move
                            }
                        }
                    }
                }
            }
        }
        return false; // No legal moves found
    }

    public static boolean isValidMove(String[][] board, MoveRequest moveRequest) {
        // Get move request detail
        int fromRow = moveRequest.getFrom().getRow();
        int fromCol = moveRequest.getFrom().getCol();
        int toRow = moveRequest.getTo().getRow();
        int toCol = moveRequest.getTo().getCol();
        // Get moved piece
        String movedPiece = board[fromRow][fromCol];

        // Check if the piece exists
        if (movedPiece.isEmpty()) {
            return false;
        }

        // Check if the piece move out of board
        if (isOutOfBoard(moveRequest)){
            return false;
        }

        // Ensure the destination is not occupied by the player's own piece
        if (!board[toRow][toCol].isEmpty() && isSameColor(movedPiece, board[toRow][toCol])) {
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
        // Check if the move puts the player's own king in check
        String allyColor = isRedPiece(movedPiece) ? "red" : "black";
        if (isKingInCheck(tempBoard, allyColor)) {
            return false; // Move is invalid if it puts own king in check
        }

        // Validate move based on the piece type
        return switch (Character.toLowerCase(movedPiece.charAt(0))) {
            case 'r' -> isValidRookMove(board, moveRequest);
            case 'h' -> isValidHorseMove(board, moveRequest);
            case 'c' -> isValidCannonMove(board, moveRequest);
            case 'e' -> isValidElephantMove(board, moveRequest);
            case 'a' -> isValidAdvisorMove(moveRequest, movedPiece);
            case 'k' -> isValidKingMove(moveRequest, movedPiece);
            case 'p' -> isValidPawnMove(moveRequest, movedPiece);
            default -> false;
        };
    }

    private static boolean isOutOfBoard(MoveRequest moveRequest) {
        // Get move request detail
        int fromRow = moveRequest.getFrom().getRow();
        int fromCol = moveRequest.getFrom().getCol();
        int toRow = moveRequest.getTo().getRow();
        int toCol = moveRequest.getTo().getCol();
        // Out of range
        if (fromRow < 0 || fromCol > 9 || toRow < 0 || toRow > 9)
            return true;
        if (fromCol < 0 || fromCol > 8 || toCol < 0 || toCol > 8)
            return true;

        return false;
    }

    private static boolean isSameColor(String piece1, String piece2) {
        return (Character.isUpperCase(piece1.charAt(0)) && Character.isUpperCase(piece2.charAt(0))) ||
                (Character.isLowerCase(piece1.charAt(0)) && Character.isLowerCase(piece2.charAt(0)));
    }

    private static boolean isValidRookMove(String[][] board, MoveRequest moveRequest) {
        // Get move request detail
        int fromRow = moveRequest.getFrom().getRow();
        int fromCol = moveRequest.getFrom().getCol();
        int toRow = moveRequest.getTo().getRow();
        int toCol = moveRequest.getTo().getCol();

        // Check move valid:
        if (fromRow == toRow) {
            return isPathClear(board, moveRequest, true);
        } else if (fromCol == toCol) {
            return isPathClear(board, moveRequest, false);
        }
        return false;
    }

    private static boolean isPathClear(String[][] board, MoveRequest request, boolean isRowFixed) {
        // Get move request detail
        int fromRow = request.getFrom().getRow();
        int fromCol = request.getFrom().getCol();
        int toRow = request.getTo().getRow();
        int toCol = request.getTo().getCol();
        // Simplify move request
        int fixed = isRowFixed ? fromRow : fromCol;
        int start = isRowFixed ? fromCol : fromRow;
        int end = isRowFixed ? toCol : toRow;

        // Check move valid
        int min = Math.min(start, end);
        int max = Math.max(start, end);

        for (int i = min + 1; i < max; i++) {
            if (!(isRowFixed ? board[fixed][i] : board[i][fixed]).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidHorseMove(String[][] board, MoveRequest moveRequest) {
        // Get move request detail
        int fromRow = moveRequest.getFrom().getRow();
        int fromCol = moveRequest.getFrom().getCol();
        int toRow = moveRequest.getTo().getRow();
        int toCol = moveRequest.getTo().getCol();

        // Check move valid:
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);

        if (rowDiff == 2 && colDiff == 1) {
            return board[(fromRow + toRow) / 2][fromCol].isEmpty();
        } else if (rowDiff == 1 && colDiff == 2) {
            return board[fromRow][(fromCol + toCol) / 2].isEmpty();
        }
        return false;
    }

    private static boolean isValidCannonMove(String[][] board, MoveRequest moveRequest) {
        // Get move request detail
        int fromRow = moveRequest.getFrom().getRow();
        int fromCol = moveRequest.getFrom().getCol();
        int toRow = moveRequest.getTo().getRow();
        int toCol = moveRequest.getTo().getCol();
        // Check move valid
        boolean isCapture = !board[toRow][toCol].isEmpty();
        int count = countPiecesBetween(board, moveRequest);

        // Check if move is horizontal or vertical
        if (fromRow != toRow && fromCol != toCol) {
            return false; // Diagonal moves are invalid
        }

        return (isCapture && count == 1) || (!isCapture && count == 0);
    }

    private static boolean isValidElephantMove(String[][] board, MoveRequest moveRequest) {
        // Get move request detail
        int fromRow = moveRequest.getFrom().getRow();
        int fromCol = moveRequest.getFrom().getCol();
        int toRow = moveRequest.getTo().getRow();
        int toCol = moveRequest.getTo().getCol();
        // Get moved piece
        String piece = board[fromRow][fromCol];

        // Check move valid:
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
        // Jumping over a piece is not allowed
        return board[midRow][midCol].isEmpty();
    }

    private static boolean isValidAdvisorMove(MoveRequest moveRequest, String piece) {
        // Get move request detail
        int fromRow = moveRequest.getFrom().getRow();
        int fromCol = moveRequest.getFrom().getCol();
        int toRow = moveRequest.getTo().getRow();
        int toCol = moveRequest.getTo().getCol();

        // Check move valid:
        // Must move exactly 1 diagonally
        if (Math.abs(toRow - fromRow) != 1 || Math.abs(toCol - fromCol) != 1) {
            return false;
        }

        // Must stay inside the palace
        if (toCol < 3 || toCol > 5) {
            return false;
        }

        boolean isRed = Character.isUpperCase(piece.charAt(0));
        return (!isRed || (toRow >= 7 && toRow <= 9)) && (isRed || (toRow >= 0 && toRow <= 2));
    }

    private static boolean isValidKingMove(MoveRequest moveRequest, String piece) {
        // Get move request detail
        int fromRow = moveRequest.getFrom().getRow();
        int fromCol = moveRequest.getFrom().getCol();
        int toRow = moveRequest.getTo().getRow();
        int toCol = moveRequest.getTo().getCol();

        // Check move valid:
        // Must move exactly 1 step (vertically or horizontally)
        if ((Math.abs(toRow - fromRow) + Math.abs(toCol - fromCol)) != 1) {
            return false;
        }

        // Must stay inside the palace
        if (toCol < 3 || toCol > 5) {
            return false;
        }

        boolean isRed = Character.isUpperCase(piece.charAt(0));
        return (!isRed || (toRow >= 7 && toRow <= 9)) && (isRed || (toRow >= 0 && toRow <= 2));
    }

    private static int countPiecesBetween(String[][] board, MoveRequest moveRequest) {
        // Get move request detail
        int fromRow = moveRequest.getFrom().getRow();
        int fromCol = moveRequest.getFrom().getCol();
        int toRow = moveRequest.getTo().getRow();
        int toCol = moveRequest.getTo().getCol();
        // Counting
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

    private static boolean isValidPawnMove(MoveRequest moveRequest, String piece) {
        // Get move request detail
        int fromRow = moveRequest.getFrom().getRow();
        int fromCol = moveRequest.getFrom().getCol();
        int toRow = moveRequest.getTo().getRow();
        int toCol = moveRequest.getTo().getCol();

        // Check move valid:
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
            if (!board[row][redKingCol].isEmpty()) { // If there's a piece between them
                return false;
            }
        }

        // If no piece is blocking, kings are facing each other (illegal move)
        return true;
    }

    private static boolean isKingInCheck(String[][] board, String allyColor) {
        // Find the allied king's position
        int kingRow = -1;
        int kingCol = -1;
        for (int row = 0; row < 10; row++) {
            for (int col = 3; col <= 5; col++) {
                String piece = board[row][col];
                if (!piece.isEmpty() && Character.toLowerCase(piece.charAt(0)) == 'k' &&
                        (allyColor.equals("red") ? isRedPiece(piece) : isBlackPiece(piece))) {
                    kingRow = row;
                    kingCol = col;
                    break;
                }
            }
            if (kingRow != -1) break;
        }

        if (kingRow == -1) return false; // King not found (shouldn't happen in valid game)

        // Check if any enemy piece can move to the king's position
        String enemyColor = allyColor.equals("red") ? "black" : "red";
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 9; col++) {
                // Get moved piece
                String movedPiece = board[row][col];
                // Define move request
                MoveRequest moveRequest = MoveRequest.builder()
                        .from(new Position(row, col))
                        .to(new Position(kingRow, kingCol))
                        .build();
                if (!movedPiece.isEmpty() && (enemyColor.equals("red") ? isRedPiece(movedPiece) : isBlackPiece(movedPiece))) {
                    switch (Character.toLowerCase(movedPiece.charAt(0))) {
                        case 'r':
                            if (isValidRookMove(board, moveRequest)) return true;
                            break;
                        case 'h':
                            if (isValidHorseMove(board, moveRequest)) return true;
                            break;
                        case 'c':
                            if (isValidCannonMove(board, moveRequest)) return true;
                            break;
                        case 'e':
                            if (isValidElephantMove(board, moveRequest)) return true;
                            break;
                        case 'a':
                            if (isValidAdvisorMove(moveRequest, movedPiece)) return true;
                            break;
                        case 'k':
                            if (isValidKingMove(moveRequest, movedPiece)) return true;
                            break;
                        case 'p':
                            if (isValidPawnMove(moveRequest, movedPiece)) return true;
                            break;
                    }
                }
            }
        }

        return false;
    }

    // Helper methods to determine piece color
    private static boolean isRedPiece(String piece) {
        return !piece.isEmpty() && Character.isUpperCase(piece.charAt(0));
    }

    private static boolean isBlackPiece(String piece) {
        return !piece.isEmpty() && Character.isLowerCase(piece.charAt(0));
    }
}
