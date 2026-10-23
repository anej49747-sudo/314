package csit314.renovation.recognition.dto;

import csit314.renovation.recognition.entity.DesignerAward;
import java.time.Instant;

public record AwardResponse(Long id, Long designerId, String title, Instant awardedAt) {
    public static AwardResponse from(DesignerAward award) {
        return new AwardResponse(award.getId(), award.getDesigner().getId(),
                award.getTitle(), award.getAwardedAt());
    }
}
