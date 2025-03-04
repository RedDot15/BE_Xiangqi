package com.example.xiangqi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationResponse {
	List<String> fields;
	String field;
	String message;

	public ValidationResponse(List<String> fields, String message) {
		this.fields = fields;
		this.message = message;
	}

	public ValidationResponse(String field, String message) {
		this.field = field;
		this.message = message;
	}
}
