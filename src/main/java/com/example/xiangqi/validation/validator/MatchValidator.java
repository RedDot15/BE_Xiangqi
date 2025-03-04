package com.example.xiangqi.validation.validator;

import com.example.xiangqi.validation.annotation.Match;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class MatchValidator implements ConstraintValidator<Match, Object> {
	private static final SpelExpressionParser PARSER = new SpelExpressionParser();
	private String[] fields;

	@Override
	public void initialize(Match constraintAnnotation) {
		fields = constraintAnnotation.fields();
	}

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		long notNull = Stream.of(fields)
				.map(field -> PARSER.parseExpression(field).getValue(value))
				.filter(Objects::nonNull)
				.count();
		// Return true if exist null value in the list
		if (notNull != fields.length) return true;
		// Return true if all value match
		List<String> stringList = Stream.of(fields)
				.map(field -> Objects.requireNonNull(
								PARSER.parseExpression(field).getValue(value))
						.toString())
				.toList();
		return stringList.stream().allMatch(s -> s.equals(stringList.get(0)));
	}
}
