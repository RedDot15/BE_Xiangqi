package com.example.xiangqi.service.my_sql;

import com.example.xiangqi.dto.response.*;
import com.example.xiangqi.dto.response.PageResponse;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Service
public class HistoryService {
	MatchService matchService;

	public PageResponse<HistoryResponse> getAllByUserId(int page, int size, Long userId) {
		// Get all finished match by user ID
		PageResponse<MatchResponse> matchResponsePage = matchService.getAllFinished(page, size, userId);

		// Create a list to hold MatchResponse objects
		List<HistoryResponse> historyResponseList = new ArrayList<>();
		// Mapping match -> history
		for (MatchResponse matchResponse : matchResponsePage.getContent()) {
			// Get player's faction
			boolean isRedPlayer = userId.equals(matchResponse.getRedPlayerResponse().getId());
			// Get result
			boolean isRedWin = matchResponse.getResult().equals("Red Player Win");
			// Define match response
			HistoryResponse historyResponse = HistoryResponse.builder()
					.id(matchResponse.getId())
					.opponentUsername(isRedPlayer
							? matchResponse.getBlackPlayerResponse().getUsername()
							: matchResponse.getRedPlayerResponse().getUsername())
					.result(isRedPlayer
							? (isRedWin ? "Win" : "Lose")
							: (isRedWin ? "Lose" : "Win"))
					.startTime(matchResponse.getStartTime())
					.endTime(matchResponse.getEndTime())
					.build();
			// Add to response page
			historyResponseList.add(historyResponse);
		}
		// Return
		return new PageResponse<>(
				historyResponseList,
				PageRequest.of(matchResponsePage.getPage(), matchResponsePage.getSize()),
				matchResponsePage.getTotalElements());
	}
}
