package csit314.renovation.moodboard.dto;

import java.util.List;

public record MoodboardPhotoResponse(Long projectId, String projectTitle, List<Photo> photos) {
    public record Photo(Long id, String label, String url) {}
}
