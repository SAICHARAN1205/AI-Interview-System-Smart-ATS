(function () {
    const state = {
        session: null,
        mode: "review",
        jobs: [],
        applicants: [],
        selectedJobId: "all",
    };

    function byId(id) {
        return document.getElementById(id);
    }

    function getMode() {
        const params = new URLSearchParams(window.location.search);
        return params.get("mode") === "scores" ? "scores" : "review";
    }

    function getVisibleApplicants() {
        if (state.selectedJobId === "all") {
            return state.applicants;
        }

        return state.applicants.filter(function (applicant) {
            return String(applicant.jobId) === String(state.selectedJobId);
        });
    }

    function getApplicantStatusMeta(applicant) {
        return window.jobsApp.getApplicationDisplayState(applicant);
    }

    function renderHeader() {
        const scoreMode = state.mode === "scores";

        byId("applicants-badge").textContent = scoreMode ? "Candidate Scores" : "Applicant Review";
        byId("applicants-title").textContent = scoreMode
            ? "Review candidate scores across recruiter jobs."
            : "Manage applicants across recruiter jobs.";
        byId("applicants-subtitle").textContent = scoreMode
            ? "Score mode preloads recruiter-visible candidate scores so you can compare shortlisted talent faster."
            : "Review candidate details, resume status, candidate scores, and application states from one page.";
        byId("applicants-user-email").textContent = state.session.email || "Unknown recruiter";
    }

    function renderFilter() {
        const filter = byId("applicants-job-filter");
        if (!filter) {
            return;
        }

        filter.innerHTML = `
            <option value="all">All recruiter jobs</option>
            ${state.jobs.map(function (job) {
                return `<option value="${window.jobsApp.escapeHtml(job.id)}">${window.jobsApp.escapeHtml(job.title || "Untitled role")}</option>`;
            }).join("")}
        `;
        filter.value = state.selectedJobId;
    }

    function renderCounts() {
        const visibleApplicants = getVisibleApplicants();
        const shortlistedCount = visibleApplicants.filter(function (item) {
            const statusMeta = getApplicantStatusMeta(item);
            return statusMeta.key === "shortlisted" || statusMeta.key === "scheduled";
        }).length;

        byId("applicants-total-count").textContent = String(visibleApplicants.length);
        byId("applicants-shortlisted-count").textContent = String(shortlistedCount);
        byId("applicants-section-title").textContent = state.mode === "scores"
            ? "Candidate scores"
            : "Applicant cards";
    }

    function getApplicantCardMarkup(applicant) {
        const scoreText = applicant.matchScore != null
            ? `${Math.round(applicant.matchScore)}% Match`
            : "Pending analysis";
        const resumeText = applicant.resumeLoading
            ? "Checking..."
            : applicant.hasResume
                ? "Uploaded"
                : "Resume not uploaded";
        const statusMeta = getApplicantStatusMeta(applicant);
        const matchClass = applicant.matchScore != null
            ? (applicant.matchScore >= 70 ? "" : " low")
            : " na";
        const canSchedule = applicant.status === "SHORTLISTED" || applicant.status === "INTERVIEW" || Boolean(applicant.interviewScheduledAt);
        const scheduleLabel = applicant.interviewScheduledAt ? "Reschedule" : "Schedule Interview";

        const scorePanel = applicant.showScore
            ? `
                <section class="score-panel">
                    <h4>Candidate Score</h4>
                    <p class="subtle-text">
                        ${applicant.matchScore != null
                            ? `ATS Score: ${Math.round(applicant.atsScore || 0)} / 100. Match Score: ${Math.round(applicant.matchScore)}%.`
                            : "Score is pending analysis. Please check back later."}
                    </p>
                </section>
            `
            : "";

        return `
            <article class="applicant-card" data-application-id="${window.jobsApp.escapeHtml(applicant.applicationId)}">
                <div class="card-header-row">
                    <div style="display:flex;gap:.75rem;align-items:center;min-width:0;flex:1;">
                        <div class="job-card-logo" style="width:42px;height:42px;font-size:.9rem;flex-shrink:0;">${(applicant.candidateName || "C").charAt(0).toUpperCase()}</div>
                        <div style="min-width:0;">
                            <h3 style="word-break:break-word;">${window.jobsApp.escapeHtml(applicant.candidateName)}</h3>
                            <p class="subtle-text" style="word-break:break-all;">${window.jobsApp.escapeHtml(applicant.candidateEmail)}</p>
                        </div>
                    </div>
                    <span class="status-pill ${window.jobsApp.escapeHtml(statusMeta.className)}">${window.jobsApp.escapeHtml(statusMeta.label)}</span>
                </div>

                <p class="job-company">${window.jobsApp.escapeHtml(applicant.jobTitle)} | ${window.jobsApp.escapeHtml(applicant.companyName)}</p>

                <div class="detail-list compact" style="margin:1.5rem 0;">
                    <div class="detail-item" style="border:none;padding:0;background:transparent;">
                        <dt style="margin-bottom:0.25rem;">Applied On</dt>
                        <dd style="font-size:0.95rem;">${window.jobsApp.escapeHtml(window.jobsApp.formatDate(applicant.appliedAt))}</dd>
                    </div>
                    <div class="detail-item" style="border:none;padding:0;background:transparent;">
                        <dt style="margin-bottom:0.25rem;">Interview Status</dt>
                        <dd style="font-size:0.95rem;word-break:break-word;">${window.jobsApp.escapeHtml(applicant.interviewScheduledAt ? window.jobsApp.formatDateTime(applicant.interviewScheduledAt) : "Not scheduled")}</dd>
                    </div>
                    <div class="detail-item" style="border:none;padding:0;background:transparent;">
                        <dt style="margin-bottom:0.25rem;">Match Score</dt>
                        <dd style="font-size:0.95rem;"><span class="match-badge${matchClass}">${window.jobsApp.escapeHtml(scoreText)}</span></dd>
                    </div>
                    <div class="detail-item" style="border:none;padding:0;background:transparent;">
                        <dt style="margin-bottom:0.25rem;">Resume</dt>
                        <dd style="font-size:0.95rem;">
                        ${applicant.hasResume
                            ? `<div style="display:flex;gap:0.5rem;flex-wrap:wrap;">
                                 <button type="button" class="btn btn-outline" data-action="view-resume" data-candidate-id="${window.jobsApp.escapeHtml(applicant.candidateId)}" style="padding:0.2rem 0.6rem;font-size:0.8rem;">View</button>
                                 <button type="button" class="btn btn-outline" data-action="download-resume" data-candidate-id="${window.jobsApp.escapeHtml(applicant.candidateId)}" style="padding:0.2rem 0.6rem;font-size:0.8rem;">Download</button>
                               </div>`
                            : `<strong>${window.jobsApp.escapeHtml(resumeText)}</strong>`}
                        </dd>
                    </div>
                </div>

                ${applicant.rejectionFeedback
                    ? `<p class="subtle-text"><strong>Rejection feedback:</strong> ${window.jobsApp.escapeHtml(applicant.rejectionFeedback)}</p>`
                    : ""}

                <div class="job-card-actions">
                    <button type="button" class="btn btn-outline" data-action="under-review" data-application-id="${window.jobsApp.escapeHtml(applicant.applicationId)}" ${(applicant.status === "UNDER_REVIEW" || applicant.status === "SHORTLISTED" || applicant.status === "INTERVIEW" || applicant.status === "REJECTED") ? "disabled" : ""}>
                        Under Review
                    </button>
                    <button type="button" class="btn btn-secondary" data-action="shortlist" data-application-id="${window.jobsApp.escapeHtml(applicant.applicationId)}" ${(applicant.status === "SHORTLISTED" || applicant.status === "INTERVIEW" || Boolean(applicant.interviewScheduledAt)) ? "disabled" : ""}>
                        Shortlist
                    </button>
                    <button type="button" class="btn btn-danger" data-action="reject" data-application-id="${window.jobsApp.escapeHtml(applicant.applicationId)}" ${applicant.status === "REJECTED" ? "disabled" : ""}>
                        Reject
                    </button>
                    <button type="button" class="btn btn-primary" data-action="schedule" data-application-id="${window.jobsApp.escapeHtml(applicant.applicationId)}" ${!canSchedule ? 'style="opacity:.5;pointer-events:none"' : ""}>
                        ${scheduleLabel}
                    </button>
                    <button type="button" class="btn btn-outline" data-action="score" data-application-id="${window.jobsApp.escapeHtml(applicant.applicationId)}">
                        ${applicant.showScore ? "Hide Score" : "View Score"}
                    </button>
                </div>

                ${scorePanel}
            </article>
        `;
    }

    function renderApplicants() {
        const grid = byId("applicants-grid");
        const empty = byId("applicants-empty");
        const visibleApplicants = getVisibleApplicants();

        renderCounts();

        if (!visibleApplicants.length) {
            grid.innerHTML = "";
            if (window.smartUi && window.smartUi.renderEmptyState) {
                window.smartUi.renderEmptyState(empty, {
                    title: "No applicants found",
                    description: state.selectedJobId === "all"
                        ? "Applicants will appear here once candidates apply to your recruiter-owned jobs."
                        : "No applicants match the currently selected recruiter job.",
                    illustration: "CV",
                    actionHref: state.selectedJobId === "all" ? "create-job.html" : "",
                    actionLabel: state.selectedJobId === "all" ? "Create another job" : "",
                });
            } else {
                empty.innerHTML = `
                    <div class="empty-state">
                        <h3>No applicants found</h3>
                        <p>${state.selectedJobId === "all"
                            ? "Applicants will appear here once candidates apply to your recruiter-owned jobs."
                            : "No applicants match the currently selected recruiter job."}</p>
                    </div>
                `;
            }
            empty.classList.remove("hidden");
            return;
        }

        empty.classList.add("hidden");
        grid.innerHTML = visibleApplicants.map(getApplicantCardMarkup).join("");
    }

    async function preloadMeta() {
        const tasks = state.applicants.map(async function (applicant) {
            try {
                applicant.hasResume = applicant.candidateId
                    ? await window.jobsApp.fetchResumeStatus(applicant.candidateId)
                    : false;
            } catch (error) {
                applicant.hasResume = false;
            } finally {
                applicant.resumeLoading = false;
            }

            if (state.mode === "scores") {
                applicant.showScore = true;
            }
        });

        await Promise.allSettled(tasks);
        renderApplicants();

        // Auto-recalculate scores for applicants with null matchScore
        await recalculatePendingScores();
    }

    async function recalculatePendingScores() {
        const pending = state.applicants.filter(function (a) {
            return a.matchScore == null && a.applicationId;
        });

        if (!pending.length) {
            return;
        }

        const recalcTasks = pending.map(async function (applicant) {
            try {
                const result = await window.api.post(
                    `/api/applications/${applicant.applicationId}/recalculate-score`
                );
                if (result && result.matchScore != null) {
                    applicant.matchScore = result.matchScore;
                    applicant.atsScore = result.atsScore;
                }
            } catch (error) {
                // Score recalculation failed — leave as pending
            }
        });

        await Promise.allSettled(recalcTasks);
        renderApplicants();
    }

    function findApplicant(applicationId) {
        return state.applicants.find(function (applicant) {
            return String(applicant.applicationId) === String(applicationId);
        });
    }

    async function toggleScore(applicationId) {
        const applicant = findApplicant(applicationId);
        if (!applicant) {
            return;
        }

        applicant.showScore = !applicant.showScore;
        renderApplicants();
    }

    function openRejectModal(onConfirm) {
        let modalRoot = byId("reject-modal-root");
        if (!modalRoot) {
            modalRoot = document.createElement("div");
            modalRoot.id = "reject-modal-root";
            document.body.appendChild(modalRoot);
        }

        modalRoot.innerHTML = `
            <div class="modal-overlay" id="reject-overlay" style="position:fixed;inset:0;background:rgba(0,0,0,.55);z-index:1000;display:flex;align-items:center;justify-content:center;">
                <div class="modal-panel" style="background:var(--c-surface,#1e1e2e);border-radius:12px;padding:2rem;width:min(520px,92vw);">
                    <h2 style="margin-top:0;">Reject application</h2>
                    <p class="subtle-text">You can optionally add feedback for the candidate.</p>
                    <label class="field">
                        <span>Feedback</span>
                        <textarea id="reject-feedback" rows="4" style="width:100%;resize:vertical;" placeholder="Optional rejection feedback"></textarea>
                    </label>
                    <div style="display:flex;gap:1rem;justify-content:flex-end;margin-top:1.5rem;">
                        <button type="button" id="reject-cancel" class="btn btn-outline">Cancel</button>
                        <button type="button" id="reject-confirm" class="btn btn-danger">Reject</button>
                    </div>
                </div>
            </div>
        `;

        function close() {
            modalRoot.innerHTML = "";
        }

        byId("reject-cancel").addEventListener("click", close);
        byId("reject-overlay").addEventListener("click", function (event) {
            if (event.target === byId("reject-overlay")) {
                close();
            }
        });
        byId("reject-confirm").addEventListener("click", async function () {
            await onConfirm(byId("reject-feedback").value.trim(), byId("reject-confirm"), close);
        });
    }

    async function updateStatus(button, applicationId, nextStatus, rejectionFeedback) {
        const applicant = findApplicant(applicationId);
        if (!applicant) {
            return;
        }

        const loadingLabel = nextStatus === "SHORTLISTED"
            ? "Shortlisting..."
            : nextStatus === "UNDER_REVIEW"
                ? "Updating..."
                : "Rejecting...";
        window.authApp.setButtonLoading(button, loadingLabel, true);

        try {
            const response = await window.jobsApp.updateApplicationStatus(applicationId, nextStatus, rejectionFeedback);
            applicant.status = response?.status || nextStatus;
            applicant.rejectionFeedback = response?.rejectionFeedback || "";

            if (applicant.status === "REJECTED") {
                applicant.interviewScheduledAt = "";
            }

            renderApplicants();
            const label = window.jobsApp.getApplicationDisplayState(applicant).label;
            if (window.showToast) {
                window.showToast(`Application updated to ${label}.`, "success");
            } else {
                window.jobsApp.setBanner("applicants-page-message", `Application updated to ${label}.`, "success");
            }
        } catch (error) {
            if (window.showToast) {
                window.showToast(error.message || "Unable to update application status.", "error");
            } else {
                window.jobsApp.setBanner("applicants-page-message", error.message || "Unable to update application status.", "error");
            }
        } finally {
            window.authApp.setButtonLoading(button, loadingLabel, false);
        }
    }

    async function handleScheduleInterview(applicationId) {
        const applicant = findApplicant(applicationId);
        if (!applicant) {
            return;
        }

        if (!window.openScheduleModal) {
            window.location.href = "interviews.html";
            return;
        }

        window.openScheduleModal(applicant, async function (app, scheduledAt, closeModal, resetBtn) {
            try {
                const parts = scheduledAt.split('T');
                const schedulePayload = {
                    applicationId: app.applicationId,
                    interviewDate: parts[0],
                    interviewTime: parts[1].substring(0, 5),
                };
                console.log(schedulePayload);
                await window.jobsApp.scheduleInterview(schedulePayload);

                applicant.status = "INTERVIEW";
                applicant.interviewScheduledAt = scheduledAt;
                closeModal();
                renderApplicants();

                if (window.showToast) {
                    window.showToast(`Interview scheduled for ${window.jobsApp.formatDateTime(scheduledAt)}`, "success");
                }
            } catch (error) {
                if (resetBtn) {
                    resetBtn();
                }
                if (window.showToast) {
                    window.showToast(error.message || "Unable to schedule interview.", "error");
                }
            }
        });
    }

    async function downloadAndOpenResume(candidateId, action) {
        if (!candidateId) {
            return;
        }

        try {
            const download = await window.api.download(`/api/resumes/candidates/${candidateId}/download`);
            const url = window.URL.createObjectURL(download.blob);

            if (action === "view") {
                window.open(url, "_blank");
            } else {
                const link = document.createElement("a");
                link.href = url;
                link.download = download.fileName || "resume";
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
            }

            window.setTimeout(function () {
                window.URL.revokeObjectURL(url);
            }, 1000);
        } catch (error) {
            if (window.showToast) {
                window.showToast(error.message || "Resume unavailable", "error");
            }
        }
    }

    async function loadApplicantsForJob(jobId) {
        if (!jobId) {
            state.applicants = [];
            renderApplicants();
            byId("applicants-loading").classList.add("hidden");
            return;
        }

        byId("applicants-loading").classList.remove("hidden");
        const empty = byId("applicants-empty");
        const grid = byId("applicants-grid");

        if (empty) empty.classList.add("hidden");
        if (grid && window.smartUi && window.smartUi.renderSkeletonCards) {
            window.smartUi.renderSkeletonCards(grid, {
                count: 4,
                variant: "applicant"
            });
        } else if (grid) {
            grid.innerHTML = "";
        }

        try {
            const applicationsPromise = jobId === "all"
                ? window.jobsApp.fetchRecruiterApplications()
                : window.jobsApp.fetchApplicationsForJob(jobId);

            const results = await Promise.allSettled([
                applicationsPromise,
                window.jobsApp.fetchInterviewSchedules(),
            ]);

            if (results[0].status === "rejected") {
                throw results[0].reason;
            }

            const applications = results[0].value || [];
            const schedules = results[1].status === "fulfilled" ? results[1].value : [];
            const schedulesByApplicationId = new Map();
            schedules.forEach(function (schedule) {
                schedulesByApplicationId.set(String(schedule.applicationId), schedule.scheduledAt || "");
            });

            const specificJob = jobId === "all" ? null : state.jobs.find(function (job) {
                return String(job.id) === String(jobId);
            });

            state.applicants = applications.map(function (application) {
                const applicantJob = specificJob || state.jobs.find(function (job) {
                    return String(job.id) === String(application.jobId || application?.job?.id);
                });
                const applicant = window.jobsApp.normalizeApplicant(application, applicantJob);
                return Object.assign({}, applicant, {
                    interviewScheduledAt: schedulesByApplicationId.get(String(applicant.applicationId)) || applicant.interviewScheduledAt || "",
                    resumeLoading: true,
                    hasResume: false,
                    scoreLoading: state.mode === "scores",
                    score: null,
                    scoreError: "",
                    showScore: state.mode === "scores",
                });
            });

            state.applicants = window.jobsApp.sortByNewest(state.applicants, "appliedAt");
            renderApplicants();
            preloadMeta();

            if (results[1].status === "rejected") {
                window.jobsApp.setBanner("applicants-page-message", results[1].reason.message || "Interview schedule details could not be loaded.", "error");
            }
        } catch (error) {
            if (window.smartUi && window.smartUi.renderEmptyState) {
                window.smartUi.renderEmptyState("applicants-empty", {
                    title: "Applicants unavailable",
                    description: error.message || "Unable to load applicants.",
                    illustration: "!!",
                    actionLabel: "Retry",
                    onAction: function () {
                        loadApplicantsForJob(jobId);
                    }
                });
            }
            byId("applicants-empty").classList.remove("hidden");
        } finally {
            byId("applicants-loading").classList.add("hidden");
        }
    }

    async function initializeApplicantsPage() {
        if (document.body.dataset.page !== "applicants") {
            return;
        }

        const session = await window.jobsApp.requireRecruiterAccess();
        if (!session) {
            return;
        }

        state.session = session;
        state.mode = getMode();

        const urlParams = new URLSearchParams(window.location.search);
        const urlJobId = urlParams.get("jobId");
        if (urlJobId) {
            state.selectedJobId = urlJobId;
        }

        try {
            const workspace = await window.jobsApp.fetchRecruiterWorkspace(session.email);
            state.jobs = workspace.jobs;

            renderHeader();
            renderFilter();
            await loadApplicantsForJob(state.selectedJobId);
        } catch (error) {
            if (window.smartUi && window.smartUi.renderEmptyState) {
                window.smartUi.renderEmptyState("applicants-empty", {
                    title: "Applicants unavailable",
                    description: error.message || "Unable to load applicants.",
                    illustration: "!!",
                    actionLabel: "Retry",
                    onAction: initializeApplicantsPage
                });
            }
            byId("applicants-empty").classList.remove("hidden");
            byId("applicants-loading").classList.add("hidden");
        }

        byId("applicants-job-filter").addEventListener("change", function (event) {
            state.selectedJobId = event.target.value;
            loadApplicantsForJob(state.selectedJobId);
        });

        byId("applicants-grid").addEventListener("click", function (event) {
            const button = event.target.closest("[data-action]");
            if (!button) {
                return;
            }

            const applicationId = button.dataset.applicationId;

            if (button.dataset.action === "under-review") {
                updateStatus(button, applicationId, "UNDER_REVIEW");
                return;
            }

            if (button.dataset.action === "shortlist") {
                updateStatus(button, applicationId, "SHORTLISTED");
                return;
            }

            if (button.dataset.action === "reject") {
                openRejectModal(async function (feedback, confirmButton, closeModal) {
                    await updateStatus(confirmButton, applicationId, "REJECTED", feedback);
                    closeModal();
                });
                return;
            }

            if (button.dataset.action === "schedule") {
                handleScheduleInterview(applicationId);
                return;
            }

            if (button.dataset.action === "view-resume") {
                downloadAndOpenResume(button.dataset.candidateId, "view");
                return;
            }

            if (button.dataset.action === "download-resume") {
                downloadAndOpenResume(button.dataset.candidateId, "download");
                return;
            }

            if (button.dataset.action === "score") {
                toggleScore(applicationId);
            }
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        initializeApplicantsPage();
    });
})();
