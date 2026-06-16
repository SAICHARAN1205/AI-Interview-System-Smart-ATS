(function () {
    function byId(id) {
        return document.getElementById(id);
    }

    function setBanner(message, type) {
        if (!window.jobsApp || !window.jobsApp.setBanner) {
            return;
        }
        window.jobsApp.setBanner("candidate-analytics-message", message, type || "error");
    }

    function clearBanner() {
        if (!window.jobsApp || !window.jobsApp.clearBanner) {
            return;
        }
        window.jobsApp.clearBanner("candidate-analytics-message");
    }

    function renderMetricCards(metrics) {
        var container = byId("candidate-metrics-grid");
        var items = Array.isArray(metrics) ? metrics : [];

        if (!container) {
            return;
        }

        if (!items.length) {
            container.innerHTML = [
                '<article class="metric-card">',
                '<p class="metric-label">Insights Pending</p>',
                '<strong class="metric-value">--</strong>',
                '<p class="metric-note">Run ATS, interviews, and applications to populate analytics.</p>',
                "</article>"
            ].join("");
            return;
        }

        container.innerHTML = items.map(function (metric) {
            return [
                '<article class="metric-card">',
                '<p class="metric-label">' + window.analyticsCharts.escapeHtml(metric.label) + "</p>",
                '<strong class="metric-value">' + window.analyticsCharts.escapeHtml(metric.value) + "</strong>",
                '<p class="metric-note">' + window.analyticsCharts.escapeHtml(metric.note || "") + "</p>",
                metric.delta ? '<p class="analytics-metric-delta ' + window.analyticsCharts.escapeHtml(metric.tone || "neutral") + '">' + window.analyticsCharts.escapeHtml(metric.delta) + "</p>" : "",
                "</article>"
            ].join("");
        }).join("");
    }

    function renderHistoryList(targetId, items, formatter, emptyMessage) {
        var container = byId(targetId);
        var records = Array.isArray(items) ? items : [];

        if (!container) {
            return;
        }

        if (!records.length) {
            container.innerHTML = [
                '<div class="analytics-list-empty">',
                '<p>' + window.analyticsCharts.escapeHtml(emptyMessage) + "</p>",
                "</div>"
            ].join("");
            return;
        }

        container.innerHTML = records.map(formatter).join("");
    }

    function renderInsights(insights) {
        var container = byId("candidate-insights-list");
        var items = Array.isArray(insights) ? insights : [];

        if (!container) {
            return;
        }

        if (!items.length) {
            container.innerHTML = [
                '<div class="analytics-list-empty">',
                "<p>Insights will appear here as SmartATS collects more candidate data.</p>",
                "</div>"
            ].join("");
            return;
        }

        container.innerHTML = items.map(function (insight) {
            return [
                '<article class="analytics-insight-card">',
                '<span class="analytics-tone-pill ' + window.analyticsCharts.escapeHtml(insight.tone || "neutral") + '">' + window.analyticsCharts.escapeHtml(insight.title) + "</span>",
                "<p>" + window.analyticsCharts.escapeHtml(insight.description || "") + "</p>",
                "</article>"
            ].join("");
        }).join("");
    }

    function renderCandidateAnalytics(payload) {
        renderMetricCards(payload.overview);

        window.analyticsCharts.renderLineChart(byId("candidate-ats-history-chart"), payload.atsScoreHistory, {
            title: "ATS score history",
            emptyMessage: "Run ATS analysis to start tracking resume progress."
        });
        window.analyticsCharts.renderDonutChart(byId("candidate-status-donut"), payload.applicationStatuses, {
            centerLabel: "Applications",
            emptyMessage: "Application statuses will appear after you start applying."
        });
        window.analyticsCharts.renderLineChart(byId("candidate-interview-history-chart"), payload.interviewScoreHistory, {
            title: "Interview score history",
            emptyMessage: "Complete AI interviews to build a score timeline."
        });
        window.analyticsCharts.renderBarChart(byId("candidate-skill-match-chart"), payload.skillMatchTrends, {
            emptyMessage: "Job match history will appear after you score jobs."
        });
        window.analyticsCharts.renderBarChart(byId("candidate-ats-breakdown-chart"), payload.atsBreakdown, {
            emptyMessage: "A fresh ATS run is needed for breakdown analytics."
        });
        window.analyticsCharts.renderHeatmap(byId("candidate-ats-heatmap"), payload.atsHeatmap, {
            emptyMessage: "Heatmap will appear after your next ATS analysis."
        });
        window.analyticsCharts.renderRadarChart(byId("candidate-radar-chart"), payload.radarMetrics, {
            title: "Candidate readiness radar",
            emptyMessage: "Radar view needs ATS, interview, and match data."
        });
        window.analyticsCharts.renderLineChart(byId("candidate-communication-chart"), payload.communicationTrends, {
            title: "Communication trend",
            emptyMessage: "Communication analytics will appear after completed interviews."
        });
        window.analyticsCharts.renderLineChart(byId("candidate-technical-chart"), payload.technicalTrends, {
            title: "Technical trend",
            emptyMessage: "Technical analytics will appear after completed interviews."
        });

        renderHistoryList(
            "candidate-ats-history-list",
            payload.atsHistory,
            function (item) {
                return [
                    '<article class="analytics-history-card">',
                    '<div class="analytics-history-head">',
                    "<strong>" + window.analyticsCharts.escapeHtml(item.targetRole || "Tracked role") + "</strong>",
                    '<span class="analytics-score-badge">' + window.analyticsCharts.escapeHtml(String(item.atsScore || 0)) + "%</span>",
                    "</div>",
                    '<p>' + window.analyticsCharts.escapeHtml(item.summary || "ATS analysis completed.") + "</p>",
                    '<div class="analytics-history-meta">',
                    '<span>' + window.analyticsCharts.escapeHtml(item.sourceFileName || "resume") + "</span>",
                    '<span>' + window.analyticsCharts.escapeHtml(new Date(item.createdAt).toLocaleDateString()) + "</span>",
                    "</div>",
                    "</article>"
                ].join("");
            },
            "ATS history will appear here after your first analysis."
        );

        renderHistoryList(
            "candidate-interview-history-list",
            payload.recentInterviews,
            function (item) {
                return [
                    '<article class="analytics-history-card">',
                    '<div class="analytics-history-head">',
                    "<strong>" + window.analyticsCharts.escapeHtml(item.jobRole || "Interview session") + "</strong>",
                    '<span class="analytics-score-badge">' + window.analyticsCharts.escapeHtml(String(item.overallScore || 0)) + '%</span>',
                    "</div>",
                    '<p>' + window.analyticsCharts.escapeHtml(item.summary || "Interview completed.") + "</p>",
                    '<div class="analytics-history-meta">',
                    '<span>Communication ' + window.analyticsCharts.escapeHtml(String(item.communicationScore || 0)) + '%</span>',
                    '<span>Technical ' + window.analyticsCharts.escapeHtml(String(item.technicalScore || 0)) + '%</span>',
                    "</div>",
                    "</article>"
                ].join("");
            },
            "Completed interview sessions will appear here."
        );

        renderInsights(payload.improvementInsights);
    }

    async function initializeCandidateDashboardAnalytics() {
        if (document.body.dataset.page !== "dashboard") {
            return;
        }

        if (!window.jobsApp || !window.jobsApp.requireCandidateAccess) {
            return;
        }

        var loading = byId("candidate-analytics-loading");
        if (loading) {
            loading.classList.remove("hidden");
        }
        if (window.smartUi && window.smartUi.renderMetricSkeletons) {
            window.smartUi.renderMetricSkeletons("candidate-metrics-grid", 4);
        }

        try {
            var session = await window.jobsApp.requireCandidateAccess();
            if (!session) {
                return;
            }

            clearBanner();
            renderMetricCards([]);
            [
                "candidate-ats-history-chart",
                "candidate-status-donut",
                "candidate-interview-history-chart",
                "candidate-skill-match-chart",
                "candidate-ats-breakdown-chart",
                "candidate-ats-heatmap",
                "candidate-radar-chart",
                "candidate-communication-chart",
                "candidate-technical-chart"
            ].forEach(function (id) {
                window.analyticsCharts.createSkeleton(byId(id));
            });

            var payload = await window.api.get("/api/analytics/candidate");
            renderCandidateAnalytics(payload || {});

            if (payload && payload.emptyStateMessage && (!payload.atsHistory || !payload.atsHistory.length) && (!payload.recentInterviews || !payload.recentInterviews.length)) {
                setBanner(payload.emptyStateMessage, "error");
            }
        } catch (error) {
            setBanner(error.message || "Unable to load candidate analytics right now.", "error");
            [
                "candidate-ats-history-chart",
                "candidate-status-donut",
                "candidate-interview-history-chart",
                "candidate-skill-match-chart",
                "candidate-ats-breakdown-chart",
                "candidate-ats-heatmap",
                "candidate-radar-chart",
                "candidate-communication-chart",
                "candidate-technical-chart"
            ].forEach(function (id) {
                window.analyticsCharts.createEmptyState(byId(id), "Candidate analytics are temporarily unavailable.");
            });
        } finally {
            if (loading) {
                loading.classList.add("hidden");
            }
        }
    }

    document.addEventListener("DOMContentLoaded", function () {
        initializeCandidateDashboardAnalytics();
        
        window.addEventListener('resume-uploaded', function () {
            initializeCandidateDashboardAnalytics();
        });
    });
})();
