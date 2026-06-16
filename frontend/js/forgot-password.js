(function () {
    function setMessage(message, type) {
        const messageBox = document.getElementById("forgot-message");
        if (!messageBox) return;

        messageBox.textContent = message;
        messageBox.className = `message-box show ${type}`;
    }

    function setButtonLoading(isLoading) {
        const btn = document.getElementById("forgot-button");
        if (!btn) return;

        if (!btn.dataset.defaultText) {
            btn.dataset.defaultText = btn.textContent;
        }

        btn.disabled = isLoading;
        btn.textContent = isLoading ? "Sending..." : btn.dataset.defaultText;
    }

    async function handleForgotPassword(e) {
        e.preventDefault();
        const emailInput = document.getElementById("forgot-email");
        const email = emailInput.value.trim();

        if (!email) return;

        setButtonLoading(true);

        try {
            const response = await window.api.post("/api/auth/forgot-password", { email });

            setMessage("Reset code sent! Redirecting...", "success");
            
            setTimeout(() => {
                window.location.href = `reset-password.html?email=${encodeURIComponent(email)}`;
            }, 1500);

        } catch (error) {
            setMessage(error.message || "Failed to send reset code. Please try again.", "error");
        } finally {
            setButtonLoading(false);
        }
    }

    document.addEventListener("DOMContentLoaded", () => {
        const form = document.getElementById("forgot-password-form");
        if (form) {
            form.addEventListener("submit", handleForgotPassword);
        }
    });

})();
