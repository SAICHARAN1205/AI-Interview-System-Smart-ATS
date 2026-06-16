(function () {
    const PROFILE_KEY = "ai_hiring_platform_profile";

    function byId(id) {
        return document.getElementById(id);
    }

    function setMessage(element, message, type) {
        if (!element) {
            return;
        }

        element.textContent = message;
        element.className = `ln-msg show ${type}`;
    }

    function clearMessage(element) {
        if (!element) {
            return;
        }

        element.textContent = "";
        element.className = "ln-msg";
    }

    function setButtonLoading(button, text, isLoading) {
        if (!button) {
            return;
        }

        if (!button.dataset.defaultText) {
            button.dataset.defaultText = button.textContent;
        }

        button.disabled = isLoading;
        button.textContent = isLoading ? text : button.dataset.defaultText;
    }

    async function loadProfile() {
        try {
            return await window.api.get("/api/profiles/me");
        } catch (error) {
            console.error("Failed to load profile from backend", error);
            return {};
        }
    }

    async function saveProfile(data, role) {
        try {
            if (role === "RECRUITER") {
                return await window.api.put("/api/profiles/recruiter", data);
            } else {
                return await window.api.put("/api/profiles/candidate", data);
            }
        } catch (error) {
            throw error;
        }
    }

    function getInitial(name, email) {
        if (name && name.trim()) {
            return name.trim().charAt(0).toUpperCase();
        }

        if (email) {
            return email.charAt(0).toUpperCase();
        }

        return "U";
    }

    function loadPreferences() {
        const prefs = JSON.parse(localStorage.getItem("ai_hiring_platform_preferences") || "{}");

        if (prefs.emailNotifications !== undefined && byId("settings-email-notifications")) {
            byId("settings-email-notifications").checked = prefs.emailNotifications;
        }

        if (prefs.marketingEmails !== undefined && byId("settings-marketing-emails")) {
            byId("settings-marketing-emails").checked = prefs.marketingEmails;
        }

        const isDarkMode = localStorage.getItem("smartats-theme") === "dark";
        if (byId("settings-dark-mode")) {
            byId("settings-dark-mode").checked = isDarkMode;
        }
    }

    async function renderResumeState() {
        const resumeDisplayArea = byId("resume-display-area");
        const resumeFileNameEl = byId("resume-file-name");
        const resumeStatusText = byId("resume-status-text");
        const resumeEmptyState = byId("resume-empty-state");
        const btnViewResume = byId("btn-view-resume");
        const btnDownloadResume = byId("btn-download-resume");

        try {
            const resume = window.api.refreshProfileResumeState ? await window.api.refreshProfileResumeState() : await window.api.get("/api/resumes/me");

            if (!resume || !resume.hasResume) {
                throw new Error("Resume not uploaded");
            }

            if (resumeDisplayArea) {
                resumeDisplayArea.style.display = "flex";
            }
            if (resumeEmptyState) {
                resumeEmptyState.style.display = "none";
            }
            if (resumeFileNameEl) {
                resumeFileNameEl.textContent = resume.fileName || "Resume";
            }
            if (resumeStatusText) {
                const uploadedAt = resume.uploadedAt ? new Date(resume.uploadedAt).toLocaleString() : "";
                resumeStatusText.textContent = uploadedAt ? `Uploaded ${uploadedAt}` : "Uploaded successfully";
            }

            if (btnViewResume) {
                btnViewResume.onclick = async function () {
                    try {
                        const file = await window.api.download("/api/resumes/me/download");
                        const url = window.URL.createObjectURL(file.blob);
                        window.open(url, "_blank");
                        window.setTimeout(function () {
                            window.URL.revokeObjectURL(url);
                        }, 1000);
                    } catch (error) {
                        if (window.showToast) {
                            window.showToast(error.message || "Resume unavailable", "error");
                        }
                    }
                };
            }

            if (btnDownloadResume) {
                btnDownloadResume.onclick = async function () {
                    try {
                        const file = await window.api.download("/api/resumes/me/download");
                        const link = document.createElement("a");
                        const url = window.URL.createObjectURL(file.blob);
                        link.href = url;
                        link.download = file.fileName || "resume";
                        document.body.appendChild(link);
                        link.click();
                        document.body.removeChild(link);
                        window.setTimeout(function () {
                            window.URL.revokeObjectURL(url);
                        }, 1000);
                    } catch (error) {
                        if (window.showToast) {
                            window.showToast(error.message || "Resume unavailable", "error");
                        }
                    }
                };
            }
        } catch (error) {
            if (resumeDisplayArea) {
                resumeDisplayArea.style.display = "none";
            }
            if (resumeEmptyState) {
                resumeEmptyState.style.display = "block";
                resumeEmptyState.textContent = "Resume not uploaded";
            }
        }
    }

    async function uploadResumeIfNeeded() {
        const resumeInput = byId("profile-resume");
        const file = resumeInput?.files?.[0];

        if (!file) {
            return false;
        }

        const formData = new FormData();
        formData.append("file", file);
        await window.api.upload("/api/resumes/upload", formData);
        resumeInput.value = "";
        await renderResumeState();
        return true;
    }

    function savePreferences() {
        const prefs = {
            emailNotifications: byId("settings-email-notifications") ? byId("settings-email-notifications").checked : false,
            marketingEmails: byId("settings-marketing-emails") ? byId("settings-marketing-emails").checked : false,
        };

        localStorage.setItem("ai_hiring_platform_preferences", JSON.stringify(prefs));

        // Save theme logic
        if (byId("settings-dark-mode")) {
            const isDark = byId("settings-dark-mode").checked;
            localStorage.setItem("smartats-theme", isDark ? "dark" : "light");
            if (isDark) {
                document.documentElement.classList.add("dark-theme");
            } else {
                document.documentElement.classList.remove("dark-theme");
            }
        }
    }

    function getResumeUploadErrorMessage(error) {
        if (window.api && typeof window.api.normalizeResumeUploadErrorMessage === "function") {
            return window.api.normalizeResumeUploadErrorMessage(error);
        }

        return error && error.message ? error.message : "Unable to upload the resume.";
    }

    async function handleSettingsSubmit(event, role, email) {
        event.preventDefault();

        const currentPassword = byId("settings-current-password")?.value || "";
        const newPassword = byId("settings-new-password")?.value || "";
        const confirmPassword = byId("settings-confirm-password")?.value || "";
        const msgBox = byId("settings-message");
        const button = byId("settings-save-btn");

        clearMessage(msgBox);
        setButtonLoading(button, "Saving...", true);

        try {
            if (currentPassword || newPassword || confirmPassword) {
                if (!currentPassword) throw new Error("Please enter your current password.");
                if (!newPassword) throw new Error("Please enter a new password.");
                
                const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;
                if (!passwordRegex.test(newPassword)) {
                    throw new Error("Password must be at least 8 characters with uppercase, lowercase, number, and special character.");
                }
                if (newPassword !== confirmPassword) throw new Error("New passwords do not match.");

                await window.api.put("/api/users/password", { currentPassword, newPassword });
                byId("settings-current-password").value = "";
                byId("settings-new-password").value = "";
                byId("settings-confirm-password").value = "";
                if (window.showToast) {
                    window.showToast("Password updated successfully.", "success");
                }
            }

            let profileData = {
                name: byId("profile-name")?.value.trim() || ""
            };

            if (role === "RECRUITER") {
                profileData.companyName = byId("recruiter-company")?.value.trim() || "";
                profileData.designation = byId("recruiter-designation")?.value.trim() || "";
                profileData.companyWebsite = byId("recruiter-website")?.value.trim() || "";
                profileData.linkedInProfile = byId("recruiter-linkedin")?.value.trim() || "";
                profileData.hiringDepartment = byId("recruiter-department")?.value.trim() || "";
                profileData.contactNumber = byId("recruiter-contact")?.value.trim() || "";
                profileData.companyLocation = byId("recruiter-location")?.value.trim() || "";
                profileData.companyDescription = byId("recruiter-description")?.value.trim() || "";
            } else {
                profileData.phone = byId("profile-phone")?.value.trim() || "";
                profileData.skills = byId("profile-skills")?.value.trim() || "";
                profileData.bio = byId("profile-bio")?.value.trim() || "";
                profileData.education = byId("profile-education")?.value.trim() || "";
                profileData.projects = byId("profile-projects")?.value.trim() || "";
            }

            const updatedProfile = await saveProfile(profileData, role);
            savePreferences();

            if (role !== "RECRUITER") {
                try {
                    await uploadResumeIfNeeded();
                } catch (error) {
                    error.isResumeUploadError = true;
                    throw error;
                }
            }

            const avatarEl = byId("profile-avatar");
            if (avatarEl) {
                avatarEl.textContent = getInitial(updatedProfile.name, email);
            }

            window.dispatchEvent(new Event("profilechange"));
            setMessage(msgBox, "Settings saved successfully.", "success");
            if (window.showToast) {
                window.showToast("Settings saved successfully.", "success");
            }
        } catch (error) {
            setMessage(
                msgBox,
                error && error.isResumeUploadError
                    ? getResumeUploadErrorMessage(error)
                    : error.message || "Unable to save settings.",
                "error"
            );
        } finally {
            setButtonLoading(button, "Save Settings", false);
        }
    }

    document.addEventListener("DOMContentLoaded", async function () {
        if (document.body.dataset.page !== "settings") {
            return;
        }

        const session = window.authApp ? window.authApp.ensureAuthenticated() : null;
        if (!session) {
            return;
        }

        const email = window.api.getUser() || "";
        const role = (window.api.getRole() || "").toUpperCase();

        const msgBox = byId("settings-message");
        if (msgBox) {
            msgBox.textContent = "Loading profile...";
            msgBox.className = "ln-msg show neutral";
        }

        const profile = await loadProfile();
        clearMessage(msgBox);

        if (role === "RECRUITER") {
            const backBtn = byId("settings-back-btn");
            if (backBtn) {
                backBtn.href = "recruiter.html";
                backBtn.textContent = "Back to Recruiter Dashboard";
            }
            if (byId("recruiter-fields")) byId("recruiter-fields").style.display = "block";
            
            // Populate recruiter fields
            if (byId("recruiter-company")) byId("recruiter-company").value = profile.companyName || "";
            if (byId("recruiter-designation")) byId("recruiter-designation").value = profile.designation || "";
            if (byId("recruiter-website")) byId("recruiter-website").value = profile.companyWebsite || "";
            if (byId("recruiter-linkedin")) byId("recruiter-linkedin").value = profile.linkedInProfile || "";
            if (byId("recruiter-department")) byId("recruiter-department").value = profile.hiringDepartment || "";
            if (byId("recruiter-contact")) byId("recruiter-contact").value = profile.contactNumber || "";
            if (byId("recruiter-location")) byId("recruiter-location").value = profile.companyLocation || "";
            if (byId("recruiter-description")) byId("recruiter-description").value = profile.companyDescription || "";
        } else {
            const backBtn = byId("settings-back-btn");
            if (backBtn) {
                backBtn.textContent = "Back to Candidate Dashboard";
            }
            if (byId("candidate-fields")) byId("candidate-fields").style.display = "block";
            
            // Populate candidate fields
            if (byId("profile-phone")) byId("profile-phone").value = profile.phone || "";
            if (byId("profile-skills")) byId("profile-skills").value = profile.skills || "";
            if (byId("profile-bio")) byId("profile-bio").value = profile.bio || "";
            if (byId("profile-education")) byId("profile-education").value = profile.education || "";
            if (byId("profile-projects")) byId("profile-projects").value = profile.projects || "";
            
            renderResumeState();
        }

        if (byId("profile-email")) byId("profile-email").value = email;
        if (byId("profile-name")) byId("profile-name").value = profile.name || email.split("@")[0] || "";
        if (byId("profile-role-badge")) byId("profile-role-badge").textContent = role || "Candidate";
        if (byId("profile-avatar")) byId("profile-avatar").textContent = getInitial(profile.name, email);

        loadPreferences();

        const form = byId("settings-form");
        if (form) {
            form.addEventListener("submit", function (event) {
                handleSettingsSubmit(event, role, email);
            });
        }
    });
})();
