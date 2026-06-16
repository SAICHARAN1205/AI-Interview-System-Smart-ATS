(function () {
    var state = {
        session: null,
        workspace: null,
        analytics: null,
        charts: {}
    };

    function byId(id) {
        return document.getElementById(id);
    }

    function renderMetricCards(metrics, targetId) {
        var container = byId(targetId);
        var items = Array.isArray(metrics) ? metrics : [];

        if (!container) {
            return;
        }

        if (!items.length) {
            container.innerHTML = [
                '<article class="metric-card">',
                '<p class="metric-label">Analytics Pending</p>',
                '<strong class="metric-value">--</strong>',
                '<p class="metric-note">Recruiter analytics will populate as activity grows.</p>',
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

    function renderSnapshotJobs() {
        var grid = byId("recruiter-jobs-snapshot");
        var empty = byId("recruiter-snapshot-empty");
        var jobs = (state.workspace && state.workspace.jobs ? state.workspace.jobs : []).slice(0, 4);

        if (!grid || !empty || !window.jobsApp) {
            return;
        }

        if (!jobs.length) {
            grid.innerHTML = "";
            empty.innerHTML = [
                '<div class="empty-state">',
                "<h3>No recruiter jobs yet</h3>",
                "<p>Create your first role from the Create Job page to start the recruiter workflow.</p>",
                "</div>"
            ].join("");
            empty.classList.remove("hidden");
            return;
        }

        empty.classList.add("hidden");
        grid.innerHTML = jobs.map(function (job) {
            return [
                '<article class="job-card">',
                '<div class="card-header-row">',
                "<div>",
                '<p class="card-kicker">Recent Job</p>',
                "<h3>" + window.jobsApp.escapeHtml(job.title || "Untitled role") + "</h3>",
                "</div>",
                '<span class="tag">' + window.jobsApp.escapeHtml(job.companyName || "Unknown company") + "</span>",
                "</div>",
                '<p class="job-salary">' + window.jobsApp.escapeHtml(window.jobsApp.getJobSalary(job)) + "</p>",
                '<p class="job-description">' + window.jobsApp.escapeHtml(job.description || "No description available.") + "</p>",
                '<dl class="detail-list compact">',
                "<div class=\"detail-item\"><dt>Applicants</dt><dd>" + window.jobsApp.escapeHtml(String(job.applicantCount == null ? "0" : job.applicantCount)) + "</dd></div>",
                "<div class=\"detail-item\"><dt>Skills</dt><dd>" + window.jobsApp.escapeHtml(window.jobsApp.getJobSkills(job)) + "</dd></div>",
                "</dl>",
                '<div class="job-card-actions" style="margin-top:1rem;">',
                '<a href="applicants.html?jobId=' + window.jobsApp.escapeHtml(job.id) + '" class="btn btn-primary">View Applicants</a>',
                "</div>",
                "</article>"
            ].join("");
        }).join("");
    }

    function renderPerformanceList(targetId, items, emptyMessage) {
        var container = byId(targetId);
        var records = Array.isArray(items) ? items : [];

        if (!container) {
            return;
        }

        if (!records.length) {
            container.innerHTML = '<div class="analytics-list-empty"><p>' + window.analyticsCharts.escapeHtml(emptyMessage) + "</p></div>";
            return;
        }

        container.innerHTML = records.map(function (item) {
            return [
                '<article class="analytics-history-card">',
                '<div class="analytics-history-head">',
                "<strong>" + window.analyticsCharts.escapeHtml(item.jobTitle || "Tracked job") + "</strong>",
                '<span class="analytics-score-badge">' + window.analyticsCharts.escapeHtml(String(item.successRate || 0)) + "%</span>",
                "</div>",
                "<p>" + window.analyticsCharts.escapeHtml(String(item.applicants || 0)) + " applicants | " +
                    window.analyticsCharts.escapeHtml(String(item.shortlisted || 0)) + " shortlisted | " +
                    window.analyticsCharts.escapeHtml(String(item.interviews || 0)) + " interviews</p>",
                "</article>"
            ].join("");
        }).join("");
    }

    function renderInsights(insights) {
        var container = byId("recruiter-insights-list");
        var items = Array.isArray(insights) ? insights : [];

        if (!container) {
            return;
        }

        if (!items.length) {
            container.innerHTML = '<div class="analytics-list-empty"><p>Insights will appear once recruiter data is available.</p></div>';
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

    function renderChartJs(canvasId, type, labels, data, colors, options) {
        var canvas = byId(canvasId);
        if (!canvas) return;

        if (state.charts[canvasId]) {
            state.charts[canvasId].destroy();
        }

        var ctx = canvas.getContext("2d");
        state.charts[canvasId] = new Chart(ctx, {
            type: type,
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: colors,
                    borderRadius: type === 'bar' ? 4 : 0,
                    borderWidth: type === 'pie' || type === 'doughnut' ? 2 : 0,
                    borderColor: '#ffffff'
                }]
            },
            options: Object.assign({
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { padding: 20, font: { family: "'Inter', sans-serif" } }
                    }
                }
            }, options || {})
        });
    }

    function renderTopCandidates(candidates) {
        var tbody = byId("top-candidates-tbody");
        if (!tbody) return;

        if (!candidates || !candidates.length) {
            tbody.innerHTML = '<tr><td colspan="6" style="padding: 1rem; text-align: center; color: #5d6476;">No top candidates found.</td></tr>';
            return;
        }

        tbody.innerHTML = candidates.map(function(c) {
            return [
                '<tr style="border-bottom: 1px solid #f0f0f0;">',
                '<td style="padding: 12px;"><strong>' + window.analyticsCharts.escapeHtml(c.candidateName) + '</strong><br><span style="font-size: 0.85rem; color: #5d6476;">' + window.analyticsCharts.escapeHtml(c.candidateEmail) + '</span></td>',
                '<td style="padding: 12px;">' + window.analyticsCharts.escapeHtml(c.jobTitle) + '</td>',
                '<td style="padding: 12px;"><span class="tag">' + window.analyticsCharts.escapeHtml(c.status) + '</span></td>',
                '<td style="padding: 12px;"><strong style="color: #4CAF50;">' + window.analyticsCharts.escapeHtml(String(c.atsScore || '--')) + '</strong></td>',
                '<td style="padding: 12px;">' + window.analyticsCharts.escapeHtml(String(c.matchScore || '--')) + '%</td>',
                '<td style="padding: 12px;">' + window.analyticsCharts.escapeHtml(c.appliedAt) + '</td>',
                '</tr>'
            ].join("");
        }).join("");
    }

    function renderAnalytics() {
        var analytics = state.analytics || {};
        renderMetricCards(analytics.overview, "recruiter-metrics-grid");
        renderMetricCards(analytics.adminPreviewMetrics, "recruiter-admin-metrics");

        // Status Distribution (Pie)
        var statusData = analytics.statusDistribution || [];
        var statusLabels = statusData.map(function(s) { return s.label; });
        var statusValues = statusData.map(function(s) { return s.value; });
        renderChartJs("statusDistributionChart", "doughnut", statusLabels, statusValues, ['#9ca3af', '#3b82f6', '#10b981', '#ef4444']);

        // ATS Score Distribution (Bar)
        var atsData = analytics.atsScoreDistribution || [];
        var atsLabels = atsData.map(function(s) { return s.label; });
        var atsValues = atsData.map(function(s) { return s.value; });
        renderChartJs("atsScoreChart", "bar", atsLabels, atsValues, ['#f87171', '#fbbf24', '#fbbf24', '#34d399', '#10b981'], {
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true, grid: { display: false } }, x: { grid: { display: false } } }
        });

        // Applications Per Job (Bar)
        var appData = analytics.applicationsPerJob || [];
        var appLabels = appData.map(function(s) { return s.label; });
        var appValues = appData.map(function(s) { return s.value; });
        renderChartJs("applicationsPerJobChart", "bar", appLabels, appValues, '#6366f1', {
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true, grid: { display: false } }, x: { grid: { display: false } } }
        });

        // Hiring Funnel (Bar)
        var funnelData = analytics.hiringFunnel || [];
        var funnelLabels = funnelData.map(function(s) { return s.label; });
        var funnelValues = funnelData.map(function(s) { return s.value; });
        renderChartJs("hiringFunnelChart", "bar", funnelLabels, funnelValues, ['#9ca3af', '#60a5fa', '#34d399', '#f59e0b'], {
            indexAxis: 'y',
            plugins: { legend: { display: false } },
            scales: { x: { beginAtZero: true, grid: { display: false } }, y: { grid: { display: false } } }
        });

        renderTopCandidates(analytics.topCandidates);

        renderPerformanceList("recruiter-hardest-list", analytics.hardestJobsToFill, "No jobs are being tracked yet.");
        renderPerformanceList("recruiter-success-list", analytics.mostSuccessfulPostings, "No recruiter wins are tracked yet.");
        renderInsights(analytics.aiInsights);

        var adminPanel = byId("recruiter-admin-preview");
        if (adminPanel) {
            adminPanel.classList.toggle("hidden", !analytics.adminPreviewVisible);
        }
    }

    async function loadFilteredAnalytics() {
        var startDate = byId("filter-start-date").value;
        var endDate = byId("filter-end-date").value;
        var jobRole = byId("filter-job-role").value;
        var workMode = byId("filter-work-mode").value;
        var experience = byId("filter-experience").value;

        var query = new URLSearchParams();
        if (startDate) query.append("startDate", startDate);
        if (endDate) query.append("endDate", endDate);
        if (jobRole) query.append("jobRole", jobRole);
        if (workMode) query.append("workMode", workMode);
        if (experience) query.append("experienceLevel", experience);

        var loading = byId("recruiter-dashboard-loading");
        if (loading) loading.classList.remove("hidden");

        try {
            state.analytics = await window.api.get("/api/analytics/recruiter?" + query.toString());
            renderAnalytics();
        } catch (error) {
            window.jobsApp.setBanner("recruiter-dashboard-message", error.message || "Unable to refresh analytics.", "error");
        } finally {
            if (loading) loading.classList.add("hidden");
        }
    }

    function downloadCsv() {
        var startDate = byId("filter-start-date").value;
        var endDate = byId("filter-end-date").value;
        var jobRole = byId("filter-job-role").value;
        var workMode = byId("filter-work-mode").value;
        var experience = byId("filter-experience").value;

        var query = new URLSearchParams();
        if (startDate) query.append("startDate", startDate);
        if (endDate) query.append("endDate", endDate);
        if (jobRole) query.append("jobRole", jobRole);
        if (workMode) query.append("workMode", workMode);
        if (experience) query.append("experienceLevel", experience);

        var url = "/api/analytics/recruiter/export.csv";
        if (query.toString()) url += "?" + query.toString();

        window.api.download(url)
            .then(function (download) {
                var localUrl = window.URL.createObjectURL(download.blob);
                var link = document.createElement("a");
                link.href = localUrl;
                link.download = download.fileName || "smartats-recruiter-analytics.csv";
                document.body.appendChild(link);
                link.click();
                link.remove();
                window.setTimeout(function () {
                    window.URL.revokeObjectURL(localUrl);
                }, 500);
            })
            .catch(function (error) {
                window.jobsApp.setBanner("recruiter-dashboard-message", error.message || "Unable to export the CSV report.", "error");
            });
    }

    function exportPdf() {
        if (!state.analytics) {
            return;
        }

        var printWindow = window.open("", "_blank", "width=1080,height=900");
        if (!printWindow) {
            window.jobsApp.setBanner("recruiter-dashboard-message", "Popup blocked. Please allow popups to export the PDF report.", "error");
            return;
        }

        var overview = (state.analytics.overview || []).map(function (metric) {
            return "<tr><td>" + window.analyticsCharts.escapeHtml(metric.label) + "</td><td>" + window.analyticsCharts.escapeHtml(metric.value) + "</td><td>" + window.analyticsCharts.escapeHtml(metric.note || "") + "</td></tr>";
        }).join("");
        var insights = (state.analytics.aiInsights || []).map(function (insight) {
            return "<li><strong>" + window.analyticsCharts.escapeHtml(insight.title) + ":</strong> " + window.analyticsCharts.escapeHtml(insight.description || "") + "</li>";
        }).join("");
        var candidates = (state.analytics.topCandidates || []).map(function (item) {
            return "<tr><td>" + window.analyticsCharts.escapeHtml(item.candidateName || "N/A") + "</td><td>" + window.analyticsCharts.escapeHtml(item.jobTitle || "N/A") + "</td><td>" + window.analyticsCharts.escapeHtml(String(item.atsScore || 0)) + "</td></tr>";
        }).join("");

        printWindow.document.write([
            "<html><head><title>SmartATS Recruiter Analytics</title>",
            "<style>body{font-family:Segoe UI,sans-serif;padding:32px;color:#1f2230}h1,h2{margin:0 0 12px}p{color:#5d6476}table{width:100%;border-collapse:collapse;margin-top:16px}td,th{border:1px solid #ddd;padding:10px;text-align:left}ul{padding-left:20px}section{margin-top:24px}</style>",
            "</head><body>",
            "<h1>SmartATS Recruiter Analytics</h1>",
            "<p>Generated on " + window.analyticsCharts.escapeHtml(new Date().toLocaleString()) + "</p>",
            "<section><h2>Overview</h2><table><thead><tr><th>Metric</th><th>Value</th><th>Note</th></tr></thead><tbody>" + overview + "</tbody></table></section>",
            "<section><h2>Top Candidates</h2><table><thead><tr><th>Candidate</th><th>Job</th><th>ATS Score</th></tr></thead><tbody>" + candidates + "</tbody></table></section>",
            "<section><h2>AI Insights</h2><ul>" + insights + "</ul></section>",
            "</body></html>"
        ].join(""));
        printWindow.document.close();
        printWindow.focus();
        printWindow.print();
    }

    async function initializeRecruiterDashboard() {
        if (document.body.dataset.page !== "recruiter-dashboard") {
            return;
        }

        var session = await window.jobsApp.requireRecruiterAccess();
        if (!session) {
            return;
        }

        state.session = session;
        byId("recruiter-user-email").textContent = session.email || "Unknown recruiter";
        byId("recruiter-role-label").textContent = session.role;

        var loading = byId("recruiter-dashboard-loading");
        if (loading) {
            loading.classList.remove("hidden");
        }
        if (window.smartUi && window.smartUi.renderMetricSkeletons) {
            window.smartUi.renderMetricSkeletons("recruiter-metrics-grid", 6);
        }

        try {
            var results = await Promise.allSettled([
                window.jobsApp.fetchRecruiterWorkspace(session.email),
                window.api.get("/api/analytics/recruiter")
            ]);

            state.workspace = results[0].status === "fulfilled" ? results[0].value : { jobs: [], applicants: [] };
            state.analytics = results[1].status === "fulfilled" ? results[1].value : {};

            renderSnapshotJobs();
            renderAnalytics();

            if (results[0].status === "rejected") {
                window.jobsApp.setBanner("recruiter-dashboard-message", results[0].reason.message || "Unable to load recruiter workspace data.", "error");
            } else if (results[1].status === "rejected") {
                window.jobsApp.setBanner("recruiter-dashboard-message", results[1].reason.message || "Unable to load recruiter analytics.", "error");
            } else if (state.analytics && state.analytics.emptyStateMessage) {
                window.jobsApp.setBanner("recruiter-dashboard-message", state.analytics.emptyStateMessage, state.workspace.jobs.length ? "success" : "error");
            }
        } catch (error) {
            window.jobsApp.setBanner("recruiter-dashboard-message", error.message || "Unable to load the recruiter dashboard.", "error");
        } finally {
            if (loading) {
                loading.classList.add("hidden");
            }
        }

        var csvButton = byId("recruiter-export-csv");
        var pdfButton = byId("recruiter-export-pdf");
        if (csvButton) {
            csvButton.addEventListener("click", downloadCsv);
        }
        if (pdfButton) {
            pdfButton.addEventListener("click", exportPdf);
        }

        var applyFiltersBtn = byId("apply-filters-btn");
        var clearFiltersBtn = byId("clear-filters-btn");

        if (applyFiltersBtn) {
            applyFiltersBtn.addEventListener("click", loadFilteredAnalytics);
        }
        
        if (clearFiltersBtn) {
            clearFiltersBtn.addEventListener("click", function() {
                byId("filter-start-date").value = "";
                byId("filter-end-date").value = "";
                byId("filter-job-role").value = "";
                byId("filter-work-mode").value = "";
                byId("filter-experience").value = "";
                loadFilteredAnalytics();
            });
        }
    }

    document.addEventListener("DOMContentLoaded", function () {
        initializeRecruiterDashboard();
    });
})();
