package com.aihiringplatform.backend.dto;

public class ProfileDTO {
    // Common fields
    private String name;
    private String email;
    private String role;

    // Candidate fields
    private String phone;
    private String skills;
    private String bio;
    private String education;
    private String projects;

    // Recruiter fields
    private String companyName;
    private String designation;
    private String companyWebsite;
    private String linkedInProfile;
    private String hiringDepartment;
    private String companyDescription;
    private String companyLocation;
    private String companyLogoPath;
    private String contactNumber;

    public ProfileDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getProjects() { return projects; }
    public void setProjects(String projects) { this.projects = projects; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getCompanyWebsite() { return companyWebsite; }
    public void setCompanyWebsite(String companyWebsite) { this.companyWebsite = companyWebsite; }
    public String getLinkedInProfile() { return linkedInProfile; }
    public void setLinkedInProfile(String linkedInProfile) { this.linkedInProfile = linkedInProfile; }
    public String getHiringDepartment() { return hiringDepartment; }
    public void setHiringDepartment(String hiringDepartment) { this.hiringDepartment = hiringDepartment; }
    public String getCompanyDescription() { return companyDescription; }
    public void setCompanyDescription(String companyDescription) { this.companyDescription = companyDescription; }
    public String getCompanyLocation() { return companyLocation; }
    public void setCompanyLocation(String companyLocation) { this.companyLocation = companyLocation; }
    public String getCompanyLogoPath() { return companyLogoPath; }
    public void setCompanyLogoPath(String companyLogoPath) { this.companyLogoPath = companyLogoPath; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public static ProfileDTOBuilder builder() {
        return new ProfileDTOBuilder();
    }

    public static class ProfileDTOBuilder {
        private final ProfileDTO dto = new ProfileDTO();

        public ProfileDTOBuilder name(String name) { dto.setName(name); return this; }
        public ProfileDTOBuilder email(String email) { dto.setEmail(email); return this; }
        public ProfileDTOBuilder role(String role) { dto.setRole(role); return this; }
        public ProfileDTOBuilder phone(String phone) { dto.setPhone(phone); return this; }
        public ProfileDTOBuilder skills(String skills) { dto.setSkills(skills); return this; }
        public ProfileDTOBuilder bio(String bio) { dto.setBio(bio); return this; }
        public ProfileDTOBuilder education(String education) { dto.setEducation(education); return this; }
        public ProfileDTOBuilder projects(String projects) { dto.setProjects(projects); return this; }
        public ProfileDTOBuilder companyName(String companyName) { dto.setCompanyName(companyName); return this; }
        public ProfileDTOBuilder designation(String designation) { dto.setDesignation(designation); return this; }
        public ProfileDTOBuilder companyWebsite(String companyWebsite) { dto.setCompanyWebsite(companyWebsite); return this; }
        public ProfileDTOBuilder linkedInProfile(String linkedInProfile) { dto.setLinkedInProfile(linkedInProfile); return this; }
        public ProfileDTOBuilder hiringDepartment(String hiringDepartment) { dto.setHiringDepartment(hiringDepartment); return this; }
        public ProfileDTOBuilder companyDescription(String companyDescription) { dto.setCompanyDescription(companyDescription); return this; }
        public ProfileDTOBuilder companyLocation(String companyLocation) { dto.setCompanyLocation(companyLocation); return this; }
        public ProfileDTOBuilder companyLogoPath(String companyLogoPath) { dto.setCompanyLogoPath(companyLogoPath); return this; }
        public ProfileDTOBuilder contactNumber(String contactNumber) { dto.setContactNumber(contactNumber); return this; }

        public ProfileDTO build() {
            return dto;
        }
    }
}
