(function () {
    var activeFile = null;
    var selectedResumeFile = null;
    var currentResumeFile = null;
    var latestUpload = null;
    var analysisResult = null;
    var atsHistory = [];
    var candidateAnalytics = null;
    var ALLOWED_RESUME_EXTENSIONS = [".pdf", ".doc", ".docx"];
    var ALLOWED_RESUME_MIME_TYPES = [
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ];
    var RESUME_FILE_NAME_HINTS = [/resume/i, /\bcv\b/i, /curriculum/i, /profile/i];
    var SUSPICIOUS_FILE_NAME_HINTS = [/ticket/i, /invoice/i, /receipt/i, /\bbill\b/i, /screenshot/i, /screen[_ -]?shot/i, /\bscan/i, /\bphoto\b/i, /\bimage\b/i, /\bform\b/i];

    function byId(id) {
        return document.getElementById(id);
    }

    function showState(stateId) {
        document.querySelectorAll(".ats-state").forEach(function (element) {
            element.classList.add("hidden");
        });

        var target = byId(stateId);
        if (target) {
            target.classList.remove("hidden");
        }

        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    function showMessage(targetId, message, type) {
        var target = byId(targetId);
        if (!target) {
            return;
        }

        if (!message) {
            target.textContent = "";
            target.className = "message-box hidden";
            return;
        }

        target.textContent = message;
        target.className = "message-box show " + (type || "success");
    }

    function syncSelectedResumeFile(file) {
        activeFile = file || null;
        selectedResumeFile = file || null;
        currentResumeFile = file || null;
    }

    function getFileInputSelection() {
        var fileInput = byId("ats-file-input");
        if (!fileInput || !fileInput.files || !fileInput.files.length) {
            return null;
        }
        return fileInput.files[0];
    }

    function hasResumePreview() {
        var preview = byId("ats-file-preview");
        return !!(preview && preview.dataset.hasFile === "true");
    }

    function getSelectedResumeFile() {
        var file = selectedResumeFile || currentResumeFile || activeFile || getFileInputSelection();
        if (file) {
            syncSelectedResumeFile(file);
        }
        return file || null;
    }

    function renderSelectedResumePreview(file) {
        var dropZone = byId("ats-drop-zone");
        var preview = byId("ats-file-preview");
        var analyzeButton = byId("ats-analyze-btn");
        var uploadNote = byId("ats-upload-note");
        var fileName = String(file && file.name || "").toLowerCase();

        if (dropZone) {
            dropZone.classList.add("hidden");
        }

        if (preview) {
            preview.classList.remove("hidden");
            preview.dataset.hasFile = "true";
        }

        if (byId("ats-fp-name")) {
            byId("ats-fp-name").textContent = file && file.name || "resume";
        }

        if (byId("ats-fp-size")) {
            byId("ats-fp-size").textContent = formatFileSize(file && file.size || 0);
        }

        if (byId("ats-fp-icon")) {
            byId("ats-fp-icon").textContent = fileName.endsWith(".pdf") ? "PDF" : "DOC";
        }

        if (analyzeButton) {
            analyzeButton.disabled = false;
        }

        if (uploadNote) {
            uploadNote.classList.add("hidden");
        }

        showResumeFileWarning(file);
    }

    function clearSelectedResumeState(options) {
        var clearInput = !options || options.clearInput !== false;
        var preview = byId("ats-file-preview");

        syncSelectedResumeFile(null);
        latestUpload = null;
        analysisResult = null;

        if (preview) {
            preview.dataset.hasFile = "false";
        }

        if (clearInput && byId("ats-file-input")) {
            byId("ats-file-input").value = "";
        }
    }

    function resetUploadUi() {
        var dropZone = byId("ats-drop-zone");
        var preview = byId("ats-file-preview");
        var progress = byId("ats-upload-progress");
        var analyzeButton = byId("ats-analyze-btn");
        var resumeFile = getSelectedResumeFile();

        if (dropZone) {
            dropZone.classList.remove("hidden", "dragover");
        }

        if (preview) {
            preview.classList.add("hidden");
        }

        if (progress) {
            progress.classList.add("hidden");
        }

        if (analyzeButton) {
            analyzeButton.disabled = !(resumeFile || hasResumePreview());
        }

        if (resumeFile) {
            renderSelectedResumePreview(resumeFile);
        } else if (preview) {
            preview.classList.add("hidden");
            preview.dataset.hasFile = "false";
        }

        if (byId("ats-upload-note")) {
            byId("ats-upload-note").classList.toggle("hidden", !!resumeFile || hasResumePreview());
        }

        if (resumeFile) {
            showResumeFileWarning(resumeFile);
        } else {
            showMessage("ats-upload-warning", "", "warning");
        }

        showMessage("ats-upload-message", "", "success");
    }

    function goLanding() {
        showState("ats-state-landing");
    }

    function goUpload() {
        if (!window.api || !window.api.getToken()) {
            window.location.href = "login.html?redirect=ats.html";
            return;
        }

        clearSelectedResumeState({ clearInput: true });

        showState("ats-state-upload");
        renderHistoryPreview();
        resetUploadUi();
    }

    function goHistory() {
        renderHistory();
        renderHistoryChart();
        showState("ats-state-history");
    }

    function goResults() {
        renderResults();
        showState("ats-state-results");
    }

    function refreshHistoryData() {
        return window.api.get("/api/analytics/candidate").then(function (payload) {
            candidateAnalytics = payload || {};
            atsHistory = Array.isArray(candidateAnalytics.atsHistory) ? candidateAnalytics.atsHistory : [];
            renderHistoryPreview();
            return candidateAnalytics;
        }).catch(function () {
            candidateAnalytics = candidateAnalytics || {};
            atsHistory = atsHistory || [];
            renderHistoryPreview();
            return candidateAnalytics;
        });
    }

    function renderHistoryPreview() {
        var preview = byId("ats-history-preview");
        var list = byId("ats-history-preview-list");

        if (!preview || !list) {
            return;
        }

        if (!atsHistory.length) {
            preview.classList.add("hidden");
            return;
        }

        preview.classList.remove("hidden");
        list.innerHTML = atsHistory.slice(0, 3).map(function (item, index) {
            return [
                '<button type="button" class="ats-history-mini-item" data-history-index="' + index + '">',
                '<span class="ats-hmi-info">',
                '<span class="ats-hmi-name">' + escapeHtml(item.sourceFileName || "resume") + (item.targetRole ? ' (' + escapeHtml(item.targetRole) + ')' : '') + "</span>",
                '<span class="ats-hmi-time">' + escapeHtml(formatDate(item.createdAt)) + "</span>",
                "</span>",
                '<span class="ats-hmi-score">' + escapeHtml(String(item.atsScore || 0)) + "</span>",
                "</button>"
            ].join("");
        }).join("");
    }

    function loadResultFromHistory(index) {
        if (!atsHistory[index]) {
            return;
        }

        analysisResult = mapHistoryItemToResult(atsHistory[index]);
        goResults();
    }

    function handleFileDrop(event) {
        event.preventDefault();
        byId("ats-drop-zone").classList.remove("dragover");
        if (event.dataTransfer.files && event.dataTransfer.files.length > 0) {
            processSelectedFile(event.dataTransfer.files[0]);
        }
    }

    function handleFileSelect(event) {
        if (event.target.files && event.target.files.length > 0) {
            processSelectedFile(event.target.files[0]);
        }
    }

    function removeSelectedFile() {
        clearSelectedResumeState({ clearInput: true });
        resetUploadUi();
    }

    function normalizeMimeType(value) {
        return String(value || "").trim().toLowerCase();
    }

    function hasAllowedResumeExtension(fileName) {
        var normalizedName = String(fileName || "").toLowerCase();
        return ALLOWED_RESUME_EXTENSIONS.some(function (extension) {
            return normalizedName.endsWith(extension);
        });
    }

    function hasAllowedResumeMimeType(fileType) {
        var normalizedType = normalizeMimeType(fileType);
        return ALLOWED_RESUME_MIME_TYPES.indexOf(normalizedType) >= 0;
    }

    function getInvalidResumeMessage() {
        return "Please upload a PDF, DOC, or DOCX resume.";
    }

    function normalizeUploadErrorMessage(error) {
        if (window.api && typeof window.api.normalizeResumeUploadErrorMessage === "function") {
            return window.api.normalizeResumeUploadErrorMessage(error);
        }

        var message = String(error && error.message || "").trim();
        return message || "Unable to analyze the resume.";
    }

    function normalizeAtsAnalysisErrorMessage(error) {
        var fallbackMessage = "Unable to complete ATS analysis right now. Please try again shortly.";
        var message = String(error && error.message || "").trim();

        if (/resume|pdf|docx?|word|file|upload/i.test(message)) {
            return normalizeUploadErrorMessage(error);
        }

        if (window.api && typeof window.api.normalizeAiErrorMessage === "function") {
            return window.api.normalizeAiErrorMessage(error, fallbackMessage);
        }

        return message || fallbackMessage;
    }

    function getResumeFileWarningMessage(fileName) {
        var normalizedName = String(fileName || "").toLowerCase();
        var looksLikeResume = RESUME_FILE_NAME_HINTS.some(function (pattern) {
            return pattern.test(normalizedName);
        });
        var looksSuspicious = SUSPICIOUS_FILE_NAME_HINTS.some(function (pattern) {
            return pattern.test(normalizedName);
        });

        if (looksSuspicious && !looksLikeResume) {
            return "This filename looks unrelated to a resume. Please double-check that you selected the correct resume document.";
        }

        return "";
    }

    function showResumeFileWarning(file) {
        var warningMessage = getResumeFileWarningMessage(file && file.name || "");
        showMessage("ats-upload-warning", warningMessage, "warning");
    }

    function processSelectedFile(file) {
        var fileName = String(file && file.name || "").toLowerCase();
        var hasValidExtension = hasAllowedResumeExtension(fileName);
        var hasValidMimeType = hasAllowedResumeMimeType(file && file.type);

        if (!hasValidExtension && !hasValidMimeType) {
            showMessage("ats-upload-message", getInvalidResumeMessage(), "error");
            return;
        }

        if (file.size > 10 * 1024 * 1024) {
            showMessage("ats-upload-message", "File is too large. Maximum size is 10 MB.", "error");
            return;
        }

        syncSelectedResumeFile(file);
        latestUpload = null;
        showMessage("ats-upload-message", "", "success");
        showResumeFileWarning(file);
        renderSelectedResumePreview(file);
    }

    function getAnalysisInput() {
        var targetRole = byId("ats-target-role").value.trim();
        var jobDescription = byId("ats-job-description").value.trim();

        if (!targetRole) {
            throw new Error("Target role is required for ATS analysis.");
        }

        return {
            targetRole: targetRole,
            jobDescription: jobDescription
        };
    }

    function runProcessingAnimation() {
        var steps = [
            { row: byId("ats-step-1"), status: byId("ats-step-1-status"), message: "Parsing resume structure..." },
            { row: byId("ats-step-2"), status: byId("ats-step-2-status"), message: "Scanning role keywords..." },
            { row: byId("ats-step-3"), status: byId("ats-step-3-status"), message: "Checking ATS-safe formatting..." },
            { row: byId("ats-step-4"), status: byId("ats-step-4-status"), message: "Scoring alignment and gaps..." },
            { row: byId("ats-step-5"), status: byId("ats-step-5-status"), message: "Saving analytics snapshot..." }
        ];
        var subtitle = byId("ats-proc-subtitle");
        var fill = byId("ats-proc-fill");
        var percentage = byId("ats-proc-pct");

        steps.forEach(function (step, index) {
            step.row.classList.toggle("ats-step-pending", index !== 0);
            step.status.textContent = index === 0 ? "..." : "-";
        });

        subtitle.textContent = steps[0].message;
        fill.style.width = "0%";
        percentage.textContent = "0%";

        return new Promise(function (resolve) {
            var activeStep = 0;
            var progress = 0;

            var timer = window.setInterval(function () {
                progress += 4;
                fill.style.width = Math.min(progress, 100) + "%";
                percentage.textContent = Math.min(progress, 100) + "%";

                var nextStep = Math.min(steps.length - 1, Math.floor(progress / 20));
                if (nextStep !== activeStep) {
                    steps[activeStep].status.textContent = "Done";
                    activeStep = nextStep;
                    steps[activeStep].row.classList.remove("ats-step-pending");
                    steps[activeStep].status.textContent = "...";
                    subtitle.textContent = steps[activeStep].message;
                }

                if (progress >= 100) {
                    window.clearInterval(timer);
                    steps[activeStep].status.textContent = "Done";
                    subtitle.textContent = "Analysis complete.";
                    window.setTimeout(resolve, 250);
                }
            }, 120);
        });
    }

    async function analyzeResume() {
        var payload;
        var resumeFile = getSelectedResumeFile();

        console.log("[SmartATS][ATS] Selected resume before validation:", {
            fileName: resumeFile && resumeFile.name || null,
            fileType: resumeFile && resumeFile.type || null,
            fileSize: resumeFile && resumeFile.size || null,
            hasPreview: hasResumePreview()
        });

        if (!resumeFile && !hasResumePreview()) {
            showMessage("ats-upload-message", getInvalidResumeMessage(), "error");
            return;
        }

        if (!resumeFile) {
            showMessage("ats-upload-message", "Resume preview is visible, but the file reference expired. Please click Replace and select the file again.", "error");
            return;
        }

        if (!hasAllowedResumeExtension(resumeFile.name) && !hasAllowedResumeMimeType(resumeFile.type)) {
            showMessage("ats-upload-message", getInvalidResumeMessage(), "error");
            return;
        }

        try {
            payload = getAnalysisInput();
        } catch (error) {
            showMessage("ats-upload-message", error.message, "error");
            return;
        }

        byId("ats-analyze-btn").disabled = true;
        byId("ats-upload-progress").classList.remove("hidden");
        byId("ats-upload-progress-text").textContent = "Uploading resume...";
        byId("ats-up-fill").style.width = "30%";
        showMessage("ats-upload-message", "", "success");

        try {
            var formData = new FormData();
            syncSelectedResumeFile(resumeFile);
            formData.append("file", resumeFile, resumeFile.name);

            latestUpload = await window.api.upload("/api/resumes/upload", formData);
            if (window.api.refreshProfileResumeState) {
                await window.api.refreshProfileResumeState();
            }
            byId("ats-upload-progress-text").textContent = "Preparing AI analysis...";
            byId("ats-up-fill").style.width = "100%";

            showState("ats-state-processing");

            var results = await Promise.all([
                window.api.post("/api/ai/ats/analyze", payload),
                runProcessingAnimation()
            ]);

            analysisResult = mapAnalysisResult(results[0], payload, latestUpload, resumeFile);
            await refreshHistoryData();

            goResults();
        } catch (error) {
            showState("ats-state-upload");
            byId("ats-upload-progress").classList.add("hidden");
            byId("ats-analyze-btn").disabled = false;
            resetUploadUi();
            showMessage("ats-upload-message", normalizeAtsAnalysisErrorMessage(error), "error");
        }
    }

    function mapAnalysisResult(apiResult, payload, uploadData, file) {
        var matchedKeywords = normalizeArray(apiResult && apiResult.matchedKeywords);
        var missingKeywords = normalizeArray(apiResult && apiResult.missingKeywords);
        var overallScore = clampNumber(apiResult && apiResult.atsScore, 0, 100, 0);
        var keywordScore = calculateKeywordScore(matchedKeywords, missingKeywords, overallScore);
        var formattingScore = narrativeScore(apiResult && apiResult.formattingQuality, overallScore);
        var projectScore = narrativeScore(apiResult && apiResult.projectQuality, Math.max(overallScore - 5, 40));

        return {
            id: String(Date.now()),
            timestamp: new Date().toISOString(),
            filename: uploadData && uploadData.fileName || file && file.name || "resume",
            targetRole: payload.targetRole,
            overallScore: overallScore,
            passProb: scoreBand(overallScore),
            readability: summarizeReadability(apiResult && apiResult.formattingQuality, overallScore),
            summary: safeText(apiResult && apiResult.summary, "ATS analysis completed."),
            atsCompatibility: safeText(apiResult && apiResult.atsCompatibility, "ATS compatibility available."),
            formattingQuality: safeText(apiResult && apiResult.formattingQuality, "Formatting feedback available."),
            projectQuality: safeText(apiResult && apiResult.projectQuality, "Project feedback available."),
            fallbackUsed: !!(apiResult && apiResult.fallbackUsed),
            resumeValidationStatus: safeText(apiResult && apiResult.resumeValidationStatus, ""),
            resumeConfidenceScore: clampNumber(apiResult && apiResult.resumeConfidenceScore, 0, 100, 0),
            message: safeText(apiResult && apiResult.message, ""),
            strengths: normalizeArray(apiResult && apiResult.strengths),
            weaknesses: normalizeArray(apiResult && apiResult.weaknesses),
            suggestions: normalizeArray(apiResult && (apiResult.optimizationTips || apiResult.optimizationFeedback)).map(mapSuggestion),
            scores: [
                { name: "ATS Score", score: overallScore, desc: safeText(apiResult && apiResult.atsCompatibility, "Overall ATS compatibility for this resume.") },
                { name: "Keyword Match", score: keywordScore, desc: matchedKeywords.length ? matchedKeywords.length + " tracked keywords were found." : "Keyword coverage is limited and should be improved." },
                { name: "Formatting", score: formattingScore, desc: safeText(apiResult && apiResult.formattingQuality, "Formatting feedback available.") },
                { name: "Projects", score: projectScore, desc: safeText(apiResult && apiResult.projectQuality, "Project feedback available.") }
            ],
            keywords: {
                found: matchedKeywords,
                missing: missingKeywords,
                recommended: missingKeywords.slice(0, 6)
            }
        };
    }

    function mapHistoryItemToResult(item) {
        return {
            id: String(item.id || Date.now()),
            timestamp: item.createdAt || new Date().toISOString(),
            filename: item.sourceFileName || "resume",
            targetRole: item.targetRole || "Software Engineer",
            overallScore: clampNumber(item.atsScore, 0, 100, 0),
            passProb: scoreBand(item.atsScore),
            readability: summarizeReadability(item.formattingQuality, item.atsScore),
            summary: safeText(item.summary, "ATS analysis completed."),
            atsCompatibility: safeText(item.atsCompatibility, "ATS compatibility available."),
            formattingQuality: safeText(item.formattingQuality, "Formatting feedback available."),
            projectQuality: safeText(item.projectQuality, "Project feedback available."),
            fallbackUsed: !!item.fallbackUsed,
            resumeValidationStatus: "",
            resumeConfidenceScore: 0,
            message: safeText(item.message, ""),
            strengths: normalizeArray(item.strengths),
            weaknesses: normalizeArray(item.weaknesses),
            suggestions: normalizeArray(item.optimizationTips).map(mapSuggestion),
            scores: [
                { name: "ATS Score", score: clampNumber(item.atsScore, 0, 100, 0), desc: safeText(item.atsCompatibility, "Overall ATS compatibility for this resume.") },
                { name: "Keyword Match", score: clampNumber(item.keywordCoverageScore, 0, 100, 0), desc: "Keyword coverage based on tracked ATS snapshots." },
                { name: "Formatting", score: clampNumber(item.formattingScore, 0, 100, 0), desc: safeText(item.formattingQuality, "Formatting feedback available.") },
                { name: "Projects", score: clampNumber(item.projectScore, 0, 100, 0), desc: safeText(item.projectQuality, "Project feedback available.") }
            ],
            keywords: {
                found: normalizeArray(item.matchedKeywords),
                missing: normalizeArray(item.missingKeywords),
                recommended: normalizeArray(item.missingKeywords).slice(0, 6)
            }
        };
    }

    function renderBreakdownChart(result) {
        window.analyticsCharts.renderBarChart(byId("ats-breakdown-donut"), result.scores.map(function (score) {
            return {
                label: score.name,
                value: score.score,
                meta: score.desc
            };
        }), {
            emptyMessage: "Breakdown analytics will appear after analysis."
        });
    }

    function renderResults() {
        var result = analysisResult;
        if (!result) {
            goUpload();
            return;
        }

        byId("ats-pass-prob").textContent = result.passProb;
        byId("ats-readability").textContent = result.readability;
        byId("ats-result-filename").textContent = result.filename;
        byId("ats-result-time").textContent = formatDateTime(result.timestamp);
        byId("ats-target-role-result").textContent = result.targetRole || "Not specified";
        byId("ats-overall-score").textContent = Math.round(result.overallScore || 0);
        byId("ats-analysis-summary").textContent = result.summary;
        showMessage(
            "ats-analysis-message",
            result.message || "",
            result.message ? "warning" : (result.fallbackUsed ? "warning" : "success")
        );

        var ring = byId("ats-score-ring-circle");
        ring.style.strokeDashoffset = "427";
        ring.style.stroke = getScoreColor(result.overallScore);
        window.setTimeout(function () {
            ring.style.strokeDashoffset = 427 * (1 - ((result.overallScore || 0) / 100));
        }, 100);

        var verdictPill = byId("ats-verdict-pill");
        var verdictText = byId("ats-verdict-text");
        if (result.overallScore >= 80) {
            verdictPill.textContent = "Excellent";
            verdictPill.style.background = "rgba(31,122,77,0.12)";
            verdictPill.style.color = "#1f7a4d";
            verdictText.textContent = "Your resume is strongly aligned for ATS review.";
        } else if (result.overallScore >= 65) {
            verdictPill.textContent = "Good";
            verdictPill.style.background = "rgba(180,122,26,0.12)";
            verdictPill.style.color = "#b47a1a";
            verdictText.textContent = "Your resume has a solid foundation with a few gaps to close.";
        } else {
            verdictPill.textContent = "Needs Work";
            verdictPill.style.background = "rgba(191,52,52,0.12)";
            verdictPill.style.color = "#bf3434";
            verdictText.textContent = "Your resume needs clearer ATS optimization before applying broadly.";
        }

        byId("ats-breakdown-grid").innerHTML = result.scores.map(function (score) {
            return [
                '<div class="ats-bk-card">',
                '<div class="ats-bk-header">',
                '<span class="ats-bk-title">' + escapeHtml(score.name) + "</span>",
                '<span class="ats-bk-score" style="color:' + getScoreColor(score.score) + '">' + Math.round(score.score) + "%</span>",
                "</div>",
                '<div class="ats-bk-bar"><div class="ats-bk-fill" style="width:' + Math.round(score.score) + '%;background:' + getScoreColor(score.score) + '"></div></div>',
                '<p class="ats-bk-desc">' + escapeHtml(score.desc) + "</p>",
                "</div>"
            ].join("");
        }).join("");

        byId("ats-suggestions-list").innerHTML = result.suggestions.length ? result.suggestions.map(function (suggestion) {
            return [
                '<div class="ats-sugg-item">',
                '<div class="ats-sugg-body">',
                '<div class="ats-sugg-header">',
                '<span class="ats-sugg-title">' + escapeHtml(suggestion.title) + "</span>",
                '<span class="ats-sugg-badge ' + escapeHtml(suggestion.type) + '">' + escapeHtml(suggestion.type) + "</span>",
                "</div>",
                '<p class="ats-sugg-desc">' + escapeHtml(suggestion.desc) + "</p>",
                "</div>",
                "</div>"
            ].join("");
        }).join("") : '<div class="ats-sugg-item"><div class="ats-sugg-body"><p class="ats-sugg-desc">No optimization tips are available yet.</p></div></div>';

        renderBulletList("ats-strengths-list", result.strengths, "No strengths returned.");
        renderBulletList("ats-weaknesses-list", result.weaknesses, "No weaknesses returned.");
        renderChips("ats-missing-keywords", result.keywords.missing);
        renderChips("ats-found-keywords", result.keywords.found);
        renderChips("ats-recommended-skills", result.keywords.recommended);

        renderBreakdownChart(result);
        renderResultsAnalytics();
    }

    function renderResultsAnalytics() {
        var historySeries = candidateAnalytics && candidateAnalytics.atsScoreHistory || [];
        var breakdownSeries = candidateAnalytics && candidateAnalytics.atsBreakdown || [];
        var heatmapSeries = candidateAnalytics && candidateAnalytics.atsHeatmap || [];

        window.analyticsCharts.renderLineChart(byId("ats-history-chart"), historySeries, {
            title: "ATS history chart",
            emptyMessage: "Your ATS trend will appear after you complete at least one analysis."
        });
        window.analyticsCharts.renderDonutChart(byId("ats-breakdown-donut"), breakdownSeries.map(function (item) {
            return {
                label: item.label,
                value: item.value,
                percentage: item.value,
                tone: item.value >= 80 ? "success" : item.value >= 65 ? "warning" : "danger"
            };
        }), {
            centerLabel: "Breakdown",
            emptyMessage: "A recent ATS analysis is needed for the breakdown view."
        });
        window.analyticsCharts.renderHeatmap(byId("ats-heatmap-chart"), heatmapSeries, {
            emptyMessage: "Heatmap will appear after the next ATS analysis."
        });
    }

    function renderHistory() {
        var list = byId("ats-history-list");
        var empty = byId("ats-history-empty");

        if (!atsHistory.length) {
            list.innerHTML = "";
            empty.classList.remove("hidden");
            return;
        }

        empty.classList.add("hidden");
        list.innerHTML = atsHistory.map(function (item, index) {
            return [
                '<button type="button" class="ats-hist-card" data-history-index="' + index + '">',
                '<span class="ats-hist-info">',
                '<span class="ats-hist-filename">' + escapeHtml(item.sourceFileName || "resume") + "</span>",
                '<span class="ats-hist-meta">' + escapeHtml(formatDateTime(item.createdAt)) + " | " + escapeHtml(item.targetRole || "Role not set") + "</span>",
                "</span>",
                '<span class="ats-hist-score"><span><span class="ats-hist-score-label">ATS Score</span><span class="ats-hist-score-val" style="color:' + getScoreColor(item.atsScore) + '">' + Math.round(item.atsScore || 0) + "</span></span></span>",
                "</button>"
            ].join("");
        }).join("");
    }

    function renderHistoryChart() {
        window.analyticsCharts.renderLineChart(byId("ats-history-page-chart"), candidateAnalytics && candidateAnalytics.atsScoreHistory || [], {
            title: "ATS timeline",
            emptyMessage: "Timeline analytics will appear after your first ATS run."
        });
    }

    function clearHistory() {
        showMessage("ats-upload-message", "Backend ATS history is preserved for analytics and cannot be cleared from this screen.", "error");
    }

    async function downloadReport() {
        console.log("[ATS Export] Export button clicked");
        if (!analysisResult) {
            showMessage("ats-analysis-message", "No analysis result found to download.", "error");
            return;
        }

        var btn = byId("ats-download-btn");
        var originalText = btn.textContent;
        btn.textContent = "Generating PDF...";
        btn.disabled = true;

        var element = byId("ats-report-container");
        console.log("[ATS Export] Target element found:", !!element);

        if (!element) {
            console.error("Export container not found.");
            btn.textContent = originalText;
            btn.disabled = false;
            return;
        }

        console.log("[ATS Export] PDF generation started");

        try {
            // Wait for DOM and charts to be fully rendered
            await new Promise(function(resolve) { setTimeout(resolve, 800); });

            var opt = {
                margin:       0.5,
                filename:     "ATS_Report_" + (analysisResult.filename || "Candidate") + ".pdf",
                image:        { type: "jpeg", quality: 0.98 },
                html2canvas:  { scale: 2, useCORS: true, logging: true },
                jsPDF:        { unit: "in", format: "letter", orientation: "portrait" }
            };

            await html2pdf().set(opt).from(element).save();
            console.log("[ATS Export] PDF generation success");
        } catch (error) {
            console.error("[ATS Export] Error generating PDF:", error);
            console.error(error);
            showMessage("ats-analysis-message", "Error downloading PDF report.", "error");
        } finally {
            btn.textContent = originalText;
            btn.disabled = false;
        }
    }

    function formatFileSize(bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1048576) {
            return (bytes / 1024).toFixed(1) + " KB";
        }
        return (bytes / 1048576).toFixed(1) + " MB";
    }

    function normalizeArray(value) {
        if (!Array.isArray(value)) {
            return [];
        }

        return value.map(function (item) {
            return safeText(item, "");
        }).filter(Boolean);
    }

    function safeText(value, fallback) {
        var text = String(value == null ? "" : value).trim();
        return text || fallback;
    }

    function clampNumber(value, min, max, fallback) {
        var parsed = Number(value);
        if (Number.isNaN(parsed)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, Math.round(parsed)));
    }

    function calculateKeywordScore(matchedKeywords, missingKeywords, fallback) {
        var total = matchedKeywords.length + missingKeywords.length;
        return total ? Math.round((matchedKeywords.length * 100) / total) : Math.max(55, Math.min(fallback, 85));
    }

    function scoreBand(score) {
        var safeScore = clampNumber(score, 0, 100, 0);
        if (safeScore >= 80) {
            return "High";
        }
        if (safeScore >= 65) {
            return "Medium";
        }
        return "Low";
    }

    function summarizeReadability(formattingQuality, overallScore) {
        var normalized = String(formattingQuality || "").toLowerCase();
        if (normalized.indexOf("high") >= 0 || normalized.indexOf("readable") >= 0 || overallScore >= 85) {
            return "Excellent";
        }
        if (normalized.indexOf("moderate") >= 0 || overallScore >= 65) {
            return "Good";
        }
        return "Needs Work";
    }

    function narrativeScore(text, fallback) {
        var normalized = String(text || "").toLowerCase();
        if (normalized.indexOf("high") >= 0 || normalized.indexOf("strong") >= 0 || normalized.indexOf("excellent") >= 0) {
            return 88;
        }
        if (normalized.indexOf("moderate") >= 0 || normalized.indexOf("serviceable") >= 0 || normalized.indexOf("good") >= 0) {
            return 72;
        }
        if (normalized.indexOf("low") >= 0 || normalized.indexOf("weak") >= 0 || normalized.indexOf("needs") >= 0) {
            return 52;
        }
        return clampNumber(fallback, 0, 100, 70);
    }

    function mapSuggestion(text) {
        var cleaned = safeText(text, "Resume improvement suggestion.");
        var parts = cleaned.split(":");
        var title = parts.length > 1 ? parts.shift().trim() : inferSuggestionTitle(cleaned);
        var description = parts.length > 0 ? parts.join(":").trim() : cleaned;

        return {
            type: inferSuggestionType(cleaned),
            title: title,
            desc: description
        };
    }

    function inferSuggestionTitle(text) {
        if (/keyword/i.test(text)) {
            return "Strengthen Keyword Coverage";
        }
        if (/format|layout|section/i.test(text)) {
            return "Improve ATS Formatting";
        }
        if (/project/i.test(text)) {
            return "Upgrade Project Detail";
        }
        if (/metric|impact|quantif/i.test(text)) {
            return "Quantify Impact";
        }
        return "Optimization Tip";
    }

    function inferSuggestionType(text) {
        if (/missing|must|required|critical|remove|simplif/i.test(text)) {
            return "critical";
        }
        if (/improve|tailor|quantif|clarif|update|expand/i.test(text)) {
            return "medium";
        }
        return "optional";
    }

    function renderBulletList(targetId, items, fallbackText) {
        var target = byId(targetId);
        target.innerHTML = (items && items.length ? items : [fallbackText]).map(function (item) {
            return "<li>" + escapeHtml(item) + "</li>";
        }).join("");
    }

    function renderChips(targetId, items) {
        var target = byId(targetId);
        if (!items || !items.length) {
            target.innerHTML = '<span class="ats-empty-chip">None detected</span>';
            return;
        }

        target.innerHTML = items.map(function (item) {
            return '<span class="ats-kw-chip">' + escapeHtml(item) + "</span>";
        }).join("");
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function getScoreColor(score) {
        if (score >= 80) {
            return "#1f7a4d";
        }
        if (score >= 65) {
            return "#b47a1a";
        }
        return "#bf3434";
    }

    function formatDate(value) {
        var date = new Date(value);
        return Number.isNaN(date.getTime()) ? "Unknown" : date.toLocaleDateString();
    }

    function formatDateTime(value) {
        var date = new Date(value);
        return Number.isNaN(date.getTime()) ? "Unknown" : date.toLocaleString();
    }

    function bindEvents() {
        var dropZone = byId("ats-drop-zone");
        var fileInput = byId("ats-file-input");

        if (dropZone && fileInput) {
            dropZone.addEventListener("click", function () {
                fileInput.click();
            });
            dropZone.addEventListener("dragover", function (event) {
                event.preventDefault();
                dropZone.classList.add("dragover");
            });
            dropZone.addEventListener("dragleave", function () {
                dropZone.classList.remove("dragover");
            });
            dropZone.addEventListener("drop", handleFileDrop);
            fileInput.addEventListener("change", handleFileSelect);
        }

        byId("ats-analyze-btn").addEventListener("click", analyzeResume);
        byId("ats-remove-btn").addEventListener("click", removeSelectedFile);
        byId("ats-clear-history-btn").addEventListener("click", clearHistory);
        byId("ats-history-preview-list").addEventListener("click", function (event) {
            var button = event.target.closest("[data-history-index]");
            if (button) {
                loadResultFromHistory(Number(button.dataset.historyIndex));
            }
        });
        byId("ats-history-list").addEventListener("click", function (event) {
            var button = event.target.closest("[data-history-index]");
            if (button) {
                loadResultFromHistory(Number(button.dataset.historyIndex));
            }
        });
    }

    async function initAtsPage() {
        if (document.body.dataset.page !== "ats") {
            return;
        }

        bindEvents();

        if (window.api && window.api.getToken()) {
            var role = window.api.getRole();
            if (String(role || "").toUpperCase() === "RECRUITER") {
                window.location.href = "recruiter.html";
                return;
            }

            await refreshHistoryData();
            goLanding();
            return;
        }

        goLanding();
    }

    window.atsApp = {
        goLanding: goLanding,
        goUpload: goUpload,
        goHistory: goHistory,
        loadHistoryResult: loadResultFromHistory,
        downloadReport: downloadReport
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initAtsPage);
    } else {
        initAtsPage();
    }
})();
