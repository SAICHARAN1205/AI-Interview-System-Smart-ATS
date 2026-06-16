(function () {
    // Initialize dark theme immediately
    try {
        if (localStorage.getItem("smartats-theme") === "dark") {
            document.documentElement.classList.add("dark-theme");
        }
    } catch (e) {
        console.warn("Could not read theme preference");
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function ensureToastContainer() {
        var container = document.getElementById("toast-container");

        if (!container) {
            container = document.createElement("div");
            container.id = "toast-container";
            container.className = "toast-container";
            container.setAttribute("aria-live", "polite");
            container.setAttribute("aria-atomic", "false");
            document.body.appendChild(container);
        }

        return container;
    }

    function showToast(message, type, duration) {
        var tone = type || "info";
        var ttl = typeof duration === "number" ? duration : 4200;
        var icons = {
            success: "Success",
            error: "Error",
            warning: "Warning",
            info: "Info",
        };
        var container = ensureToastContainer();
        var toast = document.createElement("div");

        toast.className = "toast " + tone;
        toast.setAttribute("role", tone === "error" ? "alert" : "status");
        toast.innerHTML = [
            '<span class="toast-icon" aria-hidden="true"></span>',
            '<div class="toast-copy">',
            '<strong>' + escapeHtml(icons[tone] || icons.info) + "</strong>",
            '<span class="toast-msg">' + escapeHtml(message || "") + "</span>",
            "</div>",
            '<button type="button" class="toast-close" aria-label="Dismiss notification">×</button>',
        ].join("");

        container.appendChild(toast);

        var closed = false;
        function closeToast() {
            if (closed) {
                return;
            }

            closed = true;
            toast.classList.add("out");
            window.setTimeout(function () {
                toast.remove();
            }, 240);
        }

        var timerId = window.setTimeout(closeToast, Math.max(1800, ttl));
        toast.addEventListener("mouseenter", function () {
            window.clearTimeout(timerId);
        });
        toast.addEventListener("mouseleave", function () {
            timerId = window.setTimeout(closeToast, 1600);
        });
        toast.querySelector(".toast-close").addEventListener("click", closeToast);

        return {
            close: closeToast,
        };
    }

    function renderSkeletonCards(target, options) {
        var element = typeof target === "string" ? document.getElementById(target) : target;
        var config = options || {};
        var count = Math.max(1, Number(config.count) || 4);
        var variant = config.variant || "card";

        if (!element) {
            return;
        }

        element.innerHTML = Array.from({ length: count }).map(function () {
            return [
                '<article class="ui-skeleton-card ' + escapeHtml(variant) + '" aria-hidden="true">',
                '<span class="ui-skeleton-block ui-skeleton-heading"></span>',
                '<span class="ui-skeleton-block ui-skeleton-subheading"></span>',
                '<span class="ui-skeleton-block ui-skeleton-copy"></span>',
                '<span class="ui-skeleton-block ui-skeleton-copy short"></span>',
                '<span class="ui-skeleton-block ui-skeleton-action"></span>',
                "</article>",
            ].join("");
        }).join("");
    }

    function renderMetricSkeletons(target, count) {
        var element = typeof target === "string" ? document.getElementById(target) : target;

        if (!element) {
            return;
        }

        element.innerHTML = Array.from({ length: Math.max(1, Number(count) || 4) }).map(function () {
            return [
                '<article class="ui-skeleton-card metric" aria-hidden="true">',
                '<span class="ui-skeleton-block ui-skeleton-chip"></span>',
                '<span class="ui-skeleton-block ui-skeleton-value"></span>',
                '<span class="ui-skeleton-block ui-skeleton-copy"></span>',
                "</article>",
            ].join("");
        }).join("");
    }

    function renderEmptyState(target, options) {
        var element = typeof target === "string" ? document.getElementById(target) : target;
        var config = options || {};
        var actionHref = config.actionHref;
        var actionLabel = config.actionLabel;
        var actionHtml = "";

        if (!element) {
            return;
        }

        if (actionHref && actionLabel) {
            actionHtml = '<a href="' + escapeHtml(actionHref) + '" class="btn btn-primary">' + escapeHtml(actionLabel) + "</a>";
        } else if (actionLabel) {
            actionHtml = '<button type="button" class="btn btn-primary" data-empty-action="true">' + escapeHtml(actionLabel) + "</button>";
        }

        element.innerHTML = [
            '<div class="empty-state rich-empty-state' + (config.compact ? " compact" : "") + '">',
            '<div class="empty-state-illustration" aria-hidden="true">' + escapeHtml(config.illustration || "• • •") + "</div>",
            '<h3>' + escapeHtml(config.title || "Nothing to show yet") + "</h3>",
            '<p>' + escapeHtml(config.description || "This section will populate once more data is available.") + "</p>",
            actionHtml ? '<div class="empty-state-actions">' + actionHtml + "</div>" : "",
            "</div>",
        ].join("");

        if (actionHtml && typeof config.onAction === "function") {
            var button = element.querySelector("[data-empty-action='true']");
            if (button) {
                button.addEventListener("click", config.onAction);
            }
        }
    }

    function setBusy(target, busy) {
        var element = typeof target === "string" ? document.getElementById(target) : target;

        if (!element) {
            return;
        }

        element.setAttribute("aria-busy", busy ? "true" : "false");
    }

    function openConfirm(options) {
        var config = options || {};
        var existing = document.getElementById("smartats-confirm-root");

        if (existing) {
            existing.remove();
        }

        return new Promise(function (resolve) {
            var root = document.createElement("div");
            root.id = "smartats-confirm-root";
            root.innerHTML = [
                '<div class="modal-overlay smart-confirm-overlay">',
                '<div class="modal confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="smart-confirm-title" aria-describedby="smart-confirm-description">',
                '<div class="modal-header">',
                '<h2 id="smart-confirm-title">' + escapeHtml(config.title || "Please confirm") + "</h2>",
                '<button type="button" class="modal-close" data-confirm-close aria-label="Close confirmation">×</button>',
                "</div>",
                '<div class="modal-body confirm-dialog-body">',
                '<p id="smart-confirm-description">' + escapeHtml(config.description || "Confirm this action to continue.") + "</p>",
                "</div>",
                '<div class="modal-footer">',
                '<button type="button" class="btn btn-outline" data-confirm-cancel>' + escapeHtml(config.cancelLabel || "Cancel") + "</button>",
                '<button type="button" class="btn ' + escapeHtml(config.confirmClass || "btn-primary") + '" data-confirm-ok>' + escapeHtml(config.confirmLabel || "Confirm") + "</button>",
                "</div>",
                "</div>",
                "</div>",
            ].join("");

            function close(result) {
                root.remove();
                resolve(Boolean(result));
            }

            root.addEventListener("click", function (event) {
                if (event.target.matches("[data-confirm-close], [data-confirm-cancel], .smart-confirm-overlay")) {
                    close(false);
                }
                if (event.target.matches("[data-confirm-ok]")) {
                    close(true);
                }
            });

            root.addEventListener("keydown", function (event) {
                if (event.key === "Escape") {
                    event.preventDefault();
                    close(false);
                }
            });

            document.body.appendChild(root);
            var okButton = root.querySelector("[data-confirm-ok]");
            if (okButton) {
                okButton.focus();
            }
        });
    }

    function initializeUi() {
        document.body.classList.add("ui-ready");

        // Global password toggle delegation
        document.addEventListener("click", function(e) {
            const toggleBtn = e.target.closest(".password-toggle");
            if (!toggleBtn) return;
            
            const input = toggleBtn.previousElementSibling;
            if (input && input.tagName === "INPUT") {
                // Preserve cursor position to avoid jumping
                const start = input.selectionStart;
                const end = input.selectionEnd;
                
                if (input.type === "password") {
                    input.type = "text";
                    toggleBtn.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-eye-off"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>`;
                } else {
                    input.type = "password";
                    toggleBtn.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-eye"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>`;
                }
                
                // Restore cursor position
                input.setSelectionRange(start, end);
                input.focus();
            }
        });
    }

    window.smartUi = {
        escapeHtml: escapeHtml,
        showToast: showToast,
        renderSkeletonCards: renderSkeletonCards,
        renderMetricSkeletons: renderMetricSkeletons,
        renderEmptyState: renderEmptyState,
        setBusy: setBusy,
        confirm: openConfirm,
    };

    if (!window.showToast) {
        window.showToast = showToast;
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initializeUi);
    } else {
        initializeUi();
    }
})();
