package com.example.xiangqi.dto.request;

import com.example.xiangqi.validation.annotation.Match;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Match(
		fields = {"newPassword", "confirmPassword"},
		message = "These fields {fields} must match.")
public class ChangePasswordRequest {
	@NotBlank(message = "Old password is required.")
	String oldPassword;

	@NotBlank(message = "Password is required.")
	@Pattern(
			regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&-+=()])(?=\\S+$).{8,20}$",
			message = "Password must contains at least 8 characters and at most 20 characters."
					+ "Password must contains at least one digit."
					+ "Password must contains at least one upper case alphabet."
					+ "Password must contains at least one lower case alphabet."
					+ "Password must contains at least one special character which includes !@#$%&*()-+=^."
					+ "Password must not contain any white space.")
	String newPassword;

	@NotBlank(message = "Confirm password is required.")
	String confirmPassword;
}
