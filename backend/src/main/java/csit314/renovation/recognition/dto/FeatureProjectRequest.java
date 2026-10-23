package csit314.renovation.recognition.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FeatureProjectRequest(@NotNull @Positive Long projectId) { }
