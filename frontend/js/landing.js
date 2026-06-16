(function () {
    function normalizeRole(role) {
        return (role || "").toUpperCase();
    }

    function getDashboardHref(role) {
        return normalizeRole(role) === "RECRUITER" ? "recruiter.html" : "dashboard.html";
    }

    function initScrollReveal() {
        var reveals = document.querySelectorAll(".ln-reveal");
        if (!reveals.length || !("IntersectionObserver" in window)) {
            reveals.forEach(function (el) { el.classList.add("visible"); });
            return;
        }

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add("visible");
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.12 });

        reveals.forEach(function (el) { observer.observe(el); });
    }

    function initSmoothAnchors() {
        document.querySelectorAll('a[href^="#"]').forEach(function (a) {
            a.addEventListener("click", function (e) {
                var target = document.querySelector(a.getAttribute("href"));
                if (target) {
                    e.preventDefault();
                    target.scrollIntoView({ behavior: "smooth", block: "start" });
                }
            });
        });
    }

    function updateLandingForAuth() {
        if (!window.api || !document.body || document.body.dataset.page !== "landing") return;

        var primaryCta = document.getElementById("landing-primary-cta");
        var secondaryCta = document.getElementById("landing-secondary-cta");
        var token = window.api.getToken();
        var role = token ? normalizeRole(window.api.getRole()) : "";

        var atsChecker = document.getElementById("ats-checker");
        var mockInterview = document.getElementById("mock-interview");
        var matchScore = document.getElementById("match-score");
        var recruiterFeatures = document.getElementById("recruiter-features");

        if (!token) {
            if (primaryCta) { primaryCta.href = "register.html"; primaryCta.textContent = "Get Started"; }
            if (secondaryCta) { secondaryCta.href = "jobs.html"; secondaryCta.textContent = "Explore Jobs"; }
            
            if (atsChecker) atsChecker.style.display = "";
            if (mockInterview) mockInterview.style.display = "";
            if (matchScore) matchScore.style.display = "";
            if (recruiterFeatures) recruiterFeatures.style.display = "";
            return;
        }

        var dashHref = getDashboardHref(role);
        if (primaryCta) { primaryCta.href = dashHref; primaryCta.textContent = "Open Dashboard"; }
        
        if (role === "RECRUITER") {
            if (secondaryCta) { secondaryCta.href = "create-job.html"; secondaryCta.textContent = "Create Job"; }
            if (atsChecker) atsChecker.style.display = "none";
            if (mockInterview) mockInterview.style.display = "none";
            if (matchScore) matchScore.style.display = "none";
            if (recruiterFeatures) recruiterFeatures.style.display = "";
        } else {
            if (secondaryCta) { secondaryCta.href = "jobs.html"; secondaryCta.textContent = "Browse Jobs"; }
            if (atsChecker) atsChecker.style.display = "";
            if (mockInterview) mockInterview.style.display = "";
            if (matchScore) matchScore.style.display = "";
            if (recruiterFeatures) recruiterFeatures.style.display = "none";
        }
    }

    function tryLoadJobs() {
        var grid = document.getElementById("landing-jobs-grid");
        if (!grid || !window.api) return;

        window.api.get("/api/jobs/all").then(function (jobs) {
            if (!Array.isArray(jobs) || jobs.length === 0) return;
            var html = jobs.slice(0, 4).map(function (job) {
                var title = job.title || "Untitled";
                var company = job.companyName || "Company";
                var location = job.location || "Remote";
                var salary = job.salary || job.description?.match(/[\₹\$][\d,]+/)?.[0] || "Competitive";
                return '<div class="ln-job-card">' +
                    '<h3>' + title + '</h3>' +
                    '<div class="ln-job-meta">' + company + ' — ' + location + '</div>' +
                    '<div class="ln-job-salary">' + salary + '</div>' +
                    '<a href="jobs.html" class="ln-btn ln-btn-primary" style="font-size:.82rem;padding:.4rem .9rem">Apply Now</a>' +
                '</div>';
            }).join("");
            grid.innerHTML = html;
        }).catch(function () {
            // Keep placeholder jobs on error
        });
    }

    function initScrollSpy() {
        var sections = document.querySelectorAll("section[id]");
        var navLinks = document.querySelectorAll(".ln-nav a[data-nav-link]");
        
        if (!sections.length || !navLinks.length) return;

        window.addEventListener("scroll", function() {
            var scrollY = window.pageYOffset;
            var current = "";

            sections.forEach(function(sec) {
                var top = sec.offsetTop - 150;
                var height = sec.offsetHeight;
                if (scrollY >= top && scrollY < top + height) {
                    current = sec.getAttribute("id");
                }
            });

            if (scrollY < 100) current = "hero";

            // Map section IDs to nav keys
            var keyMap = {
                "hero": "home",
                "ats-checker": "ats",
                "mock-interview": "interview",
                "recruiter-features": "features",
                "how-it-works": "features"
            };

            var activeKey = keyMap[current] || "";
            if (activeKey) {
                navLinks.forEach(function(a) {
                    a.classList.remove("active");
                    if (a.getAttribute("data-nav-link") === activeKey) {
                        a.classList.add("active");
                    }
                });
            }
        });
    }

    function init() {
        if (document.body?.dataset?.page !== "landing") return;
        initScrollReveal();
        initSmoothAnchors();
        updateLandingForAuth();
        tryLoadJobs();
        
        // Wait a small tick to ensure navbar is rendered before spy attaches
        setTimeout(initScrollSpy, 100);
    }

    document.addEventListener("DOMContentLoaded", init);
    window.addEventListener("authchange", updateLandingForAuth);
})();
