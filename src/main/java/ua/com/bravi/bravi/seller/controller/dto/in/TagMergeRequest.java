package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** The tags to fold into the one addressed by the path; they are deleted afterwards. */
public record TagMergeRequest(
        @JsonProperty("source_ids")
        @NotEmpty
        @Size(max = 50)
        List<String> sourceIds
) {
}
