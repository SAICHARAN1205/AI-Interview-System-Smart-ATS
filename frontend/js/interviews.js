(function () {
    const state = {
        session: null,
        candidates: [],
    };

    function byId(id) {
        return document.getElementById(id);
    }

    function findCandidate(applicationId) {
        return state.candidates.find(function (candidate) {
            return String(candidate.applicationId) === String(applicationId);
        });
    }

    function renderCounts() {
        const scheduledCount = state.candidates.filter(function (candidate) {
            return Boolean(candidate.scheduledAt);
        }).length;

        byId("interviews-selected-count").textContent = String(state.candidates.length);
        byId("interviews-scheduled-count").textContent = String(scheduledCount);
    }

    function getInterviewCardMarkup(candidate) {
        const scheduleLabel = candidate.scheduledAt ? "Reschedule Interview" : "Schedule Interview";
        const scheduleStatus = candidate.scheduledAt
            ? window.jobsApp.formatDateTime(candidate.scheduledAt)
            : "No interview scheduled yet";

        return `
            <article class="interview-card" data-application-id="${window.jobsApp.escapeHtml(candidate.applicationId)}">
                <div class="card-header-row">
                    <div>
                        <p class="card-kicker">Shortlisted Candidate</p>
                        <h3>${window.jobsApp.escapeHtml(candidate.candidateName)}</h3>
                        <p class="subtle-text">${window.jobsApp.escapeHtml(candidate.candidateEmail)}</p>
                    </div>
                    <span class="status-pill ${candidate.scheduledAt ? "scheduled" : "shortlisted"}">${candidate.scheduledAt ? "Scheduled" : "Shortlisted"}</span>
                </div>

                <p class="job-company">${window.jobsApp.escapeHtml(candidate.jobTitle)} • ${window.jobsApp.escapeHtml(candidate.companyName)}</p>

                <dl class="detail-list compact">
                    <div class="detail-item">
                        <dt>Status</dt>
                        <dd>${window.jobsApp.escapeHtml(candidate.status)}</dd>
                    </div>
                    <div class="detail-item">
                        <dt>Current Slot</dt>
                        <dd>${window.jobsApp.escapeHtml(scheduleStatus)}</dd>
                    </div>
                </dl>

                <div class="split-form">
                    <label class="field">
                        <span>Date</span>
                        <input type="date" data-field="date" data-application-id="${window.jobsApp.escapeHtml(candidate.applicationId)}" value="${window.jobsApp.escapeHtml(candidate.date)}">
                    </label>
                    <label class="field">
                        <span>Time</span>
                        <input type="time" data-field="time" data-application-id="${window.jobsApp.escapeHtml(candidate.applicationId)}" value="${window.jobsApp.escapeHtml(candidate.time)}">
                    </label>
                </div>

                <div class="job-card-actions">
                    <button type="button" class="btn btn-primary" data-action="schedule" data-application-id="${window.jobsApp.escapeHtml(candidate.applicationId)}">
                        ${window.jobsApp.escapeHtml(scheduleLabel)}
                    </button>
                </div>
            </article>
        `;
    }

    function renderCandidates() {
        const grid = byId("interviews-grid");
        const empty = byId("interviews-empty");

        renderCounts();

        if (!state.candidates.length) {
            grid.innerHTML = "";
            if (window.smartUi && window.smartUi.renderEmptyState) {
                window.smartUi.renderEmptyState(empty, {
                    title: "No shortlisted candidates",
                    description: "Shortlist candidates from the Applicants page first. They will appear here automatically for interview scheduling.",
                    illustration: "IV",
                    actionHref: "applicants.html",
                    actionLabel: "Open applicants"
                });
            } else {
                empty.innerHTML = `
                    <div class="empty-state">
                        <h3>No shortlisted candidates</h3>
                        <p>Shortlist candidates from the Applicants page first. They will appear here automatically for interview scheduling.</p>
                    </div>
                `;
            }
            empty.classList.remove("hidden");
            return;
        }

        empty.classList.add("hidden");
        grid.innerHTML = state.candidates.map(getInterviewCardMarkup).join("");
    }

    async function scheduleCandidate(button, applicationId) {
        const candidate = findCandidate(applicationId);

        if (!candidate) {
            return;
        }

        if (!candidate.date || !candidate.time) {
            window.jobsApp.setBanner("interviews-page-message", "Please choose both a date and time before scheduling the interview.", "error");
            return;
        }

        window.authApp.setButtonLoading(button, "Scheduling...", true);

        try {
            const schedulePayload = {
                applicationId: candidate.applicationId,
                interviewDate: candidate.date,
                interviewTime: candidate.time,
            };
            console.log(schedulePayload);
            const response = await window.jobsApp.scheduleInterview(schedulePayload);

            candidate.scheduledAt = response?.scheduledAt || response?.scheduledDateTime || `${candidate.date}T${candidate.time}`;
            const dateTimeParts = window.jobsApp.toInputDateTimeParts(candidate.scheduledAt);
            candidate.date = dateTimeParts.date;
            candidate.time = dateTimeParts.time;
            renderCandidates();
            window.jobsApp.setBanner("interviews-page-message", "Interview scheduled successfully.", "success");
        } catch (error) {
            window.jobsApp.setBanner("interviews-page-message", error.message || "Unable to schedule the interview.", "error");
        } finally {
            window.authApp.setButtonLoading(button, "Scheduling...", false);
        }
    }

    async function initializeInterviewsPage() {
        if (document.body.dataset.page !== "interviews") {
            return;
        }

        const session = await window.jobsApp.requireRecruiterAccess();

        if (!session) {
            return;
        }

        state.session = session;
        byId("interviews-user-email").textContent = session.email || "Unknown recruiter";
        byId("interviews-loading").classList.remove("hidden");
        if (window.smartUi && window.smartUi.renderSkeletonCards) {
            window.smartUi.renderSkeletonCards("interviews-grid", {
                count: 4,
                variant: "interview"
            });
        }

        try {
            const results = await Promise.allSettled([
                window.jobsApp.fetchRecruiterWorkspace(session.email),
                window.jobsApp.fetchInterviewSchedules(),
            ]);

            const workspace = results[0].status === "fulfilled"
                ? results[0].value
                : { applicants: [] };
            const schedules = results[1].status === "fulfilled" ? results[1].value : [];
            const schedulesByApplication = new Map();

            schedules.forEach(function (schedule) {
                schedulesByApplication.set(String(schedule.applicationId), schedule);
            });

            state.candidates = workspace.applicants
                .filter(function (applicant) {
                    return applicant.status === "SHORTLISTED";
                })
                .map(function (candidate) {
                    const schedule = schedulesByApplication.get(String(candidate.applicationId));
                    const parts = window.jobsApp.toInputDateTimeParts(schedule?.scheduledAt);

                    return Object.assign({}, candidate, {
                        scheduledAt: schedule?.scheduledAt || "",
                        date: parts.date,
                        time: parts.time,
                    });
                });

            renderCandidates();

            if (results[0].status === "rejected") {
                window.jobsApp.setBanner("interviews-page-message", results[0].reason.message || "Unable to load shortlisted candidates.", "error");
            }
        } catch (error) {
            if (window.smartUi && window.smartUi.renderEmptyState) {
                window.smartUi.renderEmptyState("interviews-empty", {
                    title: "Interview workflow unavailable",
                    description: error.message || "Unable to load interview scheduling data right now.",
                    illustration: "!!",
                    actionLabel: "Retry",
                    onAction: initializeInterviewsPage
                });
            }
            byId("interviews-empty").classList.remove("hidden");
        } finally {
            byId("interviews-loading").classList.add("hidden");
        }

        byId("interviews-grid").addEventListener("input", function (event) {
            const field = event.target.dataset.field;
            const applicationId = event.target.dataset.applicationId;

            if (!field || !applicationId) {
                return;
            }

            const candidate = findCandidate(applicationId);

            if (candidate) {
                candidate[field] = event.target.value;
            }
        });

        byId("interviews-grid").addEventListener("click", function (event) {
            const button = event.target.closest("[data-action='schedule']");

            if (!button) {
                return;
            }

            scheduleCandidate(button, button.dataset.applicationId);
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        initializeInterviewsPage();
    });
})();
