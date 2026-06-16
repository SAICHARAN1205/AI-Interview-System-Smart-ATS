(function () {
    const CANDIDATE_VIEWS = [
        { key: "all", label: "View Jobs" },
        { key: "applications", label: "My Applications" },
        { key: "resume", label: "Upload Resume" },
        { key: "match", label: "Match Score" },
    ];

    const candidatePageState = {
        email: "",
        role: "",
        view: "all",
        jobs: [],
        applications: [],
        applicationsByJobId: {},
    };

    const scoreCache = new Map();
    const resumeStatusCache = new Map();

    // Match score cache populated from stored Application.matchScore data
    // Used only for best-match sorting in hiring-flow.js — no dynamic AI calls
    window._matchScoreCache = window._matchScoreCache || {};

    function byId(id) {
        return document.getElementById(id);
    }

    function showElement(element, shouldShow) {
        if (!element) {
            return;
        }

        element.classList.toggle("hidden", !shouldShow);
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function getQueryParam(name) {
        return new URLSearchParams(window.location.search).get(name);
    }

    function setBanner(target, message, type) {
        const element = typeof target === "string" ? byId(target) : target;

        if (!element) {
            return;
        }

        if (!element.dataset.baseClass) {
            element.dataset.baseClass = element.className || "message-box";
        }

        if (!message) {
            element.textContent = "";
            element.className = `${element.dataset.baseClass} hidden`.replace(/\s+/g, " ").trim();
            return;
        }

        const baseClass = element.dataset.baseClass.replace(/\bhidden\b/g, "").replace(/\bshow\b/g, "").trim();
        element.textContent = message;
        element.className = `${baseClass} show ${type}`.replace(/\s+/g, " ").trim();
    }

    function clearBanner(target) {
        setBanner(target, "", "success");
    }

    function getResumeUploadErrorMessage(error) {
        if (window.api && typeof window.api.normalizeResumeUploadErrorMessage === "function") {
            return window.api.normalizeResumeUploadErrorMessage(error);
        }

        return error && error.message ? error.message : "Unable to upload the resume.";
    }

    function formatDateTime(value) {
        if (!value) {
            return "Not scheduled";
        }

        const date = new Date(value);

        if (Number.isNaN(date.getTime())) {
            return value;
        }

        return date.toLocaleString(undefined, {
            dateStyle: "medium",
            timeStyle: "short",
        });
    }

    function formatDate(value) {
        if (!value) {
            return "Not available";
        }

        const date = new Date(value);

        if (Number.isNaN(date.getTime())) {
            return value;
        }

        return date.toLocaleDateString(undefined, {
            dateStyle: "medium",
        });
    }

    function getStatusClass(status) {
        return (status || "PENDING").toLowerCase();
    }

    function formatStatusLabel(status) {
        const normalized = String(status || "").trim().toUpperCase();

        if (!normalized) {
            return "Applied";
        }

        return normalized
            .toLowerCase()
            .split("_")
            .map(function (word) {
                return word.charAt(0).toUpperCase() + word.slice(1);
            })
            .join(" ");
    }

    function getApplicationDisplayState(application) {
        const rawStatus = String(application?.status || "APPLIED").toUpperCase();

        if (rawStatus === "REJECTED") {
            return {
                key: "rejected",
                label: "Rejected",
                className: "rejected",
            };
        }

        if (rawStatus === "INTERVIEW" || application?.interviewScheduledAt) {
            return {
                key: "scheduled",
                label: "Interview Scheduled",
                className: "scheduled",
            };
        }

        if (rawStatus === "SHORTLISTED") {
            return {
                key: "shortlisted",
                label: "Shortlisted",
                className: "shortlisted",
            };
        }

        if (rawStatus === "UNDER_REVIEW") {
            return {
                key: "under_review",
                label: "Under Review",
                className: "under-review",
            };
        }

        return {
            key: "applied",
            label: "Applied",
            className: "applied",
        };
    }

    function setButtonLoading(button, loadingText, isLoading) {
        if (window.authApp && window.authApp.setButtonLoading) {
            window.authApp.setButtonLoading(button, loadingText, isLoading);
        }
    }

    function normalizeCollection(payload) {
        if (Array.isArray(payload)) {
            return payload;
        }

        if (Array.isArray(payload?.data?.content)) {
            return payload.data.content;
        }

        if (Array.isArray(payload?.data)) {
            return payload.data;
        }

        if (Array.isArray(payload?.content)) {
            return payload.content;
        }

        return [];
    }

    function sortByNewest(items, fieldName) {
        return items.slice().sort(function (left, right) {
            const leftTime = new Date(left?.[fieldName] || 0).getTime();
            const rightTime = new Date(right?.[fieldName] || 0).getTime();
            return rightTime - leftTime;
        });
    }

    function getJobSalary(job) {
        return job?.salary || job?.salaryRange || "Not specified";
    }

    function getJobSkills(job) {
        return job?.skills || job?.roleSkills || "Not specified";
    }

    function toInputDateTimeParts(value) {
        if (!value) {
            return {
                date: "",
                time: "",
            };
        }

        const date = new Date(value);

        if (Number.isNaN(date.getTime())) {
            return {
                date: "",
                time: "",
            };
        }

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        const hours = String(date.getHours()).padStart(2, "0");
        const minutes = String(date.getMinutes()).padStart(2, "0");

        return {
            date: `${year}-${month}-${day}`,
            time: `${hours}:${minutes}`,
        };
    }

    async function getResolvedSession() {
        if (!window.authApp || !window.authApp.ensureAuthenticated) {
            return null;
        }

        const session = window.authApp.ensureAuthenticated();

        if (!session) {
            return null;
        }

        const email = session.email || window.api.getUser() || "";
        const resolvedRole = await window.authApp.resolveRoleForUser(email);

        return {
            token: session.token,
            email,
            role: resolvedRole || window.authApp.normalizeRole(session.role) || "CANDIDATE",
        };
    }

    function redirectToCandidateDashboard(reason) {
        const params = new URLSearchParams();

        if (reason) {
            params.set("accessDenied", reason);
        }

        const target = params.toString() ? `dashboard.html?${params.toString()}` : "dashboard.html";
        window.location.replace(target);
    }

    function redirectToRecruiterDashboard() {
        window.location.replace("recruiter.html");
    }

    async function requireRecruiterAccess() {
        const session = await getResolvedSession();

        if (!session) {
            return null;
        }

        if (session.role !== "RECRUITER") {
            redirectToCandidateDashboard("recruiter");
            return null;
        }

        return session;
    }

    async function requireCandidateAccess() {
        const session = await getResolvedSession();

        if (!session) {
            return null;
        }

        if (session.role !== "CANDIDATE") {
            redirectToRecruiterDashboard();
            return null;
        }

        return session;
    }

    async function fetchJobsPage(page, size) {
        return window.api.get(`/api/jobs/all?page=${page}&size=${size}`);
    }

    async function fetchAllJobs() {
        const pageSize = 50;
        let page = 0;
        let collectedJobs = [];
        let hasMore = true;

        while (hasMore) {
            const payload = await fetchJobsPage(page, pageSize);
            const nextJobs = normalizeCollection(payload);

            collectedJobs = collectedJobs.concat(nextJobs);

            if (payload?.data?.last === true || nextJobs.length === 0 || nextJobs.length < pageSize) {
                hasMore = false;
            } else {
                page += 1;
            }
        }

        return sortByNewest(collectedJobs, "createdAt");
    }

    function filterRecruiterJobs(jobs, recruiterEmail) {
        return jobs.filter(function (job) {
            return (job?.recruiter?.email || "").toLowerCase() === recruiterEmail.toLowerCase();
        });
    }

    async function fetchRecruiterJobs(recruiterEmail) {
        const jobs = await fetchAllJobs();
        return filterRecruiterJobs(jobs, recruiterEmail);
    }

    async function fetchApplicationsForJob(jobId) {
        return normalizeCollection(await window.api.get(`/api/applications/job/${jobId}`));
    }

    async function fetchRecruiterApplications() {
        return normalizeCollection(await window.api.get("/api/applications/recruiter"));
    }

    function normalizeCandidateApplication(application, fallbackJob) {
        const job = application?.job || fallbackJob || {};

        return {
            applicationId: application?.applicationId || application?.id,
            jobId: application?.jobId || job?.id,
            jobTitle: application?.jobTitle || job?.title || "Untitled role",
            companyName: application?.companyName || job?.companyName || "Unknown company",
            location: application?.location || job?.location || "Not specified",
            salary: application?.salary || job?.salary || job?.salaryRange || "Not specified",
            skills: application?.skills || job?.skills || job?.roleSkills || "Not specified",
            description: application?.description || job?.description || "No description available.",
            requirements: application?.requirements || job?.requirements || "Not specified",
            status: application?.status || "APPLIED",
            rejectionFeedback: application?.rejectionFeedback || "",
            appliedAt: application?.appliedAt,
            interviewScheduledAt: application?.interviewScheduledAt || "",
            matchScore: application?.matchScore ?? null,
            atsScore: application?.atsScore ?? null,
        };
    }

    function buildApplicationsByJobId(applications) {
        return applications.reduce(function (accumulator, application) {
            if (application.jobId != null) {
                accumulator[String(application.jobId)] = application;
            }

            return accumulator;
        }, {});
    }

    function setCandidateApplications(applications) {
        const normalizedApplications = sortByNewest(
            applications.map(function (application) {
                return normalizeCandidateApplication(application);
            }),
            "appliedAt"
        );

        candidatePageState.applications = normalizedApplications;
        candidatePageState.applicationsByJobId = buildApplicationsByJobId(normalizedApplications);

        // TEMPORARY: log application data mapped to job cards
        console.log("[DEBUG] Candidate applicationsByJobId (used by job cards):",
            Object.entries(candidatePageState.applicationsByJobId).map(function (entry) {
                return {
                    jobId: entry[0],
                    status: entry[1].status,
                    rejectionFeedback: entry[1].rejectionFeedback,
                    matchScore: entry[1].matchScore,
                };
            })
        );
    }

    async function fetchCandidateApplications() {
        const payload = await window.api.get("/api/applications/candidate");
        const raw = normalizeCollection(payload);

        // TEMPORARY: log candidate API response to verify rejectionFeedback field
        console.log("[DEBUG] Candidate API raw response:", raw);
        raw.forEach(function (app) {
            if (app.status === "REJECTED") {
                console.log("[DEBUG] REJECTED application found:", {
                    applicationId: app.applicationId,
                    status: app.status,
                    rejectionFeedback: app.rejectionFeedback,
                });
            }
        });

        return raw.map(function (application) {
            return normalizeCandidateApplication(application);
        });
    }

    function normalizeApplicant(application, job) {
        // Handle new flat RecruiterApplicantResponse DTO (candidateId, candidateName at top level)
        // Also handle legacy nested format (application.candidate.id) for backward compat
        const candidateId = application?.candidateId || application?.candidate?.id;
        const candidateName = application?.candidateName || application?.candidate?.name || "Candidate";
        const candidateEmail = application?.candidateEmail || application?.candidate?.email || "No email available";
        const jobId = application?.jobId || application?.job?.id || job?.id;
        const jobTitle = application?.jobTitle || application?.job?.title || job?.title || "Untitled role";
        const companyName = application?.companyName || application?.job?.companyName || job?.companyName || "Unknown company";

        return {
            applicationId: application?.applicationId || application?.id,
            candidateId,
            candidateName,
            candidateEmail,
            status: application?.status || "APPLIED",
            rejectionFeedback: application?.rejectionFeedback || "",
            appliedAt: application?.appliedAt,
            interviewScheduledAt: application?.interviewScheduledAt || "",
            jobId,
            jobTitle,
            companyName,
            salary: application?.salary || application?.job?.salary || job?.salary || "",
            skills: application?.skills || application?.job?.skills || job?.skills || "",
            matchScore: application?.matchScore ?? null,
            atsScore: application?.atsScore ?? null,
        };
    }

    async function fetchRecruiterWorkspace(recruiterEmail) {
        const [jobs, recruiterApplications] = await Promise.all([
            fetchRecruiterJobs(recruiterEmail),
            fetchRecruiterApplications(),
        ]);

        const applicantCountsByJobId = recruiterApplications.reduce(function (accumulator, application) {
            const jobId = String(application?.jobId || "");

            if (jobId) {
                accumulator[jobId] = (accumulator[jobId] || 0) + 1;
            }

            return accumulator;
        }, {});

        const jobsWithCounts = jobs.map(function (job) {
            return Object.assign({}, job, {
                applicantCount: applicantCountsByJobId[String(job.id)] || 0,
            });
        });

        return {
            jobs: jobsWithCounts,
            applicants: recruiterApplications.map(function (application) {
                const matchingJob = jobsWithCounts.find(function (job) {
                    return String(job.id) === String(application.jobId);
                });

                return normalizeApplicant(application, matchingJob);
            }),
        };
    }

    async function fetchApplicationScore(applicationId) {
        if (!applicationId) {
            return null;
        }

        if (!scoreCache.has(applicationId)) {
            scoreCache.set(applicationId, window.api.get(`/api/score/${applicationId}`));
        }

        return scoreCache.get(applicationId);
    }

    async function fetchResumeStatus(candidateId) {
        if (!candidateId) {
            return false;
        }

        if (!resumeStatusCache.has(candidateId)) {
            resumeStatusCache.set(candidateId, window.api.get(`/api/resumes/status/${candidateId}`));
        }

        const payload = await resumeStatusCache.get(candidateId);
        return Boolean(payload?.hasResume);
    }

    async function updateApplicationStatus(applicationId, status, rejectionFeedback) {
        return window.api.put(`/api/applications/${applicationId}/status`, {
            status,
            rejectionFeedback: rejectionFeedback || "",
        });
    }

    async function fetchInterviewSchedules() {
        return normalizeCollection(await window.api.get("/api/interviews/recruiter"));
    }

    async function scheduleInterview(payload) {
        return window.api.post("/api/interviews", payload);
    }

    function renderEmptyState(targetId, message, options) {
        const container = byId(targetId);

        if (!container) {
            return;
        }

        if (window.smartUi && window.smartUi.renderEmptyState) {
            window.smartUi.renderEmptyState(container, Object.assign({
                title: "Nothing to show yet",
                description: message,
                illustration: "..."
            }, options || {}));
        } else {
            container.innerHTML = `
                <div class="empty-state">
                    <h3>Nothing to show yet</h3>
                    <p>${escapeHtml(message)}</p>
                </div>
            `;
        }
        showElement(container, true);
    }

    function renderCandidateViewLinks() {
        const container = byId("jobs-view-links");

        if (!container) {
            return;
        }

        container.innerHTML = CANDIDATE_VIEWS
            .map(function (view) {
                const activeClass = view.key === candidatePageState.view ? " is-active" : "";
                return `
                    <a href="jobs.html?view=${view.key}" class="chip-link${activeClass}">
                        ${escapeHtml(view.label)}
                    </a>
                `;
            })
            .join("");
    }

    function getCandidateViewCopy() {
        if (candidatePageState.view === "applications") {
            return {
                badge: "Candidate Applications",
                title: "Track your real application statuses",
                subtitle: "Your application history stays in sync after refresh, logout, and login so you can see what changed.",
                sectionTitle: "My applications",
                sectionCopy: "Statuses below come from the backend and update when recruiters shortlist, reject, or schedule interviews.",
            };
        }

        if (candidatePageState.view === "resume") {
            return {
                badge: "Candidate Resume",
                title: "Upload your resume and keep applying",
                subtitle: "Resume upload lives alongside open roles so you can prepare your profile without leaving the jobs workspace.",
                sectionTitle: "Available jobs",
                sectionCopy: "Already applied roles stay marked and cannot be applied to twice.",
            };
        }

        if (candidatePageState.view === "match") {
            return {
                badge: "Candidate Match View",
                title: "Explore jobs with persisted application state",
                subtitle: "Review openings, keep your applied roles visible, and use match tools after your resume is uploaded.",
                sectionTitle: "Available jobs",
                sectionCopy: "Applied jobs stay disabled so you can focus on new opportunities and current statuses.",
            };
        }

        return {
            badge: "Candidate Jobs Workspace",
            title: "Browse and act on jobs",
            subtitle: "Explore open roles, apply to jobs, and keep your candidate workflow moving with persisted status tracking.",
            sectionTitle: "Available jobs",
            sectionCopy: "Candidate views load open jobs, disable duplicate applies, and keep your saved application state visible.",
        };
    }

    function renderCandidatePageHeader() {
        const copy = getCandidateViewCopy();

        byId("jobs-role-badge").textContent = copy.badge;
        byId("jobs-page-title").textContent = copy.title;
        byId("jobs-page-subtitle").textContent = copy.subtitle;
        byId("jobs-user-email").textContent = candidatePageState.email || "Unknown user";
        byId("jobs-role-label").textContent = candidatePageState.role;
        byId("jobs-section-title").textContent = copy.sectionTitle;
        byId("jobs-section-copy").textContent = copy.sectionCopy;
    }

    function renderCandidateContextPanels() {
        const resumePanel = byId("resume-upload-panel");
        const helperPanel = byId("jobs-helper-panel");
        const helperTitle = byId("jobs-helper-title");
        const helperText = byId("jobs-helper-text");
        const showResume = candidatePageState.view === "resume";
        let helper = null;

        if (candidatePageState.view === "applications") {
            helper = {
                title: "My Applications",
                text: "This view shows your persisted application history, including recruiter decisions and interview scheduling when available.",
            };
        } else if (candidatePageState.view === "match") {
            helper = {
                title: "Match Score",
                text: "Match scores are calculated when you apply to a job and shown on applied job cards. Upload your resume first to get accurate scores.",
            };
        }

        showElement(resumePanel, showResume);
        showElement(helperPanel, Boolean(helper));

        if (helper) {
            helperTitle.textContent = helper.title;
            helperText.textContent = helper.text;
        }
    }

    function getMatchScoreBadge(jobId) {
        // Read score from stored Application.matchScore — single source of truth
        const application = candidatePageState.applicationsByJobId[String(jobId)];
        if (!application) {
            // Candidate hasn't applied to this job — no score exists
            return '';
        }
        const score = application.matchScore;
        if (score == null) {
            return `<span class="job-card-match loading">Pending analysis</span>`;
        }
        const rounded = Math.round(score);
        let cls = 'fair';
        if (rounded >= 80) cls = 'excellent';
        else if (rounded >= 60) cls = 'good';
        return `<span class="job-card-match ${cls}">${rounded}% Match</span>`;
    }

    function getCandidateCardMarkup(job) {
        const application = candidatePageState.applicationsByJobId[String(job.id)];
        const statusMeta = application ? getApplicationDisplayState(application) : null;
        const buttonLabel = application ? "Applied" : "Apply Now";
        const company = escapeHtml(job.companyName || "Company");
        const companyInitial = company.charAt(0).toUpperCase();
        
        let skillsRaw = getJobSkills(job);
        if (job.requiredSkills && skillsRaw === "Not specified") {
            skillsRaw = job.requiredSkills;
        }

        const skillTags = skillsRaw !== "Not specified"
            ? skillsRaw.split(/,\s*/).slice(0, 4).map(s => `<span class="tag">${escapeHtml(s.trim())}</span>`).join("")
            : '<span class="tag">General</span>';
            
        const locationText = escapeHtml(job.location || "Not specified");
        
        // Use workMode if available, fallback to regex
        let isRemote = false;
        if (job.workMode === "REMOTE") isRemote = true;
        else if (job.workMode === "ONSITE") isRemote = false;
        else isRemote = /remote/i.test(job.location || "");

        const matchBadge = getMatchScoreBadge(job.id);

        // Application-specific overlays (rejection feedback, match score, interview)
        let applicationOverlay = '';
        if (application) {
            const parts = [];

            // Match score
            if (application.matchScore != null) {
                parts.push(`<div class="detail-item"><dt>Match Score</dt><dd>${Math.round(application.matchScore)}% Match</dd></div>`);
            }

            // Interview status
            if (application.interviewScheduledAt) {
                parts.push(`<div class="detail-item"><dt>Interview</dt><dd>${escapeHtml(formatDateTime(application.interviewScheduledAt))}</dd></div>`);
            }

            if (parts.length) {
                applicationOverlay += `<dl class="detail-list compact" style="margin-top:.5rem;">${parts.join('')}</dl>`;
            }

            // Rejection feedback
            if (application.status === "REJECTED") {
                applicationOverlay += `<div class="rejection-feedback-box" style="margin-top:.75rem;padding:.75rem 1rem;background:var(--danger-bg, #fef2f2);border-left:4px solid var(--danger, #dc2626);border-radius:6px;">
                    <strong style="color:var(--danger, #dc2626);font-size:.85rem;">Rejection Feedback:</strong>
                    <p style="margin:.35rem 0 0;color:var(--text-secondary, #555);font-size:.9rem;">
                        ${application.rejectionFeedback
                            ? escapeHtml(application.rejectionFeedback)
                            : 'No specific feedback was provided by the recruiter.'}
                    </p>
                </div>`;
            }
        }

        // Additional Metadata badges
        let metaTags = [];
        if (job.workMode) {
            metaTags.push(`<span class="tag" style="background:#e0f2fe;color:#0284c7;">${escapeHtml(job.workMode.replace('_', ' '))}</span>`);
        }
        if (job.jobType) {
            metaTags.push(`<span class="tag" style="background:#f3e8ff;color:#9333ea;">${escapeHtml(job.jobType.replace('_', ' '))}</span>`);
        }
        if (job.experienceLevel) {
            let expText = job.experienceLevel;
            if (job.experienceLevel === "EXPERIENCED" && job.minimumExperienceYears) {
                expText += ` (${job.minimumExperienceYears}+ Yrs)`;
            }
            metaTags.push(`<span class="tag" style="background:#dcfce7;color:#166534;">${escapeHtml(expText)}</span>`);
        }

        let deadlineInfo = '';
        if (job.applicationDeadline) {
            deadlineInfo = `<p class="subtle-text" style="font-size:0.8rem;margin-top:0.3rem;">⏳ Deadline: ${escapeHtml(formatDate(job.applicationDeadline))}</p>`;
        }

        return `
            <article class="job-card" data-job-card-id="${escapeHtml(job.id)}">
                <div class="job-card-top">
                    <div class="job-card-logo">${companyInitial}</div>
                    <div class="job-card-info">
                        <h3>${escapeHtml(job.title || "Untitled role")}</h3>
                        <p class="job-company">${company}</p>
                    </div>
                    <div style="display:flex;flex-direction:column;gap:.3rem;align-items:flex-end">
                        ${statusMeta
                            ? `<span class="status-pill ${escapeHtml(statusMeta.className)}">${escapeHtml(statusMeta.label)}</span>`
                            : ""}
                        ${matchBadge}
                    </div>
                </div>
                <div class="job-card-tags" style="margin-bottom:0.4rem;">${skillTags}</div>
                ${metaTags.length > 0 ? `<div class="job-card-tags" style="margin-bottom:0.8rem;">${metaTags.join('')}</div>` : ''}
                
                <p class="job-card-location">${isRemote ? "🌐" : "📍"} ${locationText}</p>
                ${deadlineInfo}
                <p class="job-description">${escapeHtml((job.description || "No description.").substring(0, 150))}${(job.description || "").length > 150 ? "…" : ""}</p>
                ${applicationOverlay}
                <div class="job-card-bottom">
                    <span class="job-salary">${escapeHtml(getJobSalary(job))}</span>
                    <div class="job-card-actions">
                        <button type="button" class="btn ${application ? "btn-outline" : "btn-primary"}" data-action="apply" data-job-id="${escapeHtml(job.id)}" ${application ? "disabled" : ""}>
                            ${escapeHtml(buttonLabel)}
                        </button>
                    </div>
                </div>
            </article>
        `;
    }

    function getCandidateApplicationCardMarkup(application) {
        const statusMeta = getApplicationDisplayState(application);

        return `
            <article class="job-card">
                <div class="card-header-row">
                    <div>
                        <p class="card-kicker">Application</p>
                        <h3>${escapeHtml(application.jobTitle || "Untitled role")}</h3>
                    </div>
                    <span class="status-pill ${escapeHtml(statusMeta.className)}">${escapeHtml(statusMeta.label)}</span>
                </div>
                <p class="job-company">${escapeHtml(application.companyName || "Unknown company")} | ${escapeHtml(application.location || "Location not specified")}</p>
                <p class="job-salary">Salary: ${escapeHtml(application.salary || "Not specified")}</p>
                <p class="job-description">${escapeHtml(application.description || "No description available.")}</p>
                <dl class="detail-list compact">
                    <div class="detail-item">
                        <dt>Skills</dt>
                        <dd>${escapeHtml(application.skills || "Not specified")}</dd>
                    </div>
                    <div class="detail-item">
                        <dt>Current Status</dt>
                        <dd>${escapeHtml(statusMeta.label)}</dd>
                    </div>
                    <div class="detail-item">
                        <dt>Match Score</dt>
                        <dd>${application.matchScore != null ? Math.round(application.matchScore) + '% Match' : 'Pending analysis'}</dd>
                    </div>
                    <div class="detail-item">
                        <dt>Requirements</dt>
                        <dd>${escapeHtml(application.requirements || "Not specified")}</dd>
                    </div>
                    <div class="detail-item">
                        <dt>Applied On</dt>
                        <dd>${escapeHtml(formatDate(application.appliedAt))}</dd>
                    </div>
                    <div class="detail-item">
                        <dt>Interview</dt>
                        <dd>${escapeHtml(application.interviewScheduledAt ? formatDateTime(application.interviewScheduledAt) : "Not scheduled")}</dd>
                    </div>
                </dl>
                ${application.status === "REJECTED"
                    ? `<div class="rejection-feedback-box" style="margin-top:.75rem;padding:.75rem 1rem;background:var(--danger-bg, #fef2f2);border-left:4px solid var(--danger, #dc2626);border-radius:6px;">
                        <strong style="color:var(--danger, #dc2626);font-size:.85rem;">Rejection Feedback:</strong>
                        <p style="margin:.35rem 0 0;color:var(--text-secondary, #555);font-size:.9rem;">
                            ${application.rejectionFeedback
                                ? escapeHtml(application.rejectionFeedback)
                                : 'No specific feedback was provided by the recruiter.'}
                        </p>
                    </div>`
                    : ""}
                <div class="job-card-actions">
                    <a href="interview.html?${application.applicationId ? `applicationId=${escapeHtml(application.applicationId)}` : `jobId=${escapeHtml(application.jobId)}`}" class="btn btn-primary">Practice Interview</a>
                </div>
            </article>
        `;
    }

    /**
     * Populate _matchScoreCache from stored Application.matchScore data.
     * No dynamic AI calls — scores are read from the database via applications.
     * This cache is only used by hiring-flow.js for "best match" sorting.
     */
    function populateMatchScoreCacheFromApplications() {
        const applications = candidatePageState.applications || [];
        applications.forEach(function (app) {
            if (app.jobId != null && app.matchScore != null) {
                window._matchScoreCache[String(app.jobId)] = app.matchScore;
            }
        });
    }

    function renderCandidateJobs() {
        const jobsGrid = byId("jobs-grid");
        const emptyState = byId("jobs-empty");
        const searchPanel = byId("jobs-search-panel");

        showElement(emptyState, false);
        if (searchPanel) showElement(searchPanel, candidatePageState.view !== "applications");

        // Apply search/filter if hiring-flow.js is loaded
        let visibleJobs = candidatePageState.jobs;
        if (window.hiringFlow && window.hiringFlow.filterAndSortJobs) {
            visibleJobs = window.hiringFlow.filterAndSortJobs(visibleJobs);
        }

        if (!visibleJobs.length) {
            jobsGrid.innerHTML = "";
            const msg = candidatePageState.jobs.length
                ? "No jobs match your search. Try different keywords."
                : "No jobs are available right now. Check back once recruiters publish new postings.";
            renderEmptyState("jobs-empty", msg);
            return;
        }

        jobsGrid.innerHTML = visibleJobs
            .map(function (job) {
                return getCandidateCardMarkup(job);
            })
            .join("");

        // Populate match score cache from stored application data (no AI calls)
        populateMatchScoreCacheFromApplications();
    }

    let activeAppTab = "all";

    function renderCandidateApplications() {
        const jobsGrid = byId("jobs-grid");
        const emptyState = byId("jobs-empty");
        const searchPanel = byId("jobs-search-panel");
        if (searchPanel) showElement(searchPanel, false);

        showElement(emptyState, false);

        // Emit tabs render event
        window.dispatchEvent(new CustomEvent('apps-tabs-render', {
            detail: { applications: candidatePageState.applications, activeTab: activeAppTab }
        }));

        // Filter by active tab
        let visible = candidatePageState.applications;
        if (activeAppTab !== 'all') {
            visible = visible.filter(function (app) {
                return getApplicationDisplayState(app).key === activeAppTab;
            });
        }

        if (!visible.length) {
            jobsGrid.innerHTML = "";
            const msg = activeAppTab === 'all'
                ? "You have not applied to any jobs yet."
                : "No applications in this category.";
            renderEmptyState("jobs-empty", msg);
            return;
        }

        jobsGrid.innerHTML = visible
            .map(function (application) {
                return getCandidateApplicationCardMarkup(application);
            })
            .join("");
    }

    function renderCandidateContent() {
        renderCandidatePageHeader();

        if (candidatePageState.view === "applications") {
            renderCandidateApplications();
            return;
        }

        renderCandidateJobs();
    }

    function getCandidateApplicationsErrorMessage(error) {
        if (error?.status === 404) {
            return "Candidate application persistence requires `GET /api/applications/candidate`, which is not available from the backend.";
        }

        return error?.message || "Unable to load your saved applications right now.";
    }

    async function loadCandidateWorkspace() {
        const jobsGrid = byId("jobs-grid");
        const emptyState = byId("jobs-empty");
        const loadingRow = byId("jobs-loading");

        showElement(loadingRow, true);
        clearBanner("jobs-global-message");
        showElement(emptyState, false);
        if (jobsGrid && window.smartUi && window.smartUi.renderSkeletonCards) {
            window.smartUi.renderSkeletonCards(jobsGrid, {
                count: candidatePageState.view === "applications" ? 3 : 6,
                variant: candidatePageState.view === "applications" ? "application" : "job"
            });
        }

        try {
            const results = await Promise.allSettled([
                fetchAllJobs(),
                fetchCandidateApplications(),
            ]);
            const jobsResult = results[0];
            const applicationsResult = results[1];

            candidatePageState.jobs = jobsResult.status === "fulfilled" ? jobsResult.value : [];

            if (applicationsResult.status === "fulfilled") {
                setCandidateApplications(applicationsResult.value);
            } else {
                setCandidateApplications([]);
                setBanner("jobs-global-message", getCandidateApplicationsErrorMessage(applicationsResult.reason), "error");
            }

            if (candidatePageState.view === "applications") {
                renderCandidateContent();
                return;
            }

            if (jobsResult.status !== "fulfilled") {
                jobsGrid.innerHTML = "";
                renderEmptyState("jobs-empty", jobsResult.reason?.message || "Unable to load jobs right now.", {
                    title: "Jobs unavailable",
                    actionLabel: "Try again",
                    onAction: loadCandidateWorkspace
                });
                return;
            }

            renderCandidateContent();
        } catch (error) {
            jobsGrid.innerHTML = "";
            renderEmptyState("jobs-empty", error.message || "Unable to load jobs right now.", {
                title: "Jobs unavailable",
                actionLabel: "Retry",
                onAction: loadCandidateWorkspace
            });
        } finally {
            showElement(loadingRow, false);
        }
    }

    async function refreshCandidateApplications() {
        const applications = await fetchCandidateApplications();
        setCandidateApplications(applications);
    }

    async function applyToJob(button) {
        const jobId = button.dataset.jobId;
        if (!jobId) return;

        const matchingJob = candidatePageState.jobs.find(function (job) {
            return String(job.id) === String(jobId);
        });

        if (!matchingJob) {
            if (window.showToast) window.showToast("Job not found.", "error");
            return;
        }

        // Open modal if hiring-flow.js is loaded
        if (window.openApplyModal) {
            window.openApplyModal(matchingJob, async function (job, snapshot, closeModal) {
                try {
                    const response = await window.api.post(`/api/applications/apply/${jobId}`);
                    const application = normalizeCandidateApplication(response, matchingJob);
                    const nextApplications = [application].concat(
                        candidatePageState.applications.filter(function (item) {
                            return String(item.jobId) !== String(jobId);
                        })
                    );
                    setCandidateApplications(nextApplications);

                    // Show success animation in modal before closing
                    const modalBody = document.getElementById('apply-modal-body');
                    if (modalBody) {
                        modalBody.innerHTML =
                            '<div class="submit-success-anim">' +
                                '<div class="success-checkmark">✓</div>' +
                                '<h3>Application Submitted!</h3>' +
                                '<p>Your application for ' + (job.title || 'this role') + ' has been sent successfully.</p>' +
                            '</div>';
                        const footer = document.querySelector('#apply-modal-root .modal-footer');
                        if (footer) footer.innerHTML = '<button class="btn btn-primary" id="apply-done-btn">Done</button>';
                        const doneBtn = document.getElementById('apply-done-btn');
                        if (doneBtn) doneBtn.addEventListener('click', function () {
                            closeModal();
                            renderCandidateContent();
                        });
                        setTimeout(function () {
                            closeModal();
                            renderCandidateContent();
                            if (window.showToast) window.showToast("Application submitted successfully!", "success");
                        }, 2200);
                    } else {
                        closeModal();
                        renderCandidateContent();
                        if (window.showToast) window.showToast("Application submitted successfully!", "success");
                    }
                } catch (error) {
                    closeModal();
                    if (/already applied/i.test(error.message || "")) {
                        try {
                            await refreshCandidateApplications();
                            renderCandidateContent();
                            if (window.showToast) window.showToast("You've already applied to this job.", "info");
                        } catch (e) {}
                        return;
                    }
                    if (window.showToast) window.showToast(error.message || "Unable to apply.", "error");
                }
            });
            return;
        }

        // Fallback: direct apply
        setButtonLoading(button, "Applying...", true);
        try {
            const response = await window.api.post(`/api/applications/apply/${jobId}`);
            const application = normalizeCandidateApplication(response, matchingJob);
            const nextApplications = [application].concat(
                candidatePageState.applications.filter(function (item) {
                    return String(item.jobId) !== String(jobId);
                })
            );
            setCandidateApplications(nextApplications);
            renderCandidateContent();
            if (window.showToast) window.showToast("Application submitted successfully!", "success");
            else setBanner("jobs-global-message", "Application submitted successfully.", "success");
        } catch (error) {
            if (/already applied/i.test(error.message || "")) {
                try {
                    await refreshCandidateApplications();
                    renderCandidateContent();
                } catch (e) {}
                return;
            }
            if (window.showToast) window.showToast(error.message || "Unable to apply.", "error");
            else setBanner("jobs-global-message", error.message || "Unable to apply.", "error");
            setButtonLoading(button, "Applying...", false);
        }
    }

    async function uploadResume(event) {
        event.preventDefault();

        const fileInput = byId("resume-file");
        const button = byId("resume-upload-button");
        const file = fileInput.files[0];

        if (!file) {
            setBanner("jobs-global-message", "Please choose a PDF, DOC, or DOCX resume before uploading.", "error");
            return;
        }

        setButtonLoading(button, "Uploading...", true);

        try {
            const formData = new FormData();
            formData.append("file", file);
            await window.api.upload("/api/resumes/upload", formData);
            if (window.api.refreshProfileResumeState) {
                await window.api.refreshProfileResumeState();
            }
            setBanner("jobs-global-message", "Resume uploaded successfully.", "success");
            fileInput.value = "";
        } catch (error) {
            setBanner("jobs-global-message", getResumeUploadErrorMessage(error), "error");
        } finally {
            setButtonLoading(button, "Uploading...", false);
        }
    }

    async function initializeCandidateJobsPage() {
        if (document.body.dataset.page !== "jobs") {
            return;
        }

        const session = await requireCandidateAccess();

        if (!session) {
            return;
        }

        candidatePageState.email = session.email;
        candidatePageState.role = session.role;
        candidatePageState.view = getQueryParam("view") || "all";

        renderCandidateViewLinks();
        renderCandidateContextPanels();
        await loadCandidateWorkspace();

        const jobsGrid = byId("jobs-grid");
        const resumeForm = byId("resume-upload-form");

        if (jobsGrid) {
            jobsGrid.addEventListener("click", function (event) {
                const button = event.target.closest("[data-action='apply']");

                if (button) {
                    applyToJob(button);
                }
            });
        }

        if (resumeForm) {
            resumeForm.addEventListener("submit", uploadResume);
        }

        // Search/filter re-render
        window.addEventListener('jobs-filter-change', function () {
            if (candidatePageState.view !== 'applications') {
                renderCandidateJobs();
            }
        });

        // Status tabs for applications
        window.addEventListener('apps-tab-change', function (e) {
            activeAppTab = e.detail?.tab || 'all';
            renderCandidateApplications();
        });

        // Listen for global resume updates
        window.addEventListener('resume-uploaded', function () {
            resumeStatusCache.clear();
            // Reload workspace to get updated application scores from backend
            loadCandidateWorkspace();
            if (candidatePageState.view === "resume") {
                setBanner("jobs-global-message", "Resume synchronized successfully. Match scores will update for your applied jobs.", "success");
            }
        });
    }

    function getCreateJobPayload() {
        return {
            title: byId("job-title").value.trim(),
            companyName: byId("job-company").value.trim(),
            location: byId("job-location")?.value.trim() || null,
            salary: byId("job-salary").value.trim(),
            applicationDeadline: byId("job-deadline")?.value || null,
            openingsCount: byId("job-openings")?.value ? parseInt(byId("job-openings").value) : null,

            jobType: byId("job-type")?.value || null,
            workMode: byId("job-work-mode")?.value || null,
            experienceLevel: byId("job-experience-level")?.value || null,

            minimumPercentage: byId("job-min-percentage")?.value ? parseFloat(byId("job-min-percentage").value) : null,
            minimumCGPA: byId("job-min-cgpa")?.value ? parseFloat(byId("job-min-cgpa").value) : null,
            minimumExperienceYears: byId("job-min-experience")?.value ? parseInt(byId("job-min-experience").value) : null,
            minimumEducation: (byId("job-experience-level")?.value === "FRESHER" ? byId("job-education-fresher")?.value.trim() : byId("job-education-experienced")?.value.trim()) || null,

            skills: byId("job-skills").value.trim(),
            requiredSkills: byId("job-required-skills")?.value.trim() || null,
            preferredSkills: byId("job-preferred-skills")?.value.trim() || null,

            requirements: byId("job-requirements").value.trim(),
            description: byId("job-description").value.trim(),

            companyDescription: byId("job-company-desc")?.value.trim() || null,
            benefits: byId("job-benefits")?.value.trim() || null,
            noticePeriodPreference: byId("job-notice-period")?.value.trim() || null,
            recruiterNotes: byId("job-recruiter-notes")?.value.trim() || null,
        };
    }

    function clearFieldErrors() {
        const fields = ["job-title", "job-company", "job-salary", "job-skills", "job-requirements", "job-description", "job-location", "job-deadline", "job-openings"];
        fields.forEach(function (fieldId) {
            const errorElement = byId(`${fieldId}-error`);
            if (errorElement) {
                errorElement.textContent = "";
            }
        });
    }

    function validateJobPayload(payload) {
        const errors = {};

        if (!payload.title) errors["job-title"] = "Job title is required.";
        if (!payload.companyName) errors["job-company"] = "Company is required.";
        if (!payload.salary) errors["job-salary"] = "Salary is required.";
        if (!payload.skills) errors["job-skills"] = "Core skills are required.";
        if (!payload.requirements) errors["job-requirements"] = "Requirements are required.";
        if (!payload.description) errors["job-description"] = "Description is required.";

        if (payload.applicationDeadline) {
            const selectedDate = new Date(payload.applicationDeadline);
            const today = new Date();
            today.setHours(0, 0, 0, 0); // Start of today

            // Add timezone offset to selected date so it compares correctly if it was parsed as UTC
            const localSelectedDate = new Date(selectedDate.getTime() + selectedDate.getTimezoneOffset() * 60000);

            if (localSelectedDate < today) {
                errors["job-deadline"] = "Application deadline cannot be expired.";
            }
        }

        if (payload.openingsCount != null && payload.openingsCount < 1) {
            errors["job-openings"] = "Number of openings must be at least 1.";
        }

        return errors;
    }

    function renderFieldErrors(errors) {
        Object.keys(errors).forEach(function (fieldId) {
            const errorElement = byId(`${fieldId}-error`);

            if (errorElement) {
                errorElement.textContent = errors[fieldId];
            }
        });
    }

    function getRecruiterJobCardMarkup(job) {
        const applicantCount = job.applicantCount == null ? "Unavailable" : String(job.applicantCount);

        return `
            <article class="job-card recruiter-job-card" data-job-id="${escapeHtml(job.id)}">
                <div class="card-header-row">
                    <div>
                        <p class="card-kicker">My Job</p>
                        <h3>${escapeHtml(job.title || "Untitled role")}</h3>
                    </div>
                    <span class="tag">${escapeHtml(job.companyName || "Unknown company")}</span>
                </div>
                <p class="job-salary">${escapeHtml(getJobSalary(job))}</p>
                <p class="job-description">${escapeHtml(job.description || "No description available.")}</p>
                <dl class="detail-list compact">
                    <div class="detail-item">
                        <dt>Role / skills</dt>
                        <dd>${escapeHtml(getJobSkills(job))}</dd>
                    </div>
                    <div class="detail-item">
                        <dt>Requirements</dt>
                        <dd>${escapeHtml(job.requirements || "Not specified")}</dd>
                    </div>
                    <div class="detail-item">
                        <dt>Applicants</dt>
                        <dd>${escapeHtml(applicantCount)}</dd>
                    </div>
                </dl>
                <div class="job-card-actions">
                    <a href="applicants.html?jobId=${escapeHtml(job.id)}" class="btn btn-primary">View Applicants</a>
                    <button type="button" class="btn btn-outline" data-action="edit-job" data-job-id="${escapeHtml(job.id)}">Edit</button>
                    <button type="button" class="btn btn-danger" data-action="delete-job" data-job-id="${escapeHtml(job.id)}">Delete</button>
                </div>
            </article>
        `;
    }

    async function loadRecruiterJobsSection(recruiterEmail) {
        const grid = byId("recruiter-jobs-grid");
        const emptyState = byId("recruiter-jobs-empty");
        const loadingRow = byId("recruiter-jobs-loading");
        const totalJobs = byId("create-job-total-jobs");

        if (!grid || !loadingRow || !emptyState) {
            return;
        }

        showElement(loadingRow, true);
        showElement(emptyState, false);
        if (grid && window.smartUi && window.smartUi.renderSkeletonCards) {
            window.smartUi.renderSkeletonCards(grid, {
                count: 4,
                variant: "job"
            });
        }

        try {
            const workspace = await fetchRecruiterWorkspace(recruiterEmail);
            const jobs = workspace.jobs;

            if (totalJobs) {
                totalJobs.textContent = String(jobs.length);
            }

            if (!jobs.length) {
                grid.innerHTML = "";
                renderEmptyState("recruiter-jobs-empty", "Create your first role to start receiving applications and recruiter analytics.", {
                    title: "No recruiter jobs yet",
                    actionLabel: "Create a job",
                    onAction: function () {
                        var titleInput = byId("job-title");
                        if (titleInput) {
                            titleInput.focus();
                        }
                    }
                });
                return;
            }

            grid.innerHTML = jobs
                .map(function (job) {
                    return getRecruiterJobCardMarkup(job);
                })
                .join("");
        } catch (error) {
            grid.innerHTML = "";
            renderEmptyState("recruiter-jobs-empty", error.message || "Unable to load your jobs right now.", {
                title: "Recruiter jobs unavailable",
                actionLabel: "Retry",
                onAction: function () {
                    loadRecruiterJobsSection(recruiterEmail);
                }
            });
        } finally {
            showElement(loadingRow, false);
        }
    }

    async function createJob(event, recruiterEmail) {
        event.preventDefault();

        const payload = getCreateJobPayload();
        const errors = validateJobPayload(payload);
        const button = byId("create-job-button");
        const form = byId("create-job-form");

        clearFieldErrors();
        clearBanner("create-job-message");
        clearBanner("create-job-page-message");

        if (Object.keys(errors).length > 0) {
            renderFieldErrors(errors);
            setBanner("create-job-message", "Please fix the highlighted validation errors.", "error");
            return;
        }

        setButtonLoading(button, "Creating...", true);

        try {
            await window.api.post("/api/jobs/create", payload);
            form.reset();
            setBanner("create-job-message", "Job created successfully.", "success");
            setBanner("create-job-page-message", "Your new job is live in the recruiter workspace.", "success");
            await loadRecruiterJobsSection(recruiterEmail);
        } catch (error) {
            setBanner("create-job-message", error.message || "Unable to create the job.", "error");
        } finally {
            setButtonLoading(button, "Creating...", false);
        }
    }

    function openEditJobModal(job, onSave) {
        let modalRoot = byId("edit-job-modal-root");
        if (!modalRoot) {
            modalRoot = document.createElement("div");
            modalRoot.id = "edit-job-modal-root";
            document.body.appendChild(modalRoot);
        }

        modalRoot.innerHTML = `
            <div class="modal-overlay" id="edit-job-overlay" style="position:fixed;inset:0;background:rgba(0,0,0,.55);z-index:1000;display:flex;align-items:center;justify-content:center;">
                <div class="modal-panel" style="background:var(--c-surface,#1e1e2e);border-radius:12px;padding:2rem;width:min(560px,95vw);max-height:90vh;overflow-y:auto;">
                    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1.5rem;">
                        <h2 style="margin:0;">Edit Job</h2>
                        <button id="edit-job-close" style="background:none;border:none;font-size:1.5rem;cursor:pointer;color:inherit;">✕</button>
                    </div>
                    <div id="edit-job-error" class="message-box" style="display:none;margin-bottom:1rem;"></div>
                    <div class="field">
                        <label for="edit-job-title">Job Title *</label>
                        <input type="text" id="edit-job-title" value="${escapeHtml(job.title || "")}">
                    </div>
                    <div class="field">
                        <label for="edit-job-company">Company Name *</label>
                        <input type="text" id="edit-job-company" value="${escapeHtml(job.companyName || "")}">
                    </div>
                    <div class="field">
                        <label for="edit-job-salary">Salary</label>
                        <input type="text" id="edit-job-salary" value="${escapeHtml(job.salary || "")}">
                    </div>
                    <div class="field">
                        <label for="edit-job-skills">Skills</label>
                        <input type="text" id="edit-job-skills" value="${escapeHtml(job.skills || "")}">
                    </div>
                    <div class="field">
                        <label for="edit-job-requirements">Requirements *</label>
                        <textarea id="edit-job-requirements" rows="4" style="width:100%;resize:vertical;">${escapeHtml(job.requirements || "")}</textarea>
                    </div>
                    <div class="field">
                        <label for="edit-job-description">Description *</label>
                        <textarea id="edit-job-description" rows="5" style="width:100%;resize:vertical;">${escapeHtml(job.description || "")}</textarea>
                    </div>
                    <div style="display:flex;gap:1rem;margin-top:1.5rem;">
                        <button id="edit-job-save" class="btn btn-primary">Save Changes</button>
                        <button id="edit-job-cancel" class="btn btn-outline">Cancel</button>
                    </div>
                </div>
            </div>
        `;

        function close() {
            modalRoot.innerHTML = "";
        }

        byId("edit-job-close").addEventListener("click", close);
        byId("edit-job-cancel").addEventListener("click", close);
        byId("edit-job-overlay").addEventListener("click", function (e) {
            if (e.target === byId("edit-job-overlay")) close();
        });

        byId("edit-job-save").addEventListener("click", async function () {
            const saveBtn = byId("edit-job-save");
            const errorBox = byId("edit-job-error");
            const updates = {
                title: byId("edit-job-title").value.trim(),
                companyName: byId("edit-job-company").value.trim(),
                salary: byId("edit-job-salary").value.trim(),
                skills: byId("edit-job-skills").value.trim(),
                requirements: byId("edit-job-requirements").value.trim(),
                description: byId("edit-job-description").value.trim(),
            };

            if (!updates.title || !updates.companyName || !updates.requirements || !updates.description) {
                errorBox.textContent = "Title, Company, Requirements, and Description are required.";
                errorBox.style.display = "block";
                errorBox.className = "message-box show error";
                return;
            }

            errorBox.style.display = "none";
            setButtonLoading(saveBtn, "Saving...", true);

            try {
                await onSave(updates);
                close();
            } catch (error) {
                errorBox.textContent = error.message || "Unable to save changes. Please try again.";
                errorBox.style.display = "block";
                errorBox.className = "message-box show error";
                setButtonLoading(saveBtn, "Saving...", false);
            }
        });
    }

    async function handleEditJob(jobId, recruiterEmail) {
        // Find the job in recruiter workspace state
        let job = null;
        try {
            const payload = await window.api.get(`/api/jobs/${jobId}`);
            job = payload?.data || payload;
        } catch (error) {
            if (window.showToast) window.showToast("Unable to load job details.", "error");
            return;
        }

        openEditJobModal(job, async function (updates) {
            await window.api.put(`/api/jobs/${jobId}`, updates);
            if (window.showToast) window.showToast("Job updated successfully.", "success");
            await loadRecruiterJobsSection(recruiterEmail);
        });
    }

    function openDeleteConfirmation(onConfirm) {
        let modalRoot = byId("delete-job-modal-root");
        if (!modalRoot) {
            modalRoot = document.createElement("div");
            modalRoot.id = "delete-job-modal-root";
            document.body.appendChild(modalRoot);
        }

        modalRoot.innerHTML = `
            <div class="modal-overlay" id="delete-job-overlay" style="position:fixed;inset:0;background:rgba(0,0,0,.55);z-index:1000;display:flex;align-items:center;justify-content:center;">
                <div class="modal-panel" style="background:var(--c-surface,#1e1e2e);border-radius:12px;padding:2rem;width:min(460px,92vw);">
                    <h2 style="margin-top:0;">Delete job?</h2>
                    <p class="subtle-text">This will remove the job and its related applications. This action cannot be undone.</p>
                    <div style="display:flex;gap:1rem;justify-content:flex-end;margin-top:1.5rem;">
                        <button type="button" id="delete-job-cancel" class="btn btn-outline">Cancel</button>
                        <button type="button" id="delete-job-confirm" class="btn btn-danger">Delete Job</button>
                    </div>
                </div>
            </div>
        `;

        function close() {
            modalRoot.innerHTML = "";
        }

        byId("delete-job-cancel").addEventListener("click", close);
        byId("delete-job-overlay").addEventListener("click", function (event) {
            if (event.target === byId("delete-job-overlay")) {
                close();
            }
        });
        byId("delete-job-confirm").addEventListener("click", async function () {
            await onConfirm(byId("delete-job-confirm"), close);
        });
    }

    async function handleDeleteJob(jobId, button, recruiterEmail) {
        openDeleteConfirmation(async function (confirmButton, closeModal) {
            setButtonLoading(confirmButton, "Deleting...", true);

            try {
                await window.api.del(`/api/jobs/${jobId}`);
                closeModal();
                if (window.showToast) window.showToast("Job deleted successfully.", "success");
                await loadRecruiterJobsSection(recruiterEmail);
            } catch (error) {
                if (window.showToast) window.showToast(error.message || "Unable to delete the job.", "error");
                setButtonLoading(confirmButton, "Deleting...", false);
                setButtonLoading(button, "Deleting...", false);
            }
        });
    }


    function maybeFocusMyJobsSection() {
        if (getQueryParam("tab") !== "my-jobs") {
            return;
        }

        const section = byId("my-jobs-section");

        if (!section) {
            return;
        }

        section.classList.add("is-highlighted");

        window.setTimeout(function () {
            section.scrollIntoView({
                behavior: "smooth",
                block: "start",
            });
        }, 150);
    }

    async function initializeCreateJobPage() {
        if (document.body.dataset.page !== "create-job") {
            return;
        }

        const session = await requireRecruiterAccess();

        if (!session) {
            return;
        }

        byId("create-job-user-email").textContent = session.email || "Unknown recruiter";
        byId("create-job-role-label").textContent = session.role;

        const form = byId("create-job-form");
        const jobsGrid = byId("recruiter-jobs-grid");
        const deadlineInput = byId("job-deadline");

        if (deadlineInput) {
            deadlineInput.min = new Date().toLocaleDateString('en-CA');
        }

        if (form) {
            const expSelect = byId("job-experience-level");
            if (expSelect) {
                expSelect.addEventListener("change", function(e) {
                    const fresherSec = byId("fresher-section");
                    const expSec = byId("experienced-section");
                    if (e.target.value === "FRESHER") {
                        if(fresherSec) fresherSec.style.display = "flex";
                        if(expSec) expSec.style.display = "none";
                    } else if (e.target.value === "EXPERIENCED") {
                        if(fresherSec) fresherSec.style.display = "none";
                        if(expSec) expSec.style.display = "flex";
                    } else {
                        if(fresherSec) fresherSec.style.display = "none";
                        if(expSec) expSec.style.display = "none";
                    }
                });
            }

            form.addEventListener("submit", function (event) {
                createJob(event, session.email);
            });
        }

        if (jobsGrid) {
            jobsGrid.addEventListener("click", function (event) {
                const button = event.target.closest("[data-action]");

                if (!button) {
                    return;
                }

                const jobId = button.dataset.jobId;

                if (button.dataset.action === "edit-job" && jobId) {
                    handleEditJob(jobId, session.email);
                    return;
                }

                if (button.dataset.action === "delete-job" && jobId) {
                    handleDeleteJob(jobId, button, session.email);
                    return;
                }
            });
        }

        await loadRecruiterJobsSection(session.email);
        maybeFocusMyJobsSection();
    }

    window.jobsApp = {
        byId,
        showElement,
        escapeHtml,
        setBanner,
        clearBanner,
        formatDateTime,
        formatDate,
        getStatusClass,
        formatStatusLabel,
        getApplicationDisplayState,
        getJobSalary,
        getJobSkills,
        toInputDateTimeParts,
        sortByNewest,
        normalizeApplicant,
        requireRecruiterAccess,
        requireCandidateAccess,
        fetchAllJobs,
        fetchRecruiterJobs,
        fetchRecruiterWorkspace,
        fetchApplicationsForJob,
        fetchRecruiterApplications,
        fetchCandidateApplications,
        fetchApplicationScore,
        fetchResumeStatus,
        updateApplicationStatus,
        fetchInterviewSchedules,
        scheduleInterview,
    };

    document.addEventListener("DOMContentLoaded", function () {
        initializeCandidateJobsPage();
        initializeCreateJobPage();
    });
})();
