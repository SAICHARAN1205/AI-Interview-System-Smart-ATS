/**
 * SmartATS Interview Monitoring Module
 * Handles: webcam, microphone, face detection, voice-to-text, anti-cheat
 */
(function () {
    "use strict";

    const monitor = {
        mediaStream: null,
        audioCtx: null,
        analyser: null,
        cameraReady: false,
        micReady: false,
        faceCheckInterval: null,
        micLevelInterval: null,
        antiCheatBound: false,
        fullscreenWarningShown: false,
        multipleFaceActive: false,
        recognition: null,
        recognizing: false,
        transcript: "",
        interimTranscript: "",
        warningQueue: [],
        warningTimer: null,
        tabSwitchCount: 0,
        faceWarningCount: 0,
        pendingEvents: [],
        sessionId: null,
        flushTimer: null,
        faceWarningActive: false,
        faceMesh: null,
        fmCamera: null,
        lookingAwaySince: 0,
        faceLostSince: 0
    };

    function byId(id) { return document.getElementById(id); }

    // ── Device Access ──────────────────────────────────────────
    async function requestDevices() {
        const modeInput = document.querySelector("input[name='monitoring-mode']:checked");
        const mode = modeInput ? modeInput.value : "Camera + Voice";
        const needsCamera = mode === "Camera + Voice" || mode === "Camera Only";
        const needsMic = mode === "Camera + Voice" || mode === "Voice Only";

        const camDot = byId("im-camera-dot");
        const micDot = byId("im-mic-dot");
        const camStatus = byId("im-camera-status");
        const micStatus = byId("im-mic-status");
        const previewWrap = byId("im-camera-preview-wrap");
        const micWrap = byId("im-mic-level-wrap");
        const warning = byId("im-device-warning");

        if (!needsCamera && !needsMic) {
            if (camStatus) camStatus.textContent = "Not required";
            if (micStatus) micStatus.textContent = "Not required";
            monitor.cameraReady = false;
            monitor.micReady = false;
            return;
        }

        try {
            monitor.mediaStream = await navigator.mediaDevices.getUserMedia({ video: needsCamera, audio: needsMic });

            // Camera
            if (needsCamera) {
                const video = byId("im-camera-preview");
                if (video) { video.srcObject = monitor.mediaStream; }
                if (camDot) camDot.classList.add("im-dot-ok");
                if (camStatus) camStatus.textContent = "Connected";
                monitor.cameraReady = true;
                if (previewWrap) previewWrap.classList.remove("hidden");
            } else {
                if (camStatus) camStatus.textContent = "Not required";
                monitor.cameraReady = false;
            }

            // Microphone
            if (needsMic) {
                monitor.audioCtx = new (window.AudioContext || window.webkitAudioContext)();
                const source = monitor.audioCtx.createMediaStreamSource(monitor.mediaStream);
                monitor.analyser = monitor.audioCtx.createAnalyser();
                monitor.analyser.fftSize = 256;
                source.connect(monitor.analyser);
                if (micDot) micDot.classList.add("im-dot-ok");
                if (micStatus) micStatus.textContent = "Connected";
                monitor.micReady = true;
                if (micWrap) micWrap.classList.remove("hidden");
                startMicLevelPreview();
            } else {
                if (micStatus) micStatus.textContent = "Not required";
                monitor.micReady = false;
            }

            if (warning) { warning.classList.add("hidden"); warning.textContent = ""; }
        } catch (err) {
            const msg = "Permissions denied. Some monitoring features will be disabled during the interview.";
            if (needsCamera && camDot) camDot.classList.add("im-dot-fail");
            if (needsMic && micDot) micDot.classList.add("im-dot-fail");
            if (needsCamera && camStatus) camStatus.textContent = "Denied";
            if (needsMic && micStatus) micStatus.textContent = "Denied";
            if (warning) {
                warning.textContent = msg;
                warning.className = "message-box show warning";
            }
            monitor.cameraReady = false;
            monitor.micReady = false;
            pushEvent("DEVICE_WARNING", "Permissions denied by candidate.");
        }
    }

    function startMicLevelPreview() {
        if (!monitor.analyser) return;
        const bar = byId("im-mic-level-bar");
        if (!bar) return;
        const data = new Uint8Array(monitor.analyser.frequencyBinCount);
        monitor.micLevelInterval = setInterval(function () {
            monitor.analyser.getByteFrequencyData(data);
            let sum = 0;
            for (let i = 0; i < data.length; i++) sum += data[i];
            const avg = sum / data.length;
            bar.style.width = Math.min(avg / 128 * 100, 100) + "%";
        }, 100);
    }

    function startLiveMicLevel() {
        if (!monitor.analyser) return;
        const bar = byId("im-live-mic-level");
        if (!bar) return;
        const data = new Uint8Array(monitor.analyser.frequencyBinCount);
        if (monitor.micLevelInterval) clearInterval(monitor.micLevelInterval);
        monitor.micLevelInterval = setInterval(function () {
            monitor.analyser.getByteFrequencyData(data);
            let sum = 0;
            for (let i = 0; i < data.length; i++) sum += data[i];
            const avg = sum / data.length;
            bar.style.height = Math.min(avg / 128 * 100, 100) + "%";
        }, 100);
    }

    function attachLiveWebcam() {
        const video = byId("im-live-webcam");
        if (video && monitor.mediaStream) {
            video.srcObject = monitor.mediaStream;
        }
    }

    function stopDevices() {
        if (monitor.mediaStream) {
            monitor.mediaStream.getTracks().forEach(function (t) { t.stop(); });
            monitor.mediaStream = null;
        }
        if (monitor.audioCtx) {
            try { monitor.audioCtx.close(); } catch (e) { /* ignore */ }
            monitor.audioCtx = null; monitor.analyser = null;
        }
        if (monitor.micLevelInterval) { clearInterval(monitor.micLevelInterval); monitor.micLevelInterval = null; }
        monitor.cameraReady = false;
        monitor.micReady = false;
    }

    // ── Professional Face Mesh Detection ──────────────────────
    function initFaceMesh() {
        if (!window.FaceMesh || !window.Camera) {
            return;
        }

        const video = byId("im-live-webcam");
        if (!video) return;

        monitor.faceMesh = new window.FaceMesh({locateFile: function(file) {
            return "https://cdn.jsdelivr.net/npm/@mediapipe/face_mesh/" + file;
        }});
        
        monitor.faceMesh.setOptions({
            maxNumFaces: 1,
            refineLandmarks: true,
            minDetectionConfidence: 0.5,
            minTrackingConfidence: 0.5
        });

        monitor.faceMesh.onResults(onFaceMeshResults);

        monitor.fmCamera = new window.Camera(video, {
            onFrame: async function() {
                if (monitor.faceMesh) {
                    await monitor.faceMesh.send({image: video});
                }
            },
            width: 640,
            height: 480
        });
        
        monitor.fmCamera.start();
        monitor.lookingAwaySince = 0;
        monitor.faceLostSince = 0;
    }

    function onFaceMeshResults(results) {
        const overlay = byId("im-cam-overlay-msg");
        const yawEl = byId("debug-yaw");
        const pitchEl = byId("debug-pitch");
        const rollEl = byId("debug-roll");
        const statusEl = byId("debug-status");

        if (!results.multiFaceLandmarks || results.multiFaceLandmarks.length === 0) {
            // Face missing
            if (monitor.faceLostSince === 0) {
                monitor.faceLostSince = Date.now();
            } else if (Date.now() - monitor.faceLostSince > 3000) {
                showFaceWarning(overlay, "Please remain visible on camera.");
                if (statusEl) statusEl.textContent = "LOST";
            }
            monitor.lookingAwaySince = 0;
            if (yawEl) yawEl.textContent = "-";
            if (pitchEl) pitchEl.textContent = "-";
            if (rollEl) rollEl.textContent = "-";
            return;
        }

        if (results.multiFaceLandmarks.length > 1) {
            if (!monitor.multipleFaceActive) {
                monitor.multipleFaceActive = true;
                monitor.faceWarningCount++;
                updateStat("im-stat-face", monitor.faceWarningCount);
                pushEvent("MULTIPLE_FACE", "Multiple faces detected in camera frame.");
                showWarningToast("Multiple faces detected.");
            }
            if (overlay) {
                overlay.textContent = "Only one face should be visible.";
                overlay.classList.remove("hidden");
            }
            return;
        }
        monitor.multipleFaceActive = false;

        // Face found
        monitor.faceLostSince = 0;
        const landmarks = results.multiFaceLandmarks[0];

        // 3D coordinates for head pose estimation
        // Nose tip: 1, Left Eye: 33, Right Eye: 263, Chin: 152
        const nose = landmarks[1];
        const leftEye = landmarks[33];
        const rightEye = landmarks[263];
        const chin = landmarks[152];

        // Basic Yaw: ratio of horizontal distance from nose to eyes
        const leftDist = Math.abs(nose.x - leftEye.x);
        const rightDist = Math.abs(nose.x - rightEye.x);
        let yawRatio = leftDist / (rightDist || 0.001);
        if (yawRatio < 1) yawRatio = 1 / yawRatio;

        // Pitch based on nose vertical position relative to eyes and chin
        const eyeCenterY = (leftEye.y + rightEye.y) / 2;
        const faceHeight = chin.y - eyeCenterY;
        const noseToEyes = nose.y - eyeCenterY;
        const pitchRatio = noseToEyes / (faceHeight || 0.001);

        // Roll: angle between eyes
        const deltaY = rightEye.y - leftEye.y;
        const deltaX = rightEye.x - leftEye.x;
        const rollAngle = Math.atan2(deltaY, deltaX) * (180 / Math.PI);

        if (yawEl) yawEl.textContent = yawRatio.toFixed(2);
        if (pitchEl) pitchEl.textContent = pitchRatio.toFixed(2);
        if (rollEl) rollEl.textContent = Math.abs(rollAngle).toFixed(1) + "°";

        // Thresholds
        const isLookingAway = yawRatio > 2.0 || pitchRatio < 0.15 || pitchRatio > 0.65 || Math.abs(rollAngle) > 30;

        if (isLookingAway) {
            if (statusEl) statusEl.textContent = "AWAY";
            if (monitor.lookingAwaySince === 0) {
                monitor.lookingAwaySince = Date.now();
            } else if (Date.now() - monitor.lookingAwaySince > 2500) {
                showFaceWarning(overlay, "Looking away detected.");
                pushEvent("LOOKING_AWAY", "Candidate looked away from camera for extended duration.");
            }
        } else {
            if (statusEl) statusEl.textContent = "OK";
            monitor.lookingAwaySince = 0;
            hideFaceWarning(overlay);
        }
    }

    function startFaceDetection() {
        const modeInput = document.querySelector("input[name='monitoring-mode']:checked");
        const mode = modeInput ? modeInput.value : "Camera + Voice";
        if (mode === "Voice Only" || mode === "Text Only") return;
        
        initFaceMesh();
    }

    function showFaceWarning(overlay, msg) {
        if (!monitor.faceWarningActive) {
            monitor.faceWarningActive = true;
            monitor.faceWarningCount++;
            updateStat("im-stat-face", monitor.faceWarningCount);
            pushEvent("FACE_WARNING", msg || "Face not detected.");
            showWarningToast(msg || "Face not detected.");
        }
        if (overlay) { overlay.textContent = msg; overlay.classList.remove("hidden"); }
    }

    function hideFaceWarning(overlay) {
        monitor.faceWarningActive = false;
        if (overlay) overlay.classList.add("hidden");
    }

    function stopFaceDetection() {
        if (monitor.fmCamera) {
            monitor.fmCamera.stop();
            monitor.fmCamera = null;
        }
        if (monitor.faceMesh) {
            monitor.faceMesh.close();
            monitor.faceMesh = null;
        }
    }

    // ── Anti-Cheat ────────────────────────────────────────────
    function startAntiCheat() {
        if (monitor.antiCheatBound) return;
        monitor.antiCheatBound = true;

        document.addEventListener("visibilitychange", onVisibilityChange);
        window.addEventListener("blur", onWindowBlur);
        document.addEventListener("copy", onCopyPaste);
        document.addEventListener("paste", onCopyPaste);
        document.addEventListener("cut", onCopyPaste);
        document.addEventListener("keydown", onKeyDown);
        document.addEventListener("contextmenu", onContextMenu);
        document.addEventListener("fullscreenchange", onFullscreenChange);
    }

    function onVisibilityChange() {
        if (document.hidden) {
            monitor.tabSwitchCount++;
            updateStat("im-stat-tab", monitor.tabSwitchCount);
            pushEvent("TAB_SWITCH", "Tab switching detected.");
            showWarningToast("Tab switching detected. Please stay on the interview page.");
        }
    }

    function onWindowBlur() {
        // Don't double-count if visibility already caught it
    }

    function onCopyPaste(e) {
        if (e.target && e.target.id === "live-answer") {
            e.preventDefault();
            pushEvent("COPY_PASTE", e.type + " attempt blocked.");
            showWarningToast("Copy/paste is disabled during mock interview.");
        }
    }

    function onKeyDown(e) {
        if (e.target && e.target.id === "live-answer") {
            if ((e.ctrlKey || e.metaKey) && (e.key.toLowerCase() === "c" || e.key.toLowerCase() === "v" || e.key.toLowerCase() === "x")) {
                e.preventDefault();
                pushEvent("COPY_PASTE", "Keyboard shortcut blocked.");
                showWarningToast("Copy/paste shortcuts are disabled.");
            }
        }
    }

    function onContextMenu(e) {
        if (e.target && e.target.id === "live-answer") {
            e.preventDefault();
            pushEvent("CONTEXT_MENU", "Right-click blocked.");
            showWarningToast("Right-click menu is disabled.");
        }
    }

    function onFullscreenChange() {
        if (!document.fullscreenElement) {
            pushEvent("FULLSCREEN_EXIT", "Candidate exited fullscreen mode.");
            if (!monitor.fullscreenWarningShown) {
                monitor.fullscreenWarningShown = true;
                showWarningToast("Fullscreen exit detected.");
            }
        } else {
            monitor.fullscreenWarningShown = false;
        }
    }

    function stopAntiCheat() {
        document.removeEventListener("visibilitychange", onVisibilityChange);
        window.removeEventListener("blur", onWindowBlur);
        document.removeEventListener("copy", onCopyPaste);
        document.removeEventListener("paste", onCopyPaste);
        document.removeEventListener("cut", onCopyPaste);
        document.removeEventListener("keydown", onKeyDown);
        document.removeEventListener("contextmenu", onContextMenu);
        document.removeEventListener("fullscreenchange", onFullscreenChange);
        monitor.antiCheatBound = false;
    }

    // ── Voice Recording (SpeechRecognition) ───────────────────
    function initSpeechRecognition() {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRecognition) {
            const status = byId("im-voice-status");
            if (status) status.textContent = "Speech recognition not supported in this browser. Use text input.";
            const startBtn = byId("im-voice-start");
            if (startBtn) startBtn.disabled = true;
            return;
        }

        monitor.recognition = new SpeechRecognition();
        monitor.recognition.continuous = true;
        monitor.recognition.interimResults = true;
        monitor.recognition.lang = "en-US";

        monitor.recognition.onresult = function (event) {
            let interim = "";
            let final = "";
            for (let i = event.resultIndex; i < event.results.length; i++) {
                if (event.results[i].isFinal) {
                    final += event.results[i][0].transcript + " ";
                } else {
                    interim += event.results[i][0].transcript;
                }
            }
            if (final) {
                monitor.transcript += final;
                const textarea = byId("live-answer");
                if (textarea) {
                    textarea.value = monitor.transcript.trim();
                    textarea.dispatchEvent(new Event("input", { bubbles: true }));
                }
            }
            monitor.interimTranscript = interim;
            renderTranscript();
        };

        monitor.recognition.onerror = function (event) {
            if (event.error === "no-speech") return;
            const status = byId("im-voice-status");
            if (status) status.textContent = "Speech error: " + event.error;
        };

        monitor.recognition.onend = function () {
            if (monitor.recognizing) {
                // Auto-restart if still recording
                try { monitor.recognition.start(); } catch (e) { /* ignore */ }
            }
        };
    }

    function startRecording() {
        if (!monitor.recognition) return;
        monitor.transcript = byId("live-answer")?.value || "";
        monitor.interimTranscript = "";
        monitor.recognizing = true;
        try { monitor.recognition.start(); } catch (e) { /* already started */ }
        byId("im-voice-start")?.classList.add("hidden");
        byId("im-voice-stop")?.classList.remove("hidden");
        byId("im-voice-retry")?.classList.add("hidden");
        byId("im-recording-badge")?.classList.remove("hidden");
        byId("im-transcript-panel")?.classList.remove("hidden");
        const status = byId("im-voice-status");
        if (status) status.textContent = "Listening…";
    }

    function stopRecording() {
        monitor.recognizing = false;
        if (monitor.recognition) {
            try { monitor.recognition.stop(); } catch (e) { /* ignore */ }
        }
        byId("im-voice-start")?.classList.remove("hidden");
        byId("im-voice-stop")?.classList.add("hidden");
        byId("im-voice-retry")?.classList.remove("hidden");
        byId("im-recording-badge")?.classList.add("hidden");
        const status = byId("im-voice-status");
        if (status) status.textContent = "Recording stopped.";
    }

    function retryRecording() {
        monitor.transcript = "";
        monitor.interimTranscript = "";
        const textarea = byId("live-answer");
        if (textarea) { textarea.value = ""; textarea.dispatchEvent(new Event("input", { bubbles: true })); }
        renderTranscript();
        startRecording();
    }

    function renderTranscript() {
        const panel = byId("im-transcript-text");
        if (!panel) return;
        const final = monitor.transcript || "";
        const interim = monitor.interimTranscript || "";
        panel.innerHTML = escapeHtml(final) + (interim ? '<span class="im-interim">' + escapeHtml(interim) + "</span>" : "");
        panel.scrollTop = panel.scrollHeight;
    }

    function resetVoiceState() {
        monitor.transcript = "";
        monitor.interimTranscript = "";
        monitor.recognizing = false;
        byId("im-voice-start")?.classList.remove("hidden");
        byId("im-voice-stop")?.classList.add("hidden");
        byId("im-voice-retry")?.classList.add("hidden");
        byId("im-recording-badge")?.classList.add("hidden");
        byId("im-transcript-panel")?.classList.add("hidden");
        const txt = byId("im-transcript-text");
        if (txt) txt.innerHTML = "";
        const status = byId("im-voice-status");
        if (status) status.textContent = "";
    }

    // ── Warning Toast ─────────────────────────────────────────
    function showWarningToast(message) {
        const toast = byId("im-warning-toast");
        const text = byId("im-warning-text");
        if (!toast || !text) return;
        text.textContent = message;
        toast.classList.remove("hidden");
        toast.classList.add("im-toast-visible");
        if (monitor.warningTimer) clearTimeout(monitor.warningTimer);
        monitor.warningTimer = setTimeout(function () {
            toast.classList.remove("im-toast-visible");
            setTimeout(function () { toast.classList.add("hidden"); }, 400);
        }, 4000);
    }

    // ── Event Tracking & Flush ────────────────────────────────
    function pushEvent(type, message) {
        monitor.pendingEvents.push({
            type: type,
            message: message,
            timestamp: new Date().toISOString()
        });
    }

    function flushEvents() {
        if (!monitor.sessionId || !monitor.pendingEvents.length) return;
        const events = monitor.pendingEvents.slice();
        monitor.pendingEvents = [];
        if (window.api) {
            window.api.put("/api/interview/sessions/" + monitor.sessionId + "/monitoring", events).catch(function () {
                // Re-queue on failure
                monitor.pendingEvents = events.concat(monitor.pendingEvents);
            });
        }
    }

    function startEventFlushing(sessionId) {
        monitor.sessionId = sessionId;
        monitor.flushTimer = setInterval(flushEvents, 15000);
    }

    function stopEventFlushing() {
        if (monitor.flushTimer) { clearInterval(monitor.flushTimer); monitor.flushTimer = null; }
        flushEvents(); // final flush
    }

    // ── Helpers ───────────────────────────────────────────────
    function updateStat(id, val) {
        const el = byId(id);
        if (el) el.textContent = String(val);
    }

    function escapeHtml(val) {
        return String(val == null ? "" : val)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;")
            .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
    }

    // ── Public API ────────────────────────────────────────────
    window.interviewMonitor = {
        requestDevices: requestDevices,
        isReady: function () { return true; },
        startLiveMonitoring: function (sessionId) {
            attachLiveWebcam();
            startLiveMicLevel();
            startFaceDetection();
            startAntiCheat();
            initSpeechRecognition();
            startEventFlushing(sessionId);
        },
        stopAll: function () {
            stopRecording();
            stopFaceDetection();
            stopAntiCheat();
            stopEventFlushing();
            stopDevices();
        },
        startRecording: startRecording,
        stopRecording: stopRecording,
        retryRecording: retryRecording,
        resetVoiceState: resetVoiceState,
        getTranscript: function () { return monitor.transcript; },
        getMonitoringPayload: function () {
            const events = monitor.pendingEvents.slice();
            monitor.pendingEvents = [];
            return {
                monitoringEvents: events,
                transcript: monitor.transcript || null,
                cameraUsed: monitor.cameraReady,
                microphoneUsed: monitor.micReady
            };
        },
        getStats: function () {
            return {
                tabSwitchCount: monitor.tabSwitchCount,
                faceWarningCount: monitor.faceWarningCount
            };
        },
        showWarningToast: showWarningToast
    };
})();

