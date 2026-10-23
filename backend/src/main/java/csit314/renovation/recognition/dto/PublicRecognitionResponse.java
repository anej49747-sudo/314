package csit314.renovation.recognition.dto;

import java.util.List;

public record PublicRecognitionResponse(FeaturedProjectResponse featuredProject,
        List<AwardResponse> awards) { }
