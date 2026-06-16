(function () {
    let countdownInterval;
    let emailToVerify = "";
    let currentSessionId = "";

    function getQueryParam(name) {
        const params = new URLSearchParams(window.location.search);
        return params.get(name);
    }

    function setMessage(message, type) {
        const messageBox = document.getElementById("verify-message");
        if (!messageBox) return;

        messageBox.textContent = message;
        messageBox.className = `message-box show ${type}`;
    }

    function setButtonLoading(isLoading) {
        const btn = document.getElementById("verify-button");
        if (!btn) return;

        if (!btn.dataset.defaultText) {
            btn.dataset.defaultText = btn.textContent;
        }

        btn.disabled = isLoading;
        btn.textContent = isLoading ? "Verifying..." : btn.dataset.defaultText;
    }

    function persistVerificationSession() {
        try {
            sessionStorage.setItem("verification_session", JSON.stringify({
                email: emailToVerify,
                sessionId: currentSessionId || ""
            }));
        } catch (error) {
            // Ignore storage failures.
        }
    }

    function startTimer(durationInSeconds) {
        let timer = durationInSeconds;
        const timerCount = document.getElementById("timer-count");
        const timerText = document.getElementById("timer-text");
        const resendBtn = document.getElementById("resend-btn");

        timerText.style.display = "inline";
        resendBtn.style.display = "none";

        clearInterval(countdownInterval);

        countdownInterval = setInterval(() => {
            let minutes = parseInt(timer / 60, 10);
            let seconds = parseInt(timer % 60, 10);

            minutes = minutes < 10 ? "0" + minutes : minutes;
            seconds = seconds < 10 ? "0" + seconds : seconds;

            if (timerCount) {
                timerCount.textContent = minutes + ":" + seconds;
            }

            if (--timer < 0) {
                clearInterval(countdownInterval);
                timerText.style.display = "none";
                resendBtn.style.display = "inline";
                resendBtn.classList.remove("disabled");
            }
        }, 1000);
    }

    function setupOtpInputs() {
        const inputs = document.querySelectorAll(".otp-input");
        inputs.forEach((input, index) => {
            input.addEventListener("input", (e) => {
                if (e.target.value.length === 1 && index < inputs.length - 1) {
                    inputs[index + 1].focus();
                }
            });
            input.addEventListener("keydown", (e) => {
                if (e.key === "Backspace" && e.target.value === "" && index > 0) {
                    inputs[index - 1].focus();
                }
            });
            // Handle paste
            input.addEventListener("paste", (e) => {
                e.preventDefault();
                const pastedData = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, 6);
                if (pastedData) {
                    pastedData.split("").forEach((char, i) => {
                        if (i < inputs.length) {
                            inputs[i].value = char;
                        }
                    });
                    if (pastedData.length < 6) {
                        inputs[pastedData.length].focus();
                    } else {
                        inputs[5].focus();
                    }
                }
            });
        });
    }

    async function verifyOtp(e) {
        e.preventDefault();
        
        const inputs = document.querySelectorAll(".otp-input");
        let otp = "";
        inputs.forEach(input => otp += input.value);

        if (otp.length !== 6) {
            setMessage("Please enter the full 6-digit OTP", "error");
            return;
        }

        setButtonLoading(true);

        try {
            await window.api.post("/api/auth/verify-registration-otp", {
                email: emailToVerify,
                otp: otp,
                sessionId: currentSessionId
            });

            sessionStorage.removeItem("verification_session");
            setMessage("Email verified successfully! Redirecting to login...", "success");
            
            setTimeout(() => {
                window.location.href = `login.html?email=${encodeURIComponent(emailToVerify)}`;
            }, 1500);

        } catch (error) {
            setMessage(error.message || "Invalid OTP. Please try again.", "error");
            inputs.forEach(input => input.value = "");
            inputs[0].focus();
            setButtonLoading(false);
        }
    }

    async function resendOtp() {
        const resendBtn = document.getElementById("resend-btn");
        if (resendBtn.classList.contains("disabled")) return;

        resendBtn.classList.add("disabled");
        resendBtn.textContent = "Sending...";

        try {
            const response = await window.api.post("/api/auth/resend-registration-otp", {
                email: emailToVerify
            });
            currentSessionId = response && response.sessionId ? response.sessionId : currentSessionId;
            persistVerificationSession();
            
            setMessage("A new OTP has been sent to your email.", "success");
            resendBtn.textContent = "Resend OTP";
            startTimer(300); // 5 minutes

        } catch (error) {
            setMessage(error.message || "Failed to resend OTP.", "error");
            resendBtn.textContent = "Resend OTP";
            resendBtn.classList.remove("disabled");
        }
    }

    document.addEventListener("DOMContentLoaded", () => {
        const sessionStr = sessionStorage.getItem("verification_session");
        if (sessionStr) {
            try {
                const session = JSON.parse(sessionStr);
                emailToVerify = session.email;
                currentSessionId = session.sessionId || "";
            } catch (e) {}
        }
        
        if (!emailToVerify) {
            emailToVerify = getQueryParam("email");
        }
        if (!currentSessionId) {
            currentSessionId = getQueryParam("sessionId") || "";
        }
        
        if (!emailToVerify) {
            window.location.href = "login.html";
            return;
        }

        persistVerificationSession();

        const displayEl = document.getElementById("verify-email-display");
        if (displayEl) {
            displayEl.textContent = emailToVerify;
        }

        setupOtpInputs();
        startTimer(300); // 5 minutes

        const form = document.getElementById("verify-form");
        if (form) {
            form.addEventListener("submit", verifyOtp);
        }

        const resendBtn = document.getElementById("resend-btn");
        if (resendBtn) {
            resendBtn.addEventListener("click", resendOtp);
        }
    });

})();
