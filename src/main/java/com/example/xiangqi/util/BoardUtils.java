package com.example.xiangqi.util;

import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BoardUtils {
    private static final String[][] INITIAL_BOARD = {
            {"R", "H", "E", "A", "K", "A", "E", "H", "R"},
            {"", "", "", "", "", "", "", "", ""},
            {"", "C", "", "", "", "", "", "C", ""},
            {"P", "", "P", "", "P", "", "P", "", "P"},
            {"", "", "", "", "", "", "", "", ""},
            {"", "", "", "", "", "", "", "", ""},
            {"p", "", "p", "", "p", "", "p", "", "p"},
            {"", "c", "", "", "", "", "", "c", ""},
            {"", "", "", "", "", "", "", "", ""},
            {"r", "h", "e", "a", "k", "a", "e", "h", "r"}
    };

    public static String getInitialBoardState() {
        // Represent the initial chessboard as a JSON string
        return boardSerialize(INITIAL_BOARD);
    }

    public static String boardSerialize(String[][] boardState) {
        // Represent the initial chessboard as a JSON string
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(boardState);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.BOARD_STATE_SERIALIZED_FAIL);
        }
    }

    public static String[][] boardParse(String boardStateJson) {
        // Represent the initial chessboard as a JSON string
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(boardStateJson, String[][].class);
        } catch (Exception e) {
            throw new AppException(ErrorCode.BOARD_STATE_PARSING_FAIL);
        }
    }
}
