package ua.com.bravi.bravi.identity.controller.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserResponse(
        String type,
        @JsonProperty("first_name")
        String firstName,
        @JsonProperty("last_name")
        String lastName,
        String email,
        String status
){}
