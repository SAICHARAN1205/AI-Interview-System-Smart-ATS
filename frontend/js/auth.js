(function () {
    const resolvedRoleCache = new Map();
    const roleRequestCache = new Map();
    const ROLE_ACTIONS = {
        CANDIDATE: [
            {
                title: "View Jobs",
                description: "Browse open jobs and apply to roles that fit your profile.",
                href: "jobs.html?view=all",
                button: "Open Jobs",
            },
            {
                title: "My Applications",
                description: "Track your candidate workflow and continue active applications.",
                href: "jobs.html?view=applications",
                button: "View Applications",
            },
            {
                title: "AI Mock Interview",
                description: "Practice a role-specific interview and review AI feedback before the real conversation.",
                href: "interview.html",
                button: "Start Practice",
            },
            {
                title: "Upload Resume",
                description: "Upload your resume to support matching and interview steps.",
                href: "jobs.html?view=resume",
                button: "Upload Resume",
            },
            {
                title: "Analytics & Insights",
                description: "Open your candidate dashboard to track ATS progress, interview trends, and job-match history.",
                href: "dashboard.html",
                button: "Open Dashboard",
            },
            {
                title: "Match Score",
                description: "Use the jobs workspace to review role fit after your resume is uploaded.",
                href: "jobs.html?view=match",
                button: "Open Match Tools",
            },
        ],
        ADMIN: [
            {
                title: "Admin Dashboard",
                description: "View platform analytics and system health.",
                href: "admin.html",
                button: "Open Dashboard",
            },
            {
                title: "User Management",
                description: "Moderate users, candidates, and recruiters.",
                href: "admin.html#users",
                button: "Manage Users",
            },
            {
                title: "AI Configuration",
                description: "Manage AI provider failovers and API limits.",
                href: "admin.html#ai",
                button: "Configure AI",
            }
        ]
    };

    function normalizeRole(role) {
        return (role || "").toUpperCase();
    }

    function getPostLoginRoute(role) {
        const normalized = normalizeRole(role);
        if (normalized === "ADMIN") return "admin.html";
        if (normalized === "RECRUITER") return "recruiter.html";
        return "dashboard.html";
    }

    function setMessage(element, message, type) {
        if (!element) {
            return;
        }

        element.style.transition = "";
        element.style.opacity = "";
        element.textContent = message;
        element.className = `message-box show ${type}`;
    }

    function clearMessage(element) {
        if (!element) {
            return;
        }

        element.style.transition = "";
        element.style.opacity = "";
        element.textContent = "";
        element.className = "message-box";
    }

    function setButtonLoading(button, loadingText, isLoading) {
        if (!button) {
            return;
        }

        if (!button.dataset.defaultText) {
            button.dataset.defaultText = button.textContent;
        }

        button.disabled = isLoading;
        button.textContent = isLoading ? loadingText : button.dataset.defaultText;
    }

    function getCurrentEmail() {
        return window.api.getUser() || "";
    }

    function getCurrentRole() {
        return normalizeRole(window.api.getRole());
    }

    function getStoredSession() {
        return {
            token: window.api.getToken(),
            email: getCurrentEmail(),
            role: getCurrentRole(),
        };
    }

    function storeVerificationSession(email, sessionId) {
        try {
            sessionStorage.setItem("verification_session", JSON.stringify({
                email: email || "",
                sessionId: sessionId || "",
            }));
        } catch (error) {
            // Ignore storage failures.
        }
    }

    function logout() {
        window.api.clearAuth();
        window.location.href = "login.html?loggedOut=1";
    }

    function redirectToLogin() {
        window.location.href = "login.html";
    }

    function ensureAuthenticated() {
        const session = getStoredSession();

        if (!session.token) {
            redirectToLogin();
            return null;
        }

        return session;
    }

    async function resolveRoleForUser(email) {
        const normalizedEmail = (email || "").toLowerCase();
        const storedRole = getCurrentRole();

        if (!window.api.getToken() || !normalizedEmail) {
            return storedRole;
        }

        if (resolvedRoleCache.has(normalizedEmail)) {
            return resolvedRoleCache.get(normalizedEmail);
        }

        if (roleRequestCache.has(normalizedEmail)) {
            return roleRequestCache.get(normalizedEmail);
        }

        const request = (async function () {
            const me = await window.api.get("/api/users/me");

            if (me && (me.email || "").toLowerCase() === normalizedEmail && me.role) {
                const resolvedRole = normalizeRole(me.role);
                window.api.setRole(resolvedRole);
                resolvedRoleCache.set(normalizedEmail, resolvedRole);
                return resolvedRole;
            }

            resolvedRoleCache.set(normalizedEmail, storedRole);
            return storedRole;
        })().catch(function () {
            return storedRole;
        }).finally(function () {
            roleRequestCache.delete(normalizedEmail);
        });

        roleRequestCache.set(normalizedEmail, request);
        return request;
    }

    let captchaRequestCounters = {};

    async function loadCaptcha(prefix) {
        const questionEl = document.getElementById(prefix + "-captcha-question");
        const tokenEl = document.getElementById(prefix + "-captcha-token");
        const answerEl = document.getElementById(prefix + "-captcha-answer");
        
        if (!captchaRequestCounters[prefix]) captchaRequestCounters[prefix] = 0;
        const currentReqId = ++captchaRequestCounters[prefix];

        const setLoading = (isLoading) => {
            if (questionEl && isLoading) {
                questionEl.innerHTML = '<span class="spinner-inline" style="display:inline-block; width:1em; height:1em; border:2px solid currentColor; border-right-color:transparent; border-radius:50%; animation:spin 0.75s linear infinite; margin-right: 0.5rem; vertical-align: middle;"></span> Loading captcha...';
                questionEl.style.cursor = "default";
                questionEl.onclick = null;
            }
        };

        try {
            setLoading(true);
            if (tokenEl) tokenEl.value = "";
            if (answerEl) answerEl.value = "";

            const timeoutPromise = new Promise((_, reject) => setTimeout(() => reject(new Error("Timeout")), 5000));
            const response = await Promise.race([
                window.api.get("/api/auth/captcha"),
                timeoutPromise
            ]);
            
            // If another request was started, cancel this update
            if (currentReqId !== captchaRequestCounters[prefix]) return;

            if (response && response.token) {
                if (questionEl) {
                    questionEl.textContent = response.question;
                }
                if (tokenEl) tokenEl.value = response.token;
            } else {
                throw new Error("Invalid captcha response format");
            }
        } catch (error) {
            if (currentReqId !== captchaRequestCounters[prefix]) return;
            
            if (questionEl) {
                questionEl.innerHTML = '<span style="color:var(--danger); font-size:0.9rem; cursor:pointer;" title="Click to retry">Unable to load captcha. Retry.</span>';
                questionEl.onclick = () => {
                    const btn = document.getElementById(prefix + "-refresh-captcha");
                    if (btn) btn.click();
                };
            }
        } finally {
            // setLoading(false) isn't strictly necessary as the content is already overwritten,
            // but this ensures the finally block executes properly.
            if (currentReqId === captchaRequestCounters[prefix]) {
                // Done loading
            }
        }
    }

    function validateLocalCaptcha(prefix) {
        const questionEl = document.getElementById(prefix + "-captcha-question");
        const answerEl = document.getElementById(prefix + "-captcha-answer");
        
        if (!questionEl || !answerEl) return true;
        
        const question = questionEl.textContent.trim();
        const answer = answerEl.value.trim();
        
        if (!answer) return false;
        
        const match = question.match(/(\d+)\s*([\+\-])\s*(\d+)/);
        if (match) {
            const a = parseInt(match[1], 10);
            const op = match[2];
            const b = parseInt(match[3], 10);
            
            let expected = 0;
            if (op === '+') expected = a + b;
            if (op === '-') expected = a - b;
            
            return parseInt(answer, 10) === expected;
        }
        
        return true;
    }

    async function login() {
        const emailInput = document.getElementById("login-email");
        const passwordInput = document.getElementById("login-password");
        const messageBox = document.getElementById("login-message");
        const loginButton = document.getElementById("login-button");
        const captchaToken = document.getElementById("login-captcha-token");
        const captchaAnswer = document.getElementById("login-captcha-answer");

        clearMessage(messageBox);

        if (!validateLocalCaptcha("login")) {
            setMessage(messageBox, "Incorrect CAPTCHA. Please try again.", "error");
            loadCaptcha("login");
            return;
        }

        setButtonLoading(loginButton, "Logging in...", true);

        const email = emailInput.value.trim();
        const previousEmail = getCurrentEmail();
        const previousRole = getCurrentRole();

        try {
            const response = await window.api.post("/api/auth/login", {
                email,
                password: passwordInput.value,
                captchaToken: captchaToken ? captchaToken.value : null,
                captchaAnswer: captchaAnswer ? captchaAnswer.value : null,
            });

            if (!response || !response.token) {
                throw new Error("Invalid credentials. Please try again.");
            }

            window.api.setToken(response.token, response.refreshToken);
            window.api.setUser(email);

            if (response.role) {
                window.api.setRole(normalizeRole(response.role));
            } else if (previousEmail && previousEmail.toLowerCase() === email.toLowerCase() && previousRole) {
                window.api.setRole(previousRole);
            } else {
                window.api.clearRole();
            }

            const resolvedRole = await resolveRoleForUser(email);
            window.location.href = getPostLoginRoute(resolvedRole || response.role);
        } catch (error) {
            const message = error.isNetworkError
                ? error.message
                : (error.message || "Invalid credentials. Please try again.");

            if (message.includes("verify your email")) {
                storeVerificationSession(email, "");
                window.location.href = `verify-email.html?email=${encodeURIComponent(email)}`;
                return;
            }

            setMessage(messageBox, message, "error");
            loadCaptcha("login"); // reload on error
        } finally {
            setButtonLoading(loginButton, "Logging in...", false);
        }
    }

    async function register() {
        const nameInput = document.getElementById("register-name");
        const emailInput = document.getElementById("register-email");
        const passwordInput = document.getElementById("register-password");
        const roleInput = document.getElementById("register-role");
        const messageBox = document.getElementById("register-message");
        const registerButton = document.getElementById("register-button");
        const captchaToken = document.getElementById("register-captcha-token");
        const captchaAnswer = document.getElementById("register-captcha-answer");

        clearMessage(messageBox);

        // Validate password strictly before sending to backend
        const password = passwordInput.value;
        const isValidPassword = password.length >= 8 &&
                              /[A-Z]/.test(password) &&
                              /[a-z]/.test(password) &&
                              /\d/.test(password) &&
                              /[@$!%*?&#]/.test(password);

        if (!isValidPassword) {
            setMessage(messageBox, "Weak password. Please ensure all password requirements are met.", "error");
            return;
        }

        if (!validateLocalCaptcha("register")) {
            setMessage(messageBox, "Incorrect CAPTCHA. Please try again.", "error");
            loadCaptcha("register");
            return;
        }

        setButtonLoading(registerButton, "Registering...", true);

        const payload = {
            name: nameInput.value.trim(),
            email: emailInput.value.trim(),
            password: passwordInput.value,
            role: normalizeRole(roleInput.value),
            captchaToken: captchaToken ? captchaToken.value : null,
            captchaAnswer: captchaAnswer ? captchaAnswer.value : null,
        };

        try {
            const response = await window.api.post("/api/users/register", payload);
            const sessionId = response ? response.sessionId : "";
            storeVerificationSession(payload.email, sessionId);
            
            setMessage(messageBox, "Registration successful. Please verify your email...", "success");

            window.setTimeout(function () {
                window.location.href = `verify-email.html?email=${encodeURIComponent(payload.email)}&sessionId=${encodeURIComponent(sessionId)}`;
            }, 1500);
        } catch (error) {
            const errorMsg = error.message || "Registration failed. Please try again.";
            
            // Map common backend errors
            if (errorMsg.includes("429") || errorMsg.toLowerCase().includes("too many requests") || errorMsg.includes("60 seconds")) {
                setMessage(messageBox, "Please wait 60 seconds before requesting a new OTP.", "error");
            } else if (errorMsg.includes("registered")) {
                setMessage(messageBox, "Email already registered. Please login.", "error");
            } else if (errorMsg.includes("Weak")) {
                setMessage(messageBox, "Weak password. Please check the requirements.", "error");
            } else if (errorMsg.includes("CAPTCHA")) {
                setMessage(messageBox, "Captcha verification failed. Please try again.", "error");
            } else if (errorMsg.includes("Server error")) {
                setMessage(messageBox, "Server error. Please try again later.", "error");
            } else {
                setMessage(messageBox, errorMsg, "error");
            }
            
            loadCaptcha("register"); // reload on error
        } finally {
            setButtonLoading(registerButton, "Registering...", false);
        }
    }

    function getDashboardCopy() {
        return {
            badge: "Candidate Dashboard",
            title: "Candidate insights workspace",
            subtitle: "Track ATS progress, interview performance, application movement, and your next best actions from one page.",
            sectionTitle: "Candidate actions",
            sectionCopy: "These actions connect directly to your jobs, ATS, interview, and analytics workflows.",
        };
    }

    function renderDashboardActions(role) {
        const actionsContainer = document.getElementById("dashboard-actions");

        if (!actionsContainer) {
            return;
        }

        const actions = ROLE_ACTIONS[role] || [];

        actionsContainer.innerHTML = actions
            .map(function (action) {
                return `
                    <article class="action-card">
                        <div class="action-card-body">
                            <p class="action-eyebrow">Candidate Tool</p>
                            <h3>${action.title}</h3>
                            <p>${action.description}</p>
                        </div>
                        <a href="${action.href}" class="btn btn-primary">${action.button}</a>
                    </article>
                `;
            })
            .join("");
    }

    function applyDashboardNotice(role, noteBox) {
        const params = new URLSearchParams(window.location.search);
        const deniedTarget = params.get("accessDenied");

        if (!noteBox) {
            return;
        }

        if (deniedTarget === "recruiter" && role === "CANDIDATE") {
            noteBox.textContent = "Recruiter pages are available only to recruiter accounts.";
            noteBox.classList.remove("hidden");
            return;
        }

        noteBox.classList.add("hidden");
        noteBox.textContent = "";
    }

    async function initializeDashboard() {
        const session = ensureAuthenticated();

        if (!session) {
            return;
        }

        const resolvedRole = await resolveRoleForUser(session.email);
        const role = resolvedRole || "CANDIDATE";

        if (role === "RECRUITER") {
            window.location.replace("recruiter.html");
            return;
        }

        const copy = getDashboardCopy();
        const noteBox = document.getElementById("dashboard-role-note");
        const emailText = document.getElementById("dashboard-user-email");
        const roleText = document.getElementById("dashboard-role-label");

        document.getElementById("dashboard-badge").textContent = copy.badge;
        document.getElementById("dashboard-title").textContent = copy.title;
        document.getElementById("dashboard-subtitle").textContent = copy.subtitle;
        document.getElementById("dashboard-section-title").textContent = copy.sectionTitle;
        document.getElementById("dashboard-section-copy").textContent = copy.sectionCopy;
        emailText.textContent = session.email || "Unknown user";
        roleText.textContent = role;

        applyDashboardNotice(role, noteBox);
        renderDashboardActions(role);
    }

    function initializeLoginPage() {
        const params = new URLSearchParams(window.location.search);
        const emailInput = document.getElementById("login-email");
        const messageBox = document.getElementById("login-message");

        if (emailInput && params.get("email")) {
            emailInput.value = params.get("email");
        }

        if (params.get("registered") === "1") {
            setMessage(messageBox, "Registration successful. Please log in to continue.", "success");
        } else if (params.get("loggedOut") === "1") {
            const logoutMsg = "You have been logged out successfully.";
            setMessage(messageBox, logoutMsg, "success");

            if (window._logoutTimeout) {
                window.clearTimeout(window._logoutTimeout);
            }

            window._logoutTimeout = window.setTimeout(function () {
                if (messageBox.textContent !== logoutMsg) return;

                messageBox.style.transition = "opacity 0.5s ease-out";
                messageBox.style.opacity = "0";

                window.setTimeout(function () {
                    if (messageBox.textContent === logoutMsg) {
                        clearMessage(messageBox);
                    }
                }, 500);
            }, 3000);
        } else if (params.get("session_expired") === "true") {
            setMessage(messageBox, "Your session expired. Please log in again.", "error");
        }
        
        loadCaptcha("login");
        
        const refreshBtn = document.getElementById("login-refresh-captcha");
        if (refreshBtn) {
            refreshBtn.addEventListener("click", () => loadCaptcha("login"));
        }
    }

    function wireForms() {
        const loginForm = document.getElementById("login-form");
        const registerForm = document.getElementById("register-form");
        const logoutButton = document.getElementById("logout-button");

        if (loginForm) {
            initializeLoginPage();
            loginForm.addEventListener("submit", function (event) {
                event.preventDefault();
                login();
            });
        }

        if (registerForm) {
            loadCaptcha("register");
            
            const refreshBtn = document.getElementById("register-refresh-captcha");
            if (refreshBtn) {
                refreshBtn.addEventListener("click", () => loadCaptcha("register"));
            }
            
            const passInput = document.getElementById("register-password");
            if (passInput) {
                passInput.addEventListener("input", function(e) {
                    const val = e.target.value;
                    const lenEl = document.getElementById("req-length");
                    const upperEl = document.getElementById("req-upper");
                    const lowerEl = document.getElementById("req-lower");
                    const numEl = document.getElementById("req-number");
                    const specEl = document.getElementById("req-special");
                    
                    if (lenEl) lenEl.className = val.length >= 8 ? "valid" : "";
                    if (upperEl) upperEl.className = /[A-Z]/.test(val) ? "valid" : "";
                    if (lowerEl) lowerEl.className = /[a-z]/.test(val) ? "valid" : "";
                    if (numEl) numEl.className = /\d/.test(val) ? "valid" : "";
                    if (specEl) specEl.className = /[@$!%*?&#]/.test(val) ? "valid" : "";
                });
            }
            
            registerForm.addEventListener("submit", function (event) {
                event.preventDefault();
                register();
            });
        }

        if (logoutButton) {
            logoutButton.addEventListener("click", logout);
        }
    }

    window.authApp = {
        normalizeRole,
        logout,
        ensureAuthenticated,
        resolveRoleForUser,
        getStoredSession,
        getPostLoginRoute,
        setMessage,
        clearMessage,
        setButtonLoading,
    };

    document.addEventListener("DOMContentLoaded", function () {
        wireForms();

        if (document.body.dataset.page === "dashboard") {
            initializeDashboard();
        }
        (function initAutoLogout() {
            if (!window.api || !window.api.getToken || !window.api.getToken()) return;

            var inactivityTimeout;
            var warningTimeout;
            var TIMEOUT_MINUTES = 60;
            var WARNING_MINUTES = 59;

            function resetTimer() {
                clearTimeout(inactivityTimeout);
                clearTimeout(warningTimeout);

                var existingModal = document.getElementById("inactivity-warning-modal");
                if (existingModal) existingModal.remove();

                warningTimeout = setTimeout(function () {
                    showWarningModal();
                }, WARNING_MINUTES * 60 * 1000);

                inactivityTimeout = setTimeout(function () {
                    window.api.clearAuth();
                    window.location.href = "login.html?session_expired=true";
                }, TIMEOUT_MINUTES * 60 * 1000);
            }

            function showWarningModal() {
                if (document.getElementById("inactivity-warning-modal")) {
                    return;
                }

                var modalHtml = `
                    <div id="inactivity-warning-modal" style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 99999;">
                        <div style="background: var(--bg); padding: 2rem; border-radius: 8px; text-align: center; max-width: 400px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                            <h3>Session Expiring</h3>
                            <p style="margin: 1rem 0; color: var(--text-muted);">Your session will expire in 1 minute due to inactivity.</p>
                            <button id="stay-logged-in-btn" class="btn btn-primary">Stay Logged In</button>
                        </div>
                    </div>
                `;
                document.body.insertAdjacentHTML("beforeend", modalHtml);
                document.getElementById("stay-logged-in-btn").addEventListener("click", function () {
                    resetTimer();
                });
            }

            ["mousemove", "keypress", "touchstart", "click", "scroll"].forEach(function (eventName) {
                document.addEventListener(eventName, resetTimer, { passive: true });
            });
            resetTimer();
        })();
    });
})();
