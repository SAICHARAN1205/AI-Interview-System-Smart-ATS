(function () {
    let resetEmail = "";

    function getQueryParam(name) {
        const params = new URLSearchParams(window.location.search);
        return params.get(name);
    }

    function setMessage(elementId, message, type) {
        const messageBox = document.getElementById(elementId);
        if (!messageBox) return;

        messageBox.textContent = message;
        messageBox.className = `message-box show ${type}`;
    }

    function setButtonLoading(btnId, isLoading, defaultText) {
        const btn = document.getElementById(btnId);
        if (!btn) return;

        btn.disabled = isLoading;
        btn.textContent = isLoading ? "Processing..." : defaultText;
    }

    async function handleVerifyOtp(e) {
        e.preventDefault();
        const otpInput = document.getElementById("reset-otp");
        const otp = otpInput.value.trim();

        if (otp.length !== 6) {
            setMessage("verify-message", "Please enter a valid 6-digit OTP", "error");
            return;
        }

        setButtonLoading("verify-button", true, "Verify Code");

        try {
            await window.api.post("/api/auth/verify-reset-otp", { 
                email: resetEmail,
                otp: otp
            });
            
            setMessage("verify-message", "OTP verified successfully!", "success");
            
            // Switch to Step 2
            setTimeout(() => {
                document.getElementById("verify-otp-form").classList.remove("active");
                document.getElementById("new-password-form").classList.add("active");
                document.getElementById("reset-title").textContent = "Create New Password";
                document.getElementById("reset-subtitle").textContent = "Please enter your new secure password.";
                document.getElementById("reset-new-password").focus();
            }, 1000);

        } catch (error) {
            setMessage("verify-message", error.message || "Failed to verify OTP. It may be incorrect or expired.", "error");
        } finally {
            setButtonLoading("verify-button", false, "Verify Code");
        }
    }

    async function handleResetPassword(e) {
        e.preventDefault();
        const passwordInput = document.getElementById("reset-new-password");
        const confirmInput = document.getElementById("reset-confirm-password");

        const newPassword = passwordInput.value;
        const confirmPassword = confirmInput.value;

        // Validation (Regex)
        const validRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;
        
        if (!validRegex.test(newPassword)) {
            setMessage("reset-message", "Weak password. Please ensure all requirements are met.", "error");
            return;
        }

        if (newPassword !== confirmPassword) {
            setMessage("reset-message", "Passwords must match exactly.", "error");
            return;
        }

        setButtonLoading("reset-button", true, "Reset Password");

        try {
            await window.api.post("/api/auth/reset-password", { 
                email: resetEmail,
                newPassword: newPassword
            });
            
            setMessage("reset-message", "Password reset successfully! Redirecting to login...", "success");
            
            setTimeout(() => {
                window.location.href = `login.html?email=${encodeURIComponent(resetEmail)}`;
            }, 2000);

        } catch (error) {
            setMessage("reset-message", error.message || "Failed to reset password. Session may have expired.", "error");
        } finally {
            setButtonLoading("reset-button", false, "Reset Password");
        }
    }

    function initPasswordValidation() {
        const passInput = document.getElementById("reset-new-password");
        const confirmInput = document.getElementById("reset-confirm-password");
        const matchIndicator = document.getElementById("match-indicator");
        const submitBtn = document.getElementById("reset-button");
        const strengthContainer = document.getElementById("password-strength-container");
        const strengthBar = document.getElementById("strength-bar-fill");
        const strengthText = document.getElementById("strength-text");

        const validRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;

        function updateValidationState() {
            if (!passInput || !confirmInput) return;
            
            const val = passInput.value;
            const confirmVal = confirmInput.value;

            // Update requirements list
            const lenEl = document.getElementById("req-length");
            const upperEl = document.getElementById("req-upper");
            const lowerEl = document.getElementById("req-lower");
            const numEl = document.getElementById("req-number");
            const specEl = document.getElementById("req-special");
            
            const hasLen = val.length >= 8;
            const hasUpper = /[A-Z]/.test(val);
            const hasLower = /[a-z]/.test(val);
            const hasNum = /\d/.test(val);
            const hasSpec = /[^A-Za-z\d]/.test(val);

            if (lenEl) lenEl.className = hasLen ? "valid" : "";
            if (upperEl) upperEl.className = hasUpper ? "valid" : "";
            if (lowerEl) lowerEl.className = hasLower ? "valid" : "";
            if (numEl) numEl.className = hasNum ? "valid" : "";
            if (specEl) specEl.className = hasSpec ? "valid" : "";

            // Check overall regex validity
            const isRegexValid = validRegex.test(val);

            // Strength calculation
            if (val.length > 0) {
                strengthContainer.style.display = "flex";
                let strength = "weak";
                
                if (isRegexValid) {
                    if (val.length >= 12 && /[^A-Za-z\d].*[^A-Za-z\d]/.test(val)) {
                        strength = "strong";
                    } else {
                        strength = "medium";
                    }
                }

                strengthBar.className = `strength-bar ${strength}`;
                strengthText.className = `strength-text ${strength}`;
                strengthText.textContent = strength.charAt(0).toUpperCase() + strength.slice(1);
            } else {
                strengthContainer.style.display = "none";
            }

            // Match checking
            let isMatch = false;
            if (confirmVal.length > 0) {
                if (val === confirmVal) {
                    matchIndicator.textContent = "✔ Passwords match";
                    matchIndicator.className = "match-indicator match";
                    isMatch = true;
                } else {
                    matchIndicator.textContent = "❌ Passwords do not match";
                    matchIndicator.className = "match-indicator no-match";
                }
            } else {
                matchIndicator.textContent = "";
            }

            // Button toggle
            if (submitBtn) {
                submitBtn.disabled = !(isRegexValid && isMatch);
            }
        }

        if (passInput) passInput.addEventListener("input", updateValidationState);
        if (confirmInput) confirmInput.addEventListener("input", updateValidationState);
    }

    document.addEventListener("DOMContentLoaded", () => {
        resetEmail = getQueryParam("email");
        
        if (!resetEmail) {
            window.location.href = "forgot-password.html";
            return;
        }

        const displayEl = document.getElementById("reset-email-display");
        if (displayEl) {
            displayEl.textContent = resetEmail;
        }

        initPasswordValidation();

        const verifyForm = document.getElementById("verify-otp-form");
        if (verifyForm) {
            verifyForm.addEventListener("submit", handleVerifyOtp);
        }

        const newPassForm = document.getElementById("new-password-form");
        if (newPassForm) {
            newPassForm.addEventListener("submit", handleResetPassword);
        }
    });

})();
