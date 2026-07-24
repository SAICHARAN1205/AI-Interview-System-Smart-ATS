const testRegistration = async () => {
    try {
        console.log("Fetching CAPTCHA...");
        const captchaRes = await fetch("https://ai-interview-system-smart-ats.onrender.com/api/auth/captcha");
        const captchaData = await captchaRes.json();
        
        const token = captchaData.data.token;
        const question = captchaData.data.question;
        const answer = eval(question.replace("= ?", "").trim()).toString();

        console.log("CAPTCHA Answer:", answer);

        const payload = {
            name: "Test User",
            email: "test.smartats.debug@gmail.com",
            password: "TestPassword123!",
            role: "CANDIDATE",
            captchaToken: token,
            captchaAnswer: answer
        };

        console.log("Sending registration request...");
        const regRes = await fetch("https://ai-interview-system-smart-ats.onrender.com/api/users/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        const regData = await regRes.text();
        console.log("Registration Response Status:", regRes.status);
        console.log("Registration Response Body:", regData);
    } catch (e) {
        console.error("Error:", e);
    }
};

testRegistration();
