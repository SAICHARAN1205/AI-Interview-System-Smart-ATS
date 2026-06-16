package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.ProfileDTO;
import com.aihiringplatform.backend.entity.CandidateProfile;
import com.aihiringplatform.backend.entity.RecruiterProfile;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.CandidateProfileRepository;
import com.aihiringplatform.backend.repository.RecruiterProfileRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Transactional(readOnly = true)
    public ProfileDTO getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ProfileDTO.ProfileDTOBuilder builder = ProfileDTO.builder()
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name());

        if (user.getRole() == Role.CANDIDATE) {
            candidateProfileRepository.findByUserEmail(email).ifPresent(profile -> {
                builder.phone(profile.getPhone())
                        .skills(profile.getSkills())
                        .bio(profile.getBio())
                        .education(profile.getEducation())
                        .projects(profile.getProjects());
            });
        } else if (user.getRole() == Role.RECRUITER) {
            recruiterProfileRepository.findByUserEmail(email).ifPresent(profile -> {
                builder.companyName(profile.getCompanyName())
                        .designation(profile.getDesignation())
                        .companyWebsite(profile.getCompanyWebsite())
                        .linkedInProfile(profile.getLinkedInProfile())
                        .hiringDepartment(profile.getHiringDepartment())
                        .companyDescription(profile.getCompanyDescription())
                        .companyLocation(profile.getCompanyLocation())
                        .companyLogoPath(profile.getCompanyLogoPath())
                        .contactNumber(profile.getContactNumber());
            });
        }

        return builder.build();
    }

    @Transactional
    public ProfileDTO updateCandidateProfile(String email, ProfileDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != Role.CANDIDATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only candidates can update candidate profile");
        }

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            user.setName(dto.getName().trim());
            userRepository.save(user);
        }

        CandidateProfile profile = candidateProfileRepository.findByUserEmail(email)
                .orElse(new CandidateProfile());

        profile.setUser(user);
        profile.setPhone(dto.getPhone());
        profile.setSkills(dto.getSkills());
        profile.setBio(dto.getBio());
        profile.setEducation(dto.getEducation());
        profile.setProjects(dto.getProjects());

        candidateProfileRepository.save(profile);

        return getProfile(email);
    }

    @Transactional
    public ProfileDTO updateRecruiterProfile(String email, ProfileDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can update recruiter profile");
        }

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            user.setName(dto.getName().trim());
            userRepository.save(user);
        }

        RecruiterProfile profile = recruiterProfileRepository.findByUserEmail(email)
                .orElse(new RecruiterProfile());

        profile.setUser(user);
        profile.setCompanyName(dto.getCompanyName());
        profile.setDesignation(dto.getDesignation());
        profile.setCompanyWebsite(dto.getCompanyWebsite());
        profile.setLinkedInProfile(dto.getLinkedInProfile());
        profile.setHiringDepartment(dto.getHiringDepartment());
        profile.setCompanyDescription(dto.getCompanyDescription());
        profile.setCompanyLocation(dto.getCompanyLocation());
        profile.setCompanyLogoPath(dto.getCompanyLogoPath());
        profile.setContactNumber(dto.getContactNumber());

        recruiterProfileRepository.save(profile);

        return getProfile(email);
    }
}
