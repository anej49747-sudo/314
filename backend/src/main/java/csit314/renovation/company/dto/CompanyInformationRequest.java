package csit314.renovation.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyInformationRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 150, message = "Company name must be 150 characters or fewer")
        String companyName,
        @Size(max = 300, message = "Company address must be 300 characters or fewer")
        String address,
        @Size(max = 3000, message = "Company description must be 3000 characters or fewer")
        String description) {}
