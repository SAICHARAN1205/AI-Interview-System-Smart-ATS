(function () {
    function normalizeRole(role) {
        return (role || "").toUpperCase();
    }

    function getDashboardHref(role) {
        return normalizeRole(role) === "RECRUITER" ? "recruiter.html" : "dashboard.html";
    }

    function isLoggedIn() {
        return Boolean(window.api && window.api.getToken && window.api.getToken());
    }

    function getCurrentNavKey() {
        return document.body?.dataset?.navCurrent || "";
    }

    function getProfile() {
        try {
            var raw = localStorage.getItem("ai_hiring_platform_profile");
            return raw ? JSON.parse(raw) : null;
        } catch(e) { return null; }
    }

    function getUserInitial() {
        var profile = getProfile();
        if (profile && profile.name && profile.name.trim()) return profile.name.trim().charAt(0).toUpperCase();
        var user = window.api.getUser() || "";
        return user.charAt(0).toUpperCase() || "U";
    }

    function getUserDisplayName() {
        var profile = getProfile();
        if (profile && profile.name && profile.name.trim()) return profile.name.trim();
        var user = window.api.getUser() || "";
        return user.split("@")[0] || "User";
    }

    function isLandingPage() {
        return document.body?.dataset?.page === "landing";
    }

    function getNavLinks() {
        var key = getCurrentNavKey();
        var isLanding = isLandingPage();
        var role = window.api && window.api.getRole ? normalizeRole(window.api.getRole()) : "";
        var loggedIn = isLoggedIn();
        
        var links = [];

        if (!loggedIn) {
            links = [
                { key: "home", href: isLanding ? "#hero" : "index.html", label: "Home" },
                { key: "features", href: isLanding ? "#recruiter-features" : "index.html#recruiter-features", label: "Features" },
                { key: "jobs", href: "jobs.html", label: "Jobs" },
                { key: "ats", href: "ats.html", label: "ATS Checker" },
                { key: "interview", href: isLanding ? "#mock-interview" : "index.html#mock-interview", label: "Mock Interview" }
            ];
        } else if (role === "RECRUITER") {
            links = [
                { key: "home", href: isLanding ? "#hero" : "index.html", label: "Home" },
                { key: "create-job", href: "create-job.html", label: "Create Job" },
                { key: "applicants", href: "applicants.html", label: "Applicants" },
                { key: "analytics", href: "recruiter.html", label: "Analytics" },
                { key: "interviews", href: "interviews.html", label: "Interviews" }
            ];
        } else {
            links = [
                { key: "home", href: isLanding ? "#hero" : "index.html", label: "Home" },
                { key: "jobs", href: "jobs.html", label: "Jobs" },
                { key: "ats", href: "ats.html", label: "ATS Checker" },
                { key: "interview", href: "interview.html", label: "Mock Interview" },
                { key: "dashboard", href: "dashboard.html", label: "Dashboard" }
            ];
        }
        
        return links.map(function (l) {
            var active = key === l.key ? " active" : "";
            return '<a href="' + l.href + '" class="' + active + '" data-nav-link="' + l.key + '">' + l.label + '</a>';
        }).join("");
    }

    function getGuestActions() {
        return '<a href="login.html" class="ln-btn ln-btn-ghost">Login</a>' +
               '<a href="register.html" class="ln-btn ln-btn-primary">Register</a>';
    }

    function getLoggedInActions() {
        var initial = getUserInitial();
        var name = getUserDisplayName();
        var role = normalizeRole(window.api.getRole());
        var dashHref = getDashboardHref(role);
        
        var dropdownLinks = '';
        if (role === "RECRUITER") {
            dropdownLinks = '<a href="' + dashHref + '">Dashboard</a>' +
                            '<a href="create-job.html">Create Job</a>' +
                            '<a href="applicants.html">Applicants</a>' +
                            '<a href="settings.html">Account Settings</a>';
        } else {
            dropdownLinks = '<a href="' + dashHref + '">Dashboard</a>' +
                            '<a href="jobs.html?view=applications">My Applications</a>' +
                            '<a href="settings.html">Account Settings</a>';
        }

        return '<div class="ln-avatar-wrap">' +
                   '<button type="button" class="ln-avatar" data-avatar-toggle aria-label="Open profile menu for ' + name + '" aria-expanded="false" title="' + name + '">' + initial + '</button>' +
                   '<div class="ln-dropdown" id="profile-dropdown">' +
                       '<div class="ln-dropdown-header"><strong>' + name + '</strong>' + (role || "User") + '</div>' +
                       dropdownLinks +
                       '<div class="ln-drop-divider"></div>' +
                       '<button type="button" class="ln-drop-danger" data-logout-action>Logout</button>' +
                   '</div>' +
               '</div>';
    }

    function renderNavbar() {
        var root = document.querySelector("[data-navbar-root]");
        if (!root || !window.api) return;

        // Always use the modern ln-navbar on every page
        root.className = "ln-navbar";
        root.innerHTML =
            '<a class="ln-brand" href="index.html">' +
                '<div class="ln-brand-icon">AI</div>' +
                '<span class="ln-brand-text">SmartATS</span>' +
            '</a>' +
            '<button class="ln-hamburger" data-mobile-toggle aria-label="Toggle navigation menu" aria-expanded="false" aria-controls="smartats-mobile-nav smartats-mobile-actions">' +
                '<span></span><span></span><span></span>' +
            '</button>' +
            '<nav class="ln-nav" id="smartats-mobile-nav" aria-label="Primary navigation">' + getNavLinks() + '</nav>' +
            '<div class="ln-actions" id="smartats-mobile-actions">' +
                (isLoggedIn() ? getLoggedInActions() : getGuestActions()) +
            '</div>';
    }

    function logout() {
        if (window.authApp && typeof window.authApp.logout === "function") {
            window.authApp.logout();
            return;
        }
        if (window.api && typeof window.api.clearAuth === "function") {
            window.api.clearAuth();
        }
        window.location.href = "login.html?loggedOut=1";
    }

    function handleDropdown(event) {
        var toggle = event.target.closest("[data-avatar-toggle]");
        if (toggle) {
            event.stopPropagation();
            var wrap = toggle.closest(".ln-avatar-wrap");
            var dd = toggle.nextElementSibling;
            var shouldOpen = dd ? !dd.classList.contains("open") : false;
            document.querySelectorAll(".ln-avatar-wrap.open").forEach(function (item) {
                item.classList.remove("open");
                var button = item.querySelector("[data-avatar-toggle]");
                var dropdown = item.querySelector(".ln-dropdown");
                if (button) {
                    button.setAttribute("aria-expanded", "false");
                }
                if (dropdown) {
                    dropdown.classList.remove("open");
                }
            });
            if (dd && wrap && shouldOpen) {
                wrap.classList.add("open");
                dd.classList.add("open");
                toggle.setAttribute("aria-expanded", "true");
            }
            return;
        }

        // Close dropdown on outside click
        var openDropdowns = document.querySelectorAll(".ln-avatar-wrap.open");
        openDropdowns.forEach(function (wrap) {
            if (!wrap.contains(event.target)) {
                wrap.classList.remove("open");
                var dropdown = wrap.querySelector(".ln-dropdown");
                var button = wrap.querySelector("[data-avatar-toggle]");
                if (dropdown) {
                    dropdown.classList.remove("open");
                }
                if (button) {
                    button.setAttribute("aria-expanded", "false");
                }
            }
        });
    }

    function handleMobileToggle(event) {
        var btn = event.target.closest("[data-mobile-toggle]");
        if (btn) {
            var nav = btn.closest(".ln-navbar");
            if (nav) {
                var isOpen = nav.classList.toggle("mobile-open");
                btn.setAttribute("aria-expanded", String(isOpen));
            }
        }
    }

    function closeOverlays() {
        var nav = document.querySelector(".ln-navbar.mobile-open");
        var toggle = document.querySelector("[data-mobile-toggle]");
        if (nav) {
            nav.classList.remove("mobile-open");
        }
        if (toggle) {
            toggle.setAttribute("aria-expanded", "false");
        }
        document.querySelectorAll(".ln-avatar-wrap.open").forEach(function (wrap) {
            wrap.classList.remove("open");
            var dropdown = wrap.querySelector(".ln-dropdown");
            var button = wrap.querySelector("[data-avatar-toggle]");
            if (dropdown) {
                dropdown.classList.remove("open");
            }
            if (button) {
                button.setAttribute("aria-expanded", "false");
            }
        });
    }

    function initializeNavbar() {
        renderNavbar();

        var root = document.querySelector("[data-navbar-root]");
        if (!root || root.dataset.listenersBound === "true") return;
        root.dataset.listenersBound = "true";

        root.addEventListener("click", function (event) {
            var logoutBtn = event.target.closest("[data-logout-action]");
            if (logoutBtn) { event.preventDefault(); logout(); return; }
            handleMobileToggle(event);
            if (event.target.closest(".ln-nav a, .ln-actions a")) {
                closeOverlays();
            }
        });

        document.addEventListener("click", handleDropdown);
        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                closeOverlays();
            }
        });
        window.addEventListener("authchange", renderNavbar);
        window.addEventListener("profilechange", renderNavbar);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initializeNavbar);
    } else {
        initializeNavbar();
    }
})();
