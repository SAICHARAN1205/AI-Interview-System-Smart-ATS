(function () {
    const DEFAULT_BASE_URL = "https://ai-interview-system-smart-ats.onrender.com";
    const API_BASE_URL_KEY = "ai_hiring_platform_api_base_url";
    const TOKEN_KEY = "ai_hiring_platform_token";
    const REFRESH_TOKEN_KEY = "ai_hiring_platform_refresh_token";
    const ROLE_KEY = "ai_hiring_platform_role";
    const USER_KEY = "ai_hiring_platform_user";
    let activeBaseURL = null;
    let discoveryPromise = null;
    let isRefreshing = false;
    let refreshQueue = [];

    // ENFORCE STRICT ADMIN SESSION: Clean up stale localStorage if a tab/browser was closed.
    try {
        if (localStorage.getItem(ROLE_KEY) === 'ADMIN' && !sessionStorage.getItem(TOKEN_KEY)) {
            localStorage.removeItem(TOKEN_KEY);
            localStorage.removeItem(REFRESH_TOKEN_KEY);
            localStorage.removeItem(ROLE_KEY);
            localStorage.removeItem(USER_KEY);
        }
    } catch (error) {
        // Ignore in restricted environments
    }

    function normalizeBaseURL(value) {
        return typeof value === "string" ? value.trim().replace(/\/+$/, "") : "";
    }

    function readStorage(key) {
        try {
            let val = sessionStorage.getItem(key);
            if (!val) {
                val = localStorage.getItem(key);
            }
            return val;
        } catch (error) {
            return null;
        }
    }

    function writeStorage(key, value, forceSessionStorage = false) {
        try {
            if (!value) {
                localStorage.removeItem(key);
                sessionStorage.removeItem(key);
                return;
            }

            if (forceSessionStorage) {
                sessionStorage.setItem(key, value);
                localStorage.removeItem(key);
            } else {
                localStorage.setItem(key, value);
                sessionStorage.removeItem(key);
            }
        } catch (error) {
            // Ignore storage failures in restricted browser modes.
        }
    }

    function emitAuthChange() {
        try {
            window.dispatchEvent(new CustomEvent("authchange", {
                detail: {
                    token: getToken(),
                    role: getRole(),
                    user: getUser(),
                },
            }));
        } catch (error) {
            // Ignore event dispatch failures outside the browser runtime.
        }
    }

    function rememberBaseURL(baseURL) {
        activeBaseURL = normalizeBaseURL(baseURL);
        writeStorage(API_BASE_URL_KEY, activeBaseURL);
    }

    async function discoverBaseURL(forceRefresh) {
        return DEFAULT_BASE_URL;
    }

    function getToken() {
        return readStorage(TOKEN_KEY);
    }

    function setToken(token, refreshToken) {
        const isSession = getRole() === 'ADMIN';
        writeStorage(TOKEN_KEY, token, isSession);
        if (refreshToken) {
            writeStorage(REFRESH_TOKEN_KEY, refreshToken, isSession);
        }
        emitAuthChange();
    }

    function clearToken(skipNotify) {
        writeStorage(TOKEN_KEY, "");
        writeStorage(REFRESH_TOKEN_KEY, "");

        if (!skipNotify) {
            emitAuthChange();
        }
    }

    function getRole() {
        return readStorage(ROLE_KEY);
    }

    function setRole(role) {
        const isSession = role === 'ADMIN';
        writeStorage(ROLE_KEY, role, isSession);
        
        if (isSession) {
            // Move existing tokens to sessionStorage if we are setting ADMIN role
            const currentToken = readStorage(TOKEN_KEY);
            const currentRefresh = readStorage(REFRESH_TOKEN_KEY);
            const currentUser = readStorage(USER_KEY);
            
            if (currentToken) writeStorage(TOKEN_KEY, currentToken, true);
            if (currentRefresh) writeStorage(REFRESH_TOKEN_KEY, currentRefresh, true);
            if (currentUser) writeStorage(USER_KEY, currentUser, true);
        }
        
        emitAuthChange();
    }

    function clearRole(skipNotify) {
        writeStorage(ROLE_KEY, "");

        if (!skipNotify) {
            emitAuthChange();
        }
    }

    function getUser() {
        return readStorage(USER_KEY);
    }

    function setUser(user) {
        const isSession = getRole() === 'ADMIN';
        writeStorage(USER_KEY, user, isSession);
        emitAuthChange();
    }

    function clearUser(skipNotify) {
        writeStorage(USER_KEY, "");

        if (!skipNotify) {
            emitAuthChange();
        }
    }

    function clearAuth() {
        clearToken(true);
        clearRole(true);
        clearUser(true);
        emitAuthChange();
    }

    function getHeaders() {
        const headers = {
            Accept: "application/json",
        };

        const token = getToken();

        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }

        return headers;
    }

    function getFileNameFromDisposition(dispositionHeader) {
        if (!dispositionHeader) {
            return "";
        }

        const match = dispositionHeader.match(/filename="?([^"]+)"?/i);
        return match ? match[1] : "";
    }

    function extractErrorMessage(payload, fallbackMessage) {
        if (typeof payload === "string" && payload.trim()) {
            if (payload.trim().toLowerCase().startsWith("<!doctype html") || payload.trim().toLowerCase().startsWith("<html")) {
                return fallbackMessage;
            }
            return payload;
        }

        if (payload && typeof payload === "object") {
            if (payload.message) {
                return payload.message;
            }

            if (payload.error) {
                return payload.error;
            }

            if (payload.errors && typeof payload.errors === "object") {
                const messages = Object.values(payload.errors).filter(Boolean);

                if (messages.length > 0) {
                    return messages.join(", ");
                }
            }
        }

        return fallbackMessage;
    }

    function sanitizeUserMessage(message, fallbackMessage) {
        var raw = String(message || "").trim();
        var fallback = fallbackMessage || "Something went wrong. Please try again.";

        if (!raw) {
            return fallback;
        }

        if (/<!doctype html|<html|sql|jdbc|hibernate|jwt|stack trace|exception:|org\.springframework|io\.jsonwebtoken|select .* from|insert into|update .* set|delete from/i.test(raw)) {
            return fallback;
        }

        return raw;
    }

    function normalizeResumeUploadErrorMessage(error) {
        const rawMessage = extractErrorMessage(
            error && error.payload ? error.payload : error && error.message ? { message: error.message } : null,
            "We couldn't upload this resume. Please try again with a valid PDF, DOC, or DOCX file."
        );
        const message = String(rawMessage || "").trim();

        if (!message) {
            return "We couldn't upload this resume. Please try again with a valid PDF, DOC, or DOCX file.";
        }

        if (/could not read text from this file|resume text is unavailable/i.test(message)) {
            return "We could not read text from this file. Please upload a real resume PDF or DOCX with selectable text. Scanned/image-only PDFs are not supported.";
        }

        if (/does not appear to be a resume|valid resume document/i.test(message)) {
            return "This file does not appear to be a resume or CV. Please upload a valid resume document.";
        }

        if (/too large|10 mb/i.test(message)) {
            return "File is too large. Maximum size is 10 MB.";
        }

        if (/file is required|choose|select/i.test(message)) {
            return "Please select a PDF, DOC, or DOCX resume.";
        }

        if (/pdf|docx?|word/i.test(message)) {
            return "Please upload a PDF, DOC, or DOCX resume.";
        }

        if (error && error.status >= 500) {
            return "We couldn't upload this resume right now. Please try again.";
        }

        return "We couldn't upload this resume. Please try again with a valid PDF, DOC, or DOCX file.";
    }

    function normalizeAiErrorMessage(error, fallbackMessage) {
        const rawMessage = extractErrorMessage(
            error && error.payload ? error.payload : error && error.message ? { message: error.message } : null,
            fallbackMessage || "Unable to complete the AI analysis right now. Please try again shortly."
        );
        const message = String(rawMessage || "").trim();
        const status = Number(error && error.status || 0);

        if (status === 429 && /please wait \d+ more second/i.test(message)) {
            return message;
        }

        if (status === 429 || /too many requests|rate limit|provider busy|temporarily busy|overload|overloaded|timeout|timed out|read timed out|connect timed out|connection reset|service unavailable|bad gateway|gateway timeout/i.test(message)) {
            return "AI provider temporarily busy. Using fallback analysis.";
        }

        return message || fallbackMessage || "Unable to complete the AI analysis right now. Please try again shortly.";
    }

    function buildRequestOptions(method, body, config) {
        const options = {
            method,
            headers: getHeaders(),
        };

        if (config?.accept) {
            options.headers.Accept = config.accept;
        }

        if (body !== undefined) {
            if (body instanceof FormData) {
                options.body = body;
            } else {
                options.headers["Content-Type"] = config?.contentType || "application/json";
                options.body = config?.serialize === false ? body : JSON.stringify(body);
            }
        }

        return options;
    }

    async function fetchWithTimeout(url, options, timeoutMs) {
        var ttl = Math.max(5000, Number(timeoutMs) || 45000);
        var controller = new AbortController();
        var timerId = window.setTimeout(function () {
            controller.abort();
        }, ttl);

        try {
            return await fetch(url, Object.assign({}, options, {
                signal: controller.signal,
            }));
        } finally {
            window.clearTimeout(timerId);
        }
    }

    async function executeRequest(endpoint, options, config) {
        const requestConfig = config || {};

        let response;
        let lastError = null;
        const baseURL = DEFAULT_BASE_URL;

        try {
            response = await fetchWithTimeout(`${baseURL}${endpoint}`, options, requestConfig.timeoutMs);
        } catch (error) {
            lastError = error;
        }

        if (!response) {
            const networkError = new Error(
                "Unable to connect to the server. Please check your internet connection."
            );
            networkError.isNetworkError = true;
            networkError.cause = lastError;
            throw networkError;
        }

        if (response && !response.ok && response.status === 401 && !endpoint.includes("/api/auth/")) {
            const refreshToken = readStorage(REFRESH_TOKEN_KEY);
            if (refreshToken) {
                if (isRefreshing) {
                    return new Promise(function(resolve, reject) {
                        refreshQueue.push({ resolve, reject, endpoint, options, config });
                    });
                }
                
                isRefreshing = true;
                try {
                    const refreshURL = DEFAULT_BASE_URL;
                    const refreshResponse = await fetchWithTimeout(`${refreshURL}/api/auth/refresh`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ refreshToken: refreshToken })
                    }, 10000);
                    
                    if (refreshResponse.ok) {
                        const refreshData = await refreshResponse.json();
                        setToken(refreshData.token, refreshData.refreshToken);
                        
                        options.headers.Authorization = `Bearer ${refreshData.token}`;
                        
                        isRefreshing = false;
                        const queue = refreshQueue.slice();
                        refreshQueue = [];
                        queue.forEach(function(req) {
                            req.options.headers.Authorization = `Bearer ${refreshData.token}`;
                            executeRequest(req.endpoint, req.options, req.config).then(req.resolve).catch(req.reject);
                        });
                        
                        return executeRequest(endpoint, options, config);
                    } else {
                        throw new Error("Refresh failed");
                    }
                } catch (e) {
                    isRefreshing = false;
                    refreshQueue.forEach(function(req) { req.reject(new Error("Session expired. Please log in again.")); });
                    refreshQueue = [];
                    clearAuth();
                    window.location.href = "login.html?session_expired=true";
                    throw new Error("Session expired. Please log in again.");
                }
            } else {
                clearAuth();
                window.location.href = "login.html?session_expired=true";
                throw new Error("Session expired. Please log in again.");
            }
        }


        if (requestConfig.responseType === "blob") {
            if (!response.ok) {
                let payload = null;

                try {
                    const contentType = response.headers.get("content-type") || "";
                    payload = contentType.includes("application/json")
                        ? await response.json()
                        : await response.text();
                } catch (error) {
                    payload = null;
                }

                let errorMessage = sanitizeUserMessage(
                    extractErrorMessage(payload, "Something went wrong. Please try again."),
                    "Something went wrong. Please try again."
                );

                if (response.status === 403 || errorMessage === "Forbidden") {
                    errorMessage = "You do not have permission to access this resource.";
                } else if (response.status === 401) {
                    errorMessage = "Session expired. Please log in again.";
                } else if (response.status === 404) {
                    errorMessage = "Requested resource not found.";
                } else if (response.status >= 500) {
                    errorMessage = "Server error. Please try later.";
                }

                const requestError = new Error(errorMessage);
                requestError.status = response.status;
                requestError.payload = payload;
                throw requestError;
            }

            return {
                blob: await response.blob(),
                contentType: response.headers.get("content-type") || "application/octet-stream",
                fileName: getFileNameFromDisposition(response.headers.get("content-disposition")),
            };
        }

        const contentType = response.headers.get("content-type") || "";
        let payload = null;

        try {
            payload = contentType.includes("application/json")
                ? await response.json()
                : await response.text();
        } catch (error) {
            payload = null;
        }

        if (!response.ok) {
            let errorMessage = sanitizeUserMessage(
                extractErrorMessage(payload, "Something went wrong. Please try again."),
                "Something went wrong. Please try again."
            );

            // Do not override server-provided auth messages like "Incorrect password" or "Incorrect CAPTCHA"
            // if we are hitting auth endpoints
            const isAuthEndpoint = endpoint.includes("/api/auth/");
            
            if (!isAuthEndpoint) {
                if (response.status === 403 || errorMessage === "Forbidden") {
                    errorMessage = "You do not have permission to access this resource.";
                } else if (response.status === 401) {
                    errorMessage = "Session expired. Please log in again.";
                } else if (response.status === 404) {
                    errorMessage = "Requested resource not found.";
                } else if (response.status >= 500 && !errorMessage) {
                    errorMessage = "Server error. Please try later.";
                }
            } else if (response.status === 401 && !payload) {
                 // Fallback if auth endpoint gives 401 without message
                 errorMessage = "Invalid credentials or unauthorized request.";
            }

            const requestError = new Error(errorMessage);
            requestError.status = response.status;
            requestError.payload = payload;
            throw requestError;
        }

        if (payload && typeof payload === 'object' && 'success' in payload) {
            if (payload.success) {
                return payload.data;
            } else {
                const requestError = new Error(sanitizeUserMessage(payload.message, "Something went wrong. Please try again."));
                requestError.status = response.status;
                requestError.payload = payload;
                throw requestError;
            }
        }

        return payload;
    }

    async function request(method, endpoint, body, config) {
        const options = buildRequestOptions(method, body, config);
        return executeRequest(endpoint, options, config);
    }

    async function refreshProfileResumeState() {
        try {
            const resume = await request("GET", "/api/resumes/me");
            const profileKey = "ai_hiring_platform_profile";
            const profile = JSON.parse(readStorage(profileKey) || "{}");
            profile.resumeFileName = resume.fileName || "Resume";
            profile.hasResume = true;
            writeStorage(profileKey, JSON.stringify(profile));
            try {
                window.dispatchEvent(new CustomEvent("resume-uploaded", { detail: { resume: resume } }));
            } catch (e) {}
            return resume;
        } catch (error) {
            const profileKey = "ai_hiring_platform_profile";
            const profile = JSON.parse(readStorage(profileKey) || "{}");
            profile.resumeFileName = "";
            profile.hasResume = false;
            writeStorage(profileKey, JSON.stringify(profile));
            return null;
        }
    }

    window.api = {
        get baseURL() {
            return DEFAULT_BASE_URL;
        },
        get API_URL() {
            return DEFAULT_BASE_URL;
        },
        get: function (endpoint) {
            return request("GET", endpoint);
        },
        post: function (endpoint, body) {
            return request("POST", endpoint, body);
        },
        put: function (endpoint, body) {
            return request("PUT", endpoint, body);
        },
        del: function (endpoint) {
            return request("DELETE", endpoint);
        },
        upload: function (endpoint, formData) {
            return request("POST", endpoint, formData);
        },
        download: function (endpoint) {
            return request("GET", endpoint, undefined, {
                accept: "application/octet-stream,application/pdf,*/*",
                responseType: "blob",
            });
        },
        getToken,
        setToken,
        clearToken,
        getRole,
        setRole,
        clearRole,
        getUser,
        setUser,
        clearUser,
        clearAuth,
        isAuthenticated: function () {
            return Boolean(getToken());
        },
        removeToken: clearAuth,
        detectBaseURL: discoverBaseURL,
        setBaseURL: rememberBaseURL,
        normalizeResumeUploadErrorMessage,
        normalizeAiErrorMessage,
        refreshProfileResumeState,
    };
})();
