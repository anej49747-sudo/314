package csit314.renovation.company.dto;

import csit314.renovation.company.entity.DesignerCompany;

public record CompanyInformationResponse(String companyName, String address, String description) {
    public static CompanyInformationResponse empty() {
        return new CompanyInformationResponse("", "", "");
    }

    public static CompanyInformationResponse from(DesignerCompany company) {
        return new CompanyInformationResponse(company.getCompanyName(),
                company.getAddress() == null ? "" : company.getAddress(),
                company.getDescription() == null ? "" : company.getDescription());
    }
}
