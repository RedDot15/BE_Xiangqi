package com.example.xiangqi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayerResponse implements Comparable {
	Long id;

	String username;

	String role;

	Integer rating;

	@Override
	public int compareTo(Object o) {
		return id.compareTo(((PlayerResponse) o).getId());
	}
}
