(function () {
    const ACTIVE_SESSION_KEY = "smartats_active_interview_session";
    const DEFAULT_SKILLS = [
        "Java",
        "Spring Boot",
        "React",
        "JavaScript",
        "SQL",
        "REST API",
        "Problem Solving",
        "Communication",
    ];

    const state = {
        session: null,
        sessionId: null,
        sessionStarted: false,
        currentIndex: 0,
        answers: {},
        selectedSkills: new Set(["Problem Solving", "Communication"]),
        roleOptions: [],
        elapsedSeconds: 0,
        timerId: null,
        autosaveId: null,
        saveInFlight: false,
        result: null,
        timedOut: false,
    };

    function byId(id) {
        return document.getElementById(id);
    }

    function showElement(element, shouldShow) {
        if (element) {
            element.classList.toggle("hidden", !shouldShow);
        }
    }

    function showOnly(panelId) {
        [
            "interview-loading-panel",
            "interview-setup-panel",
            "interview-generating-panel",
            "interview-instructions-panel",
            "interview-live-panel",
            "interview-submitting-panel",
            "interview-results-panel",
        ].forEach(function (id) {
            showElement(byId(id), id === panelId);
        });
    }

    function escapeHtml(value) {
        if (window.jobsApp && window.jobsApp.escapeHtml) {
            return window.jobsApp.escapeHtml(value);
        }

        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function setBanner(target, message, type) {
        const element = typeof target === "string" ? byId(target) : target;

        if (!element) {
            return;
        }

        if (!message) {
            element.textContent = "";
            element.className = element.className.replace(/\bshow\b|\bsuccess\b|\berror\b|\bwarning\b/g, "").trim() + " hidden";
            return;
        }

        element.textContent = message;
        element.className = element.className
            .replace(/\bhidden\b|\bshow\b|\bsuccess\b|\berror\b|\bwarning\b/g, "")
            .trim();
        element.classList.add("show", type || "success");
    }

    function clearBanner(target) {
        setBanner(target, "", "success");
    }

    function normalizeAiErrorMessage(error, fallbackMessage) {
        if (window.api && typeof window.api.normalizeAiErrorMessage === "function") {
            return window.api.normalizeAiErrorMessage(error, fallbackMessage);
        }

        return String(error && error.message || "").trim() || fallbackMessage;
    }

    function formatTime(totalSeconds) {
        const safeSeconds = Math.max(0, Math.floor(totalSeconds || 0));
        const minutes = String(Math.floor(safeSeconds / 60)).padStart(2, "0");
        const seconds = String(safeSeconds % 60).padStart(2, "0");
        return `${minutes}:${seconds}`;
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

    function parseSkillList(value) {
        return String(value || "")
            .split(",")
            .map(function (skill) {
                return skill.trim();
            })
            .filter(Boolean);
    }

    function normalizeApplications(applications) {
        return applications.map(function (application) {
            return {
                type: "application",
                applicationId: application.applicationId || application.id,
                jobId: application.jobId,
                title: application.jobTitle || "Untitled role",
                company: application.companyName || "Unknown company",
                skills: parseSkillList(application.skills),
                description: application.description || "",
            };
        });
    }

    function normalizeJobs(jobs) {
        return jobs.map(function (job) {
            return {
                type: "job",
                jobId: job.id,
                applicationId: null,
                title: job.title || "Untitled role",
                company: job.companyName || "Unknown company",
                skills: parseSkillList(job.skills || job.roleSkills),
                description: job.description || "",
            };
        });
    }

    function getSelectedRoleOption() {
        const select = byId("interview-job-select");
        const value = select ? select.value : "";

        if (value === "custom") {
            return {
                type: "custom",
                title: byId("interview-custom-role").value.trim(),
                skills: Array.from(state.selectedSkills),
            };
        }

        return state.roleOptions.find(function (option) {
            return option.value === value;
        }) || null;
    }

    function getSelectedInterviewType() {
        const input = document.querySelector("input[name='interview-type']:checked");
        return input ? input.value : "Mixed";
    }

    function getSelectedMonitoringMode() {
        const input = document.querySelector("input[name='monitoring-mode']:checked");
        return input ? input.value : "Camera + Voice";
    }

    function getSelectedSkills() {
        return Array.from(state.selectedSkills);
    }

    function setHeroStatus(status) {
        const heroStatus = byId("interview-hero-status");

        if (heroStatus) {
            heroStatus.textContent = status;
        }
    }

    function setElapsed(seconds) {
        state.elapsedSeconds = Math.max(0, Math.floor(seconds || 0));
        byId("interview-hero-elapsed").textContent = formatTime(state.elapsedSeconds);
        byId("live-elapsed-time").textContent = `Elapsed ${formatTime(state.elapsedSeconds)}`;
    }

    function addSkill(skill) {
        const cleaned = String(skill || "").trim();

        if (cleaned) {
            const lowerCleaned = cleaned.toLowerCase();
            const exists = Array.from(state.selectedSkills).some(function (s) {
                return s.toLowerCase() === lowerCleaned;
            });
            if (!exists) {
                state.selectedSkills.add(cleaned);
            }
        }
    }

    function renderSkillPicker() {
        const container = byId("interview-skill-picker");

        if (!container) {
            return;
        }

        const allSkills = Array.from(new Set(DEFAULT_SKILLS.concat(Array.from(state.selectedSkills))));
        container.innerHTML = allSkills.map(function (skill) {
            const active = state.selectedSkills.has(skill) ? " active" : "";
            return `<button type="button" class="skill-chip${active}" data-skill="${escapeHtml(skill)}">${escapeHtml(skill)}</button>`;
        }).join("");
    }

    function syncSetupPreview() {
        const duration = Number(byId("interview-duration").value || 20);
        byId("instructions-duration").textContent = `${duration} minutes`;
    }

    function populateRoleSelect(applications, jobs) {
        const select = byId("interview-job-select");
        const appOptions = normalizeApplications(applications);
        const appliedJobIds = new Set(appOptions.map(function (option) {
            return String(option.jobId);
        }));
        const jobOptions = normalizeJobs(jobs).filter(function (option) {
            return !appliedJobIds.has(String(option.jobId));
        });

        state.roleOptions = appOptions
            .concat(jobOptions)
            .map(function (option) {
                const prefix = option.type === "application" ? "application" : "job";
                return Object.assign({}, option, {
                    value: `${prefix}:${option.applicationId || option.jobId}`,
                });
            });

        const optionsMarkup = state.roleOptions.map(function (option) {
            const label = option.type === "application"
                ? `${option.title} at ${option.company} (Applied)`
                : `${option.title} at ${option.company}`;

            return `<option value="${escapeHtml(option.value)}">${escapeHtml(label)}</option>`;
        }).join("");

        select.innerHTML = `${optionsMarkup}<option value="custom">Custom role</option>`;

        const params = new URLSearchParams(window.location.search);
        const applicationId = params.get("applicationId");
        const jobId = params.get("jobId");
        const requestedValue = applicationId ? `application:${applicationId}` : jobId ? `job:${jobId}` : "";

        if (requestedValue && state.roleOptions.some(function (option) { return option.value === requestedValue; })) {
            select.value = requestedValue;
        } else if (!state.roleOptions.length) {
            select.value = "custom";
        }

        applySelectedRoleDefaults();
    }

    function applySelectedRoleDefaults() {
        const option = getSelectedRoleOption();
        const customWrap = byId("interview-custom-role-wrap");
        showElement(customWrap, byId("interview-job-select").value === "custom");

        if (option && option.type !== "custom") {
            state.selectedSkills = new Set(option.skills.length ? option.skills : ["Problem Solving", "Communication"]);
        }

        renderSkillPicker();
        syncSetupPreview();
    }

    async function loadSetupData() {
        showOnly("interview-loading-panel");
        setHeroStatus("Loading");

        try {
            const results = await Promise.allSettled([
                window.jobsApp ? window.jobsApp.fetchCandidateApplications() : window.api.get("/api/applications/candidate"),
                window.jobsApp ? window.jobsApp.fetchAllJobs() : window.api.get("/api/jobs/all?page=0&size=50"),
            ]);
            const applications = results[0].status === "fulfilled" ? normalizeCollection(results[0].value) : [];
            const jobs = results[1].status === "fulfilled" ? normalizeCollection(results[1].value) : [];

            populateRoleSelect(applications, jobs);

            if (results[0].status === "rejected") {
                setBanner("interview-page-message", "Applications could not be loaded. You can still use a custom interview role.", "error");
            }
        } catch (error) {
            populateRoleSelect([], []);
            setBanner("interview-page-message", error.message || "Unable to load setup data.", "error");
        }

        showOnly("interview-setup-panel");
        setHeroStatus("Setup");
    }

    function getSetupPayload() {
        const option = getSelectedRoleOption();
        const customRole = byId("interview-custom-role").value.trim();
        const jobRole = option?.type === "custom" ? customRole : option?.title;
        
        const skillInput = byId("interview-custom-skill");
        if (skillInput && skillInput.value.trim()) {
            addSkill(skillInput.value);
            skillInput.value = "";
            renderSkillPicker();
        }
        
        const skills = getSelectedSkills();

        if (!jobRole) {
            throw new Error("Choose a job role or enter a custom role.");
        }

        if (!skills.length) {
            throw new Error("Select at least one skill or technology.");
        }

        return {
            jobId: option?.jobId || null,
            applicationId: option?.applicationId || null,
            jobRole,
            difficulty: byId("interview-difficulty").value,
            skills,
            interviewType: getSelectedInterviewType(),
            monitoringMode: getSelectedMonitoringMode(),
            estimatedDurationMinutes: Number(byId("interview-duration").value || 20),
        };
    }

    function normalizeSession(session) {
        const answers = {};
        Object.entries(session?.answers || {}).forEach(function ([key, value]) {
            answers[Number(key)] = value || "";
        });

        return Object.assign({}, session, {
            questions: Array.isArray(session?.questions) ? session.questions : [],
            answers,
            currentQuestionIndex: session?.currentQuestionIndex || 0,
            elapsedSeconds: session?.elapsedSeconds || 0,
        });
    }

    function rememberSession(sessionId) {
        try {
            localStorage.setItem(ACTIVE_SESSION_KEY, String(sessionId));
        } catch (error) {
            // Local storage is optional.
        }

        const url = new URL(window.location.href);
        url.searchParams.set("sessionId", sessionId);
        window.history.replaceState({}, "", url.toString());
    }

    function forgetSession() {
        try {
            localStorage.removeItem(ACTIVE_SESSION_KEY);
        } catch (error) {
            // Local storage is optional.
        }
    }

    async function startInterviewSetup(event) {
        event.preventDefault();
        clearBanner("interview-page-message");

        let payload;

        try {
            payload = getSetupPayload();
        } catch (error) {
            setBanner("interview-page-message", error.message, "error");
            return;
        }

        showOnly("interview-generating-panel");
        setHeroStatus("Generating");
        window.authApp.setButtonLoading(byId("interview-start-button"), "Generating...", true);

        try {
            const session = normalizeSession(await window.api.post("/api/interview/sessions", payload));
            state.session = session;
            state.sessionId = session.id;
            state.currentIndex = 0;
            state.answers = Object.assign({}, session.answers);
            setElapsed(session.elapsedSeconds || 0);
            rememberSession(session.id);
            if (session.message) {
                setBanner("interview-page-message", session.message, session.fallbackUsed ? "warning" : "success");
            }
            renderInstructions();
        } catch (error) {
            showOnly("interview-setup-panel");
            setHeroStatus("Setup");
            setBanner(
                "interview-page-message",
                normalizeAiErrorMessage(error, "Unable to generate interview questions."),
                "error"
            );
        } finally {
            window.authApp.setButtonLoading(byId("interview-start-button"), "Generating...", false);
        }
    }

    function renderInstructions() {
        const session = state.session;
        byId("instructions-duration").textContent = `${session.estimatedDurationMinutes || 20} minutes`;
        showOnly("interview-instructions-panel");
        setHeroStatus("Instructions");
    }

    function beginLiveInterview() {
        if (!state.session || !state.session.questions.length) {
            setBanner("interview-page-message", "This interview session has no questions. Please retry setup.", "error");
            showOnly("interview-setup-panel");
            return;
        }

        // Gate on device permissions
        if (window.interviewMonitor && !window.interviewMonitor.isReady()) {
            setBanner("im-device-warning", "Camera and microphone access are required. Please click Check Devices and grant permissions.", "error");
            return;
        }

        state.sessionStarted = true;
        state.currentIndex = Math.min(state.session.currentQuestionIndex || 0, state.session.questions.length - 1);
        setHeroStatus("Live");
        showOnly("interview-live-panel");
        renderLiveInterview();
        startTimer();

        // Start monitoring
        if (window.interviewMonitor) {
            window.interviewMonitor.startLiveMonitoring(state.sessionId);
        }
    }

    function startTimer() {
        stopTimer();
        state.timerId = window.setInterval(function () {
            setElapsed(state.elapsedSeconds + 1);
            updateCountdown();
        }, 1000);
        updateCountdown();
    }

    function stopTimer() {
        if (state.timerId) {
            window.clearInterval(state.timerId);
            state.timerId = null;
        }
    }

    function updateCountdown() {
        const totalSeconds = (state.session?.estimatedDurationMinutes || 20) * 60;
        const remaining = Math.max(0, totalSeconds - state.elapsedSeconds);
        byId("live-countdown").textContent = `${formatTime(remaining)} left`;

        if (remaining <= 0 && state.sessionStarted && !state.timedOut) {
            state.timedOut = true;
            setBanner("live-session-message", "Time is up. Submitting the answers you completed.", "error");
            submitInterview(true);
        }
    }

    function getAnswer(index) {
        return state.answers[index] || "";
    }

    function getAnsweredCount() {
        return (state.session?.questions || []).filter(function (_, index) {
            return getAnswer(index).trim().length > 0;
        }).length;
    }

    function renderLiveInterview() {
        const questions = state.session.questions;
        const index = state.currentIndex;
        const answer = getAnswer(index);
        const answeredCount = getAnsweredCount();
        const progress = Math.round(((index + 1) / questions.length) * 100);

        byId("live-interview-title").textContent = `${state.session.jobRole} ${state.session.interviewType} Interview`;
        const modeLabel = state.session.monitoringMode || "Camera + Voice";
        byId("live-monitoring-mode").textContent = modeLabel;
        byId("live-question-number").textContent = `Question ${index + 1} of ${questions.length}`;
        byId("live-question-text").textContent = questions[index];
        byId("live-answer").value = answer;
        byId("live-question-status").textContent = answer.trim() ? "Answered" : "Pending";
        byId("live-progress-fill").style.width = `${progress}%`;
        byId("live-progress-summary").textContent = `${answeredCount} of ${questions.length} answered`;
        byId("live-prev-button").disabled = index === 0;
        showElement(byId("live-next-button"), index < questions.length - 1);
        showElement(byId("live-submit-button"), index === questions.length - 1);
        renderQuestionStatusGrid();
        clearBanner("live-session-message");
    }

    function renderQuestionStatusGrid() {
        const container = byId("question-status-grid");

        container.innerHTML = state.session.questions.map(function (_, index) {
            const isCurrent = index === state.currentIndex;
            const answered = getAnswer(index).trim().length > 0;
            const className = isCurrent ? " current" : answered ? " answered" : "";
            const label = answered ? "Answered" : "Pending";

            return `
                <button type="button" class="question-status-button${className}" data-question-index="${index}">
                    <strong>${index + 1}</strong>
                    <span>${escapeHtml(label)}</span>
                </button>
            `;
        }).join("");
    }

    function updateAnswer(value) {
        state.answers[state.currentIndex] = value;
        byId("live-question-status").textContent = value.trim() ? "Answered" : "Pending";
        byId("live-progress-summary").textContent = `${getAnsweredCount()} of ${state.session.questions.length} answered`;
        renderQuestionStatusGrid();
        byId("live-save-status").textContent = "Unsaved changes";

        if (state.autosaveId) {
            window.clearTimeout(state.autosaveId);
        }

        state.autosaveId = window.setTimeout(function () {
            saveCurrentAnswer();
        }, 700);
    }

    async function saveCurrentAnswer() {
        if (!state.sessionId || state.saveInFlight || state.session?.status === "COMPLETED") {
            return;
        }

        state.saveInFlight = true;
        byId("live-save-status").textContent = "Saving...";

        try {
            const monPayload = window.interviewMonitor ? window.interviewMonitor.getMonitoringPayload() : {};
            const updated = normalizeSession(await window.api.put(`/api/interview/sessions/${state.sessionId}/answers`, Object.assign({
                questionIndex: state.currentIndex,
                answer: getAnswer(state.currentIndex),
                currentQuestionIndex: state.currentIndex,
                elapsedSeconds: state.elapsedSeconds,
            }, monPayload)));
            state.session = Object.assign({}, state.session, updated);
            state.answers = Object.assign({}, state.answers, updated.answers);
            byId("live-save-status").textContent = "Saved";
        } catch (error) {
            byId("live-save-status").textContent = "Save failed";
            setBanner("live-session-message", error.message || "Unable to save this answer. Check your connection and retry.", "error");
        } finally {
            state.saveInFlight = false;
        }
    }

    function validateCurrentAnswer() {
        if (!getAnswer(state.currentIndex).trim()) {
            setBanner("live-session-message", "Add an answer before moving forward. You can return and edit it later.", "error");
            byId("live-answer").focus();
            return false;
        }

        return true;
    }

    async function goToQuestion(index, requireCurrentAnswer) {
        const target = Math.max(0, Math.min(index, state.session.questions.length - 1));

        if (target === state.currentIndex) {
            return;
        }

        if (requireCurrentAnswer && target > state.currentIndex && !validateCurrentAnswer()) {
            return;
        }

        await saveCurrentAnswer();
        state.currentIndex = target;
        renderLiveInterview();
    }

    function getFirstEmptyQuestionIndex() {
        return state.session.questions.findIndex(function (_, index) {
            return !getAnswer(index).trim();
        });
    }

    async function submitInterview(allowPartial) {
        if (!state.sessionId || state.session?.status === "COMPLETED") {
            return;
        }

        if (!allowPartial) {
            const firstEmpty = getFirstEmptyQuestionIndex();

            if (firstEmpty !== -1) {
                state.currentIndex = firstEmpty;
                renderLiveInterview();
                setBanner("live-session-message", "Please answer every question before submitting the interview.", "error");
                return;
            }
        }

        stopTimer();
        state.sessionStarted = false;
        showOnly("interview-submitting-panel");
        setHeroStatus("Scoring");

        // Stop monitoring
        if (window.interviewMonitor) {
            window.interviewMonitor.stopRecording();
        }

        try {
            await saveCurrentAnswer();
            const monPayload = window.interviewMonitor ? window.interviewMonitor.getMonitoringPayload() : {};
            const response = normalizeSession(await window.api.post(`/api/interview/sessions/${state.sessionId}/submit`, Object.assign({
                questionIndex: state.currentIndex,
                answer: getAnswer(state.currentIndex),
                currentQuestionIndex: state.currentIndex,
                elapsedSeconds: state.elapsedSeconds,
            }, monPayload)));
            state.session = response;
            state.result = await window.api.get(`/api/interview/sessions/${state.sessionId}/result`);
            forgetSession();
            if (window.interviewMonitor) window.interviewMonitor.stopAll();
            renderResults();
        } catch (error) {
            showOnly("interview-live-panel");
            state.sessionStarted = true;
            startTimer();
            setHeroStatus("Live");
            setBanner(
                "live-session-message",
                normalizeAiErrorMessage(error, "Unable to submit the interview."),
                "error"
            );
        }
    }

    function renderScore(value) {
        return `${Math.round(Number(value) || 0)}%`;
    }

    function renderList(targetId, items) {
        const container = byId(targetId);
        container.innerHTML = (items && items.length ? items : ["No feedback available yet."]).map(function (item) {
            return `<li>${escapeHtml(item)}</li>`;
        }).join("");
    }

    function renderResults() {
        const result = state.result || state.session?.result || {};
        byId("results-summary").textContent = result.summary || "Your AI feedback is ready.";
        byId("result-overall-score").textContent = renderScore(result.overallScore);
        byId("result-communication-score").textContent = renderScore(result.communicationScore);
        byId("result-technical-score").textContent = renderScore(result.technicalScore);

        // Monitoring scores
        const confEl = byId("result-confidence-score");
        const fluEl = byId("result-fluency-score");
        if (confEl) confEl.textContent = renderScore(result.confidenceScore);
        if (fluEl) fluEl.textContent = renderScore(result.fluencyScore);

        // Integrity section
        const intStatus = byId("result-integrity-status");
        if (intStatus) {
            const status = result.integrityStatus || "Clean";
            intStatus.textContent = status;
            intStatus.className = "im-integrity-badge im-badge-" + status.toLowerCase();
        }
        const tabEl = byId("result-tab-switches");
        const faceEl = byId("result-face-warnings");
        const suspEl = byId("result-suspicious-count");
        if (tabEl) tabEl.textContent = String(result.tabSwitchCount || 0);
        if (faceEl) faceEl.textContent = String(result.faceWarningCount || 0);
        if (suspEl) suspEl.textContent = String(result.suspiciousActivityCount || 0);

        renderList("result-strengths", result.strengths);
        renderList("result-weaknesses", result.weaknesses);
        renderList("result-suggestions", result.improvementSuggestions);
        byId("result-communication-evaluation").textContent = result.communicationEvaluation || "Communication analysis is not available yet.";
        byId("result-technical-relevance").textContent = result.technicalRelevance || "Technical relevance analysis is not available yet.";
        showOnly("interview-results-panel");
        setHeroStatus("Complete");
        setBanner("interview-page-message", result.message || "", result.fallbackUsed ? "warning" : "success");
    }

    function downloadFeedback() {
        if (!state.sessionId || !window.api || !window.api.download) {
            setBanner("interview-page-message", "Interview PDF report is not available right now.", "error");
            return;
        }

        window.api.download(`/api/interview/sessions/${state.sessionId}/report.pdf`).catch(function () {
            setBanner("interview-page-message", "Failed to download interview PDF report.", "error");
        });
    }

    function retryInterview() {
        stopTimer();
        if (window.interviewMonitor) window.interviewMonitor.stopAll();
        state.session = null;
        state.sessionId = null;
        state.sessionStarted = false;
        state.currentIndex = 0;
        state.answers = {};
        state.result = null;
        state.elapsedSeconds = 0;
        state.timedOut = false;
        forgetSession();
        const url = new URL(window.location.href);
        url.searchParams.delete("sessionId");
        window.history.replaceState({}, "", url.toString());
        setElapsed(0);
        showOnly("interview-setup-panel");
        setHeroStatus("Setup");
    }

    async function exitInterview() {
        if (!state.sessionStarted) {
            window.location.href = "dashboard.html";
            return;
        }

        var shouldExit = window.smartUi && typeof window.smartUi.confirm === "function"
            ? await window.smartUi.confirm({
                title: "Exit interview?",
                description: "Your saved answers will remain in SmartATS, but the live timer in this browser will stop.",
                confirmLabel: "Exit interview",
                confirmClass: "btn-danger",
            })
            : window.confirm("Exit this interview? Saved answers will remain in SmartATS, but the timer will stop in this browser.");

        if (shouldExit) {
            saveCurrentAnswer().finally(function () {
                window.location.href = "dashboard.html";
            });
        }
    }

    async function resumeSessionFromUrl() {
        const params = new URLSearchParams(window.location.search);
        const sessionId = params.get("sessionId");

        if (!sessionId) {
            return false;
        }

        showOnly("interview-loading-panel");
        setHeroStatus("Loading");

        try {
            const session = normalizeSession(await window.api.get(`/api/interview/sessions/${sessionId}`));
            state.session = session;
            state.sessionId = session.id;
            state.currentIndex = session.currentQuestionIndex || 0;
            state.answers = Object.assign({}, session.answers);
            setElapsed(session.elapsedSeconds || 0);

            if (session.status === "COMPLETED") {
                state.result = session.result || await window.api.get(`/api/interview/sessions/${session.id}/result`);
                renderResults();
            } else {
                setBanner("interview-page-message", "Your saved interview session was restored.", "success");
                beginLiveInterview();
            }

            return true;
        } catch (error) {
            setBanner("interview-page-message", "Saved interview session could not be restored. Start a new session when ready.", "error");
            return false;
        }
    }

    function bindEvents() {
        byId("interview-setup-form").addEventListener("submit", startInterviewSetup);
        byId("interview-job-select").addEventListener("change", applySelectedRoleDefaults);
        byId("interview-duration").addEventListener("change", syncSetupPreview);
        byId("interview-difficulty").addEventListener("change", syncSetupPreview);
        document.querySelectorAll("input[name='interview-type']").forEach(function (input) {
            input.addEventListener("change", syncSetupPreview);
        });

        byId("interview-skill-picker").addEventListener("click", function (event) {
            const button = event.target.closest("[data-skill]");

            if (!button) {
                return;
            }

            const skill = button.dataset.skill;

            if (state.selectedSkills.has(skill)) {
                state.selectedSkills.delete(skill);
            } else {
                state.selectedSkills.add(skill);
            }

            renderSkillPicker();
        });

        byId("interview-custom-skill").addEventListener("keydown", function (event) {
            if (event.key !== "Enter") {
                return;
            }

            event.preventDefault();
            addSkill(event.target.value);
            event.target.value = "";
            renderSkillPicker();
        });

        byId("interview-custom-skill").addEventListener("blur", function (event) {
            if (event.target.value.trim()) {
                addSkill(event.target.value);
                event.target.value = "";
                renderSkillPicker();
            }
        });

        byId("interview-back-setup").addEventListener("click", function () {
            if (window.interviewMonitor) window.interviewMonitor.stopAll();
            showOnly("interview-setup-panel");
            setHeroStatus("Setup");
        });
        byId("interview-begin-button").addEventListener("click", beginLiveInterview);
        byId("interview-exit-button").addEventListener("click", exitInterview);
        byId("live-answer").addEventListener("input", function (event) {
            updateAnswer(event.target.value);
        });
        byId("live-prev-button").addEventListener("click", function () {
            if (window.interviewMonitor) { window.interviewMonitor.stopRecording(); window.interviewMonitor.resetVoiceState(); }
            goToQuestion(state.currentIndex - 1, false);
        });
        byId("live-next-button").addEventListener("click", function () {
            if (window.interviewMonitor) { window.interviewMonitor.stopRecording(); window.interviewMonitor.resetVoiceState(); }
            goToQuestion(state.currentIndex + 1, true);
        });
        byId("live-submit-button").addEventListener("click", function () {
            submitInterview(false);
        });
        byId("question-status-grid").addEventListener("click", function (event) {
            const button = event.target.closest("[data-question-index]");

            if (button) {
                if (window.interviewMonitor) { window.interviewMonitor.stopRecording(); window.interviewMonitor.resetVoiceState(); }
                goToQuestion(Number(button.dataset.questionIndex), false);
            }
        });
        byId("result-retry-button").addEventListener("click", retryInterview);
        byId("result-download-button").addEventListener("click", downloadFeedback);

        // Monitoring device check button
        const deviceCheckBtn = byId("im-device-check-btn");
        if (deviceCheckBtn) {
            deviceCheckBtn.addEventListener("click", function () {
                if (window.interviewMonitor) window.interviewMonitor.requestDevices();
            });
        }

        // Voice control buttons
        const voiceStart = byId("im-voice-start");
        const voiceStop = byId("im-voice-stop");
        const voiceRetry = byId("im-voice-retry");
        if (voiceStart) voiceStart.addEventListener("click", function () { if (window.interviewMonitor) window.interviewMonitor.startRecording(); });
        if (voiceStop) voiceStop.addEventListener("click", function () { if (window.interviewMonitor) window.interviewMonitor.stopRecording(); });
        if (voiceRetry) voiceRetry.addEventListener("click", function () { if (window.interviewMonitor) window.interviewMonitor.retryRecording(); });

        window.addEventListener("beforeunload", function (event) {
            if (!state.sessionStarted) {
                return;
            }

            event.preventDefault();
            event.returnValue = "";
        });
    }

    async function initializeInterviewPage() {
        if (document.body.dataset.page !== "mock-interview") {
            return;
        }

        bindEvents();

        const session = await window.jobsApp.requireCandidateAccess();

        if (!session) {
            return;
        }

        byId("interview-user-email").textContent = session.email || "Candidate";

        if (!(await resumeSessionFromUrl())) {
            await loadSetupData();
        }
    }

    document.addEventListener("DOMContentLoaded", initializeInterviewPage);
})();

