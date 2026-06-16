/**
 * hiring-flow.js — Advanced hiring workflow features
 * Toast notifications, Application modal, Search/Filter/Sort, Status tabs
 */
(function () {
    var PROFILE_KEY = "ai_hiring_platform_profile";

    // ─── Toast System ───
    function showToast(message, type, duration) {
        if (window.smartUi && typeof window.smartUi.showToast === "function") {
            return window.smartUi.showToast(message, type, duration);
        }

        type = type || "success";
        duration = duration || 4000;
        var container = document.getElementById("toast-container");
        if (!container) {
            container = document.createElement("div");
            container.id = "toast-container";
            container.className = "toast-container";
            document.body.appendChild(container);
        }
        var icons = { success: "✓", error: "✕", info: "ℹ" };
        var toast = document.createElement("div");
        toast.className = "toast " + type;
        toast.innerHTML =
            '<span class="toast-icon">' + (icons[type] || "ℹ") + "</span>" +
            '<span class="toast-msg">' + escHtml(message) + "</span>" +
            '<button class="toast-close" aria-label="Close">✕</button>';
        container.appendChild(toast);
        var close = function () {
            toast.classList.add("out");
            setTimeout(function () { toast.remove(); }, 350);
        };
        toast.querySelector(".toast-close").addEventListener("click", close);
        setTimeout(close, duration);
    }
    window.showToast = showToast;

    // ─── Profile helper ───
    function getProfile() {
        try { return JSON.parse(localStorage.getItem(PROFILE_KEY) || "{}"); }
        catch (e) { return {}; }
    }

    // ─── File size formatter ───
    function formatFileSize(bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return (bytes / 1024).toFixed(1) + " KB";
        return (bytes / 1048576).toFixed(1) + " MB";
    }

    // ─── Application Modal ───
    function openApplyModal(job, onSubmit) {
        var root = document.getElementById("apply-modal-root");
        if (!root) return;

        var userObj = (window.api && window.api.getUser()) || {};
        var email = typeof userObj === "string" ? userObj : (userObj.email || "");
        var profile = getProfile();
        var displayName = profile.name || (email ? email.split("@")[0] : "") || "";
        var phone = profile.phone || "";
        var skills = profile.skills || "";
        var hasProfileResume = Boolean(profile.resumeFileName);

        root.innerHTML =
            '<div class="modal-overlay" id="apply-modal-overlay">' +
                '<div class="modal">' +
                    // Step indicator
                    '<div class="modal-steps">' +
                        '<div class="modal-step active" id="step-1">' +
                            '<span class="modal-step-num">1</span>' +
                            '<span>Details</span>' +
                        '</div>' +
                        '<div class="modal-step-line" id="step-line-1"></div>' +
                        '<div class="modal-step" id="step-2">' +
                            '<span class="modal-step-num">2</span>' +
                            '<span>Resume</span>' +
                        '</div>' +
                        '<div class="modal-step-line" id="step-line-2"></div>' +
                        '<div class="modal-step" id="step-3">' +
                            '<span class="modal-step-num">3</span>' +
                            '<span>Submit</span>' +
                        '</div>' +
                    '</div>' +
                    // Header
                    '<div class="modal-header">' +
                        '<h2>Apply to ' + escHtml(job.title || "this role") + '</h2>' +
                        '<button class="modal-close" data-close-modal aria-label="Close">✕</button>' +
                    '</div>' +
                    '<div class="modal-body" id="apply-modal-body">' +
                        '<p style="color:var(--c-muted);font-size:.9rem;margin-bottom:1rem">' +
                            escHtml(job.companyName || "") + (job.location ? " — " + escHtml(job.location) : "") +
                            (job.salary || job.salaryRange ? ' · <strong>' + escHtml(job.salary || job.salaryRange) + '</strong>' : '') +
                        '</p>' +
                        '<div class="ln-form-grid">' +
                            '<div class="ln-field" id="field-name">' +
                                '<label for="apply-name">Full Name <span style="color:var(--c-danger)">*</span></label>' +
                                '<input type="text" id="apply-name" value="' + escAttr(displayName) + '" placeholder="Your full name">' +
                                '<span class="ln-field-error">Full name is required</span>' +
                            '</div>' +
                            '<div class="ln-field">' +
                                '<label for="apply-email">Email</label>' +
                                '<input type="email" id="apply-email" value="' + escAttr(email) + '" readonly>' +
                            '</div>' +
                            '<div class="ln-field">' +
                                '<label for="apply-phone">Phone Number</label>' +
                                '<input type="tel" id="apply-phone" value="' + escAttr(phone) + '" placeholder="+91 9876543210">' +
                            '</div>' +
                            '<div class="ln-field">' +
                                '<label for="apply-skills">Skills</label>' +
                                '<input type="text" id="apply-skills" value="' + escAttr(skills) + '" placeholder="Java, React, Python">' +
                            '</div>' +
                        '</div>' +
                        '<div class="modal-divider"></div>' +
                        '<p style="font-weight:700;font-size:.88rem;margin-bottom:.5rem">Resume</p>' +
                        '<div class="resume-options">' +
                            '<label class="resume-option' + (hasProfileResume ? " selected" : "") + '" id="ro-profile">' +
                                '<input type="radio" name="resume-choice" value="profile"' + (hasProfileResume ? " checked" : "") + (hasProfileResume ? "" : " disabled") + '>' +
                                '<div class="ro-icon">📄</div>' +
                                '<div class="ro-label">Profile Resume</div>' +
                                '<div class="ro-desc">' + (hasProfileResume ? escHtml(profile.resumeFileName) : "No resume in profile") + '</div>' +
                            '</label>' +
                            '<label class="resume-option' + (!hasProfileResume ? " selected" : "") + '" id="ro-upload">' +
                                '<input type="radio" name="resume-choice" value="upload"' + (!hasProfileResume ? " checked" : "") + '>' +
                                '<div class="ro-icon">📎</div>' +
                                '<div class="ro-label">Upload New</div>' +
                                '<div class="ro-desc">PDF or DOC file</div>' +
                            '</label>' +
                        '</div>' +
                        '<div id="apply-upload-area"' + (hasProfileResume ? ' class="hidden"' : '') + '>' +
                            '<div class="ln-field" style="margin-top:.5rem" id="field-resume-file">' +
                                '<label for="apply-resume-file">Select File</label>' +
                                '<input type="file" id="apply-resume-file" accept=".pdf,.doc,.docx">' +
                            '</div>' +
                            '<div id="apply-file-preview"></div>' +
                        '</div>' +
                    '</div>' +
                    '<div class="modal-footer">' +
                        '<button class="btn btn-outline" data-close-modal>Cancel</button>' +
                        '<button class="btn btn-primary" id="apply-submit-btn">Submit Application</button>' +
                    '</div>' +
                '</div>' +
            '</div>';

        // Update step indicator state
        function setStep(n) {
            for (var i = 1; i <= 3; i++) {
                var stepEl = document.getElementById('step-' + i);
                var lineEl = document.getElementById('step-line-' + (i - 1));
                if (stepEl) {
                    stepEl.className = 'modal-step' + (i < n ? ' completed' : '') + (i === n ? ' active' : '');
                }
                if (lineEl) {
                    lineEl.className = 'modal-step-line' + (i < n ? ' done' : '');
                }
            }
        }

        // Resume option toggle
        root.querySelectorAll('.resume-option').forEach(function (opt) {
            opt.addEventListener('click', function () {
                var radio = opt.querySelector('input[type="radio"]');
                if (radio && radio.disabled) return;
                root.querySelectorAll('.resume-option').forEach(function (o) { o.classList.remove('selected'); });
                opt.classList.add('selected');
                if (radio) radio.checked = true;
                var uploadArea = document.getElementById('apply-upload-area');
                if (opt.querySelector('input').value === 'upload') {
                    uploadArea.classList.remove('hidden');
                } else {
                    uploadArea.classList.add('hidden');
                }
                setStep(2);
            });
        });

        // File preview
        var fileInput = document.getElementById('apply-resume-file');
        if (fileInput) {
            fileInput.addEventListener('change', function () {
                var preview = document.getElementById('apply-file-preview');
                if (!preview) return;
                if (fileInput.files && fileInput.files[0]) {
                    var f = fileInput.files[0];
                    preview.innerHTML =
                        '<div class="file-preview">' +
                            '<span class="file-preview-icon">📄</span>' +
                            '<span class="file-preview-name">' + escHtml(f.name) + '</span>' +
                            '<span class="file-preview-size">' + formatFileSize(f.size) + '</span>' +
                            '<button type="button" class="file-preview-remove" aria-label="Remove file">✕</button>' +
                        '</div>';
                    preview.querySelector('.file-preview-remove').addEventListener('click', function () {
                        fileInput.value = '';
                        preview.innerHTML = '';
                    });
                    setStep(2);
                } else {
                    preview.innerHTML = '';
                }
            });
        }

        // Track interactions to advance step indicator
        var detailInputs = root.querySelectorAll('#apply-name, #apply-phone, #apply-skills');
        detailInputs.forEach(function (inp) {
            inp.addEventListener('focus', function () { setStep(1); });
        });

        // Close handlers
        function closeModal() {
            var overlay = document.getElementById('apply-modal-overlay');
            if (overlay) {
                overlay.classList.add('closing');
                setTimeout(function () { root.innerHTML = ''; }, 250);
            }
        }
        root.querySelectorAll('[data-close-modal]').forEach(function (el) {
            el.addEventListener('click', closeModal);
        });
        root.querySelector('.modal-overlay').addEventListener('click', function (e) {
            if (e.target === e.currentTarget) closeModal();
        });

        // Validation
        function validateForm() {
            var valid = true;
            var nameField = document.getElementById('field-name');
            var nameInput = document.getElementById('apply-name');
            if (nameField && nameInput) {
                if (!nameInput.value.trim()) {
                    nameField.classList.add('has-error');
                    valid = false;
                } else {
                    nameField.classList.remove('has-error');
                }
            }
            // Validate file if upload selected
            var resumeChoice = root.querySelector('input[name="resume-choice"]:checked');
            if (resumeChoice && resumeChoice.value === 'upload') {
                var fileInp = document.getElementById('apply-resume-file');
                var fileField = document.getElementById('field-resume-file');
                if (fileInp && fileField && (!fileInp.files || !fileInp.files[0])) {
                    fileField.classList.add('has-error');
                    if (!fileField.querySelector('.ln-field-error')) {
                        var errSpan = document.createElement('span');
                        errSpan.className = 'ln-field-error';
                        errSpan.textContent = 'Please select a resume file';
                        errSpan.style.display = 'block';
                        fileField.appendChild(errSpan);
                    } else {
                        fileField.querySelector('.ln-field-error').style.display = 'block';
                    }
                    valid = false;
                } else if (fileField) {
                    fileField.classList.remove('has-error');
                    var errEl = fileField.querySelector('.ln-field-error');
                    if (errEl) errEl.style.display = 'none';
                }
            }
            return valid;
        }

        // Submit
        document.getElementById('apply-submit-btn').addEventListener('click', function () {
            if (!validateForm()) return;

            setStep(3);
            var btn = this;
            btn.disabled = true;
            btn.textContent = 'Submitting...';

            var snapshot = {
                name: document.getElementById('apply-name').value.trim(),
                email: document.getElementById('apply-email').value.trim(),
                phone: document.getElementById('apply-phone').value.trim(),
                skills: document.getElementById('apply-skills').value.trim(),
                resumeChoice: root.querySelector('input[name="resume-choice"]:checked')?.value || 'profile',
                resumeFile: null,
            };

            var customFileInput = document.getElementById('apply-resume-file');
            if (snapshot.resumeChoice === 'upload' && customFileInput && customFileInput.files[0]) {
                snapshot.resumeFile = customFileInput.files[0];
            }

            // Save snapshot to localStorage for this job
            try {
                var snapshots = JSON.parse(localStorage.getItem('app_snapshots') || '{}');
                snapshots[String(job.id)] = {
                    name: snapshot.name,
                    email: snapshot.email,
                    phone: snapshot.phone,
                    skills: snapshot.skills,
                    resumeChoice: snapshot.resumeChoice,
                    resumeFileName: snapshot.resumeFile ? snapshot.resumeFile.name : (profile.resumeFileName || ''),
                    appliedAt: new Date().toISOString(),
                };
                localStorage.setItem('app_snapshots', JSON.stringify(snapshots));
            } catch (e) {}

            // Upload resume if new file provided
            var uploadPromise = Promise.resolve();
            if (snapshot.resumeFile) {
                var formData = new FormData();
                formData.append('file', snapshot.resumeFile);
                uploadPromise = fetch((window.api.baseURL || '') + '/api/resumes/upload', {
                    method: 'POST',
                    headers: { Authorization: 'Bearer ' + window.api.getToken() },
                    body: formData,
                }).catch(function () {}); // Non-blocking
            }

            uploadPromise.then(function () {
                if (snapshot.resumeFile && window.api.refreshProfileResumeState) {
                    window.api.refreshProfileResumeState();
                }
                if (typeof onSubmit === 'function') {
                    onSubmit(job, snapshot, closeModal);
                } else {
                    // Show success animation
                    var body = document.getElementById('apply-modal-body');
                    if (body) {
                        body.innerHTML =
                            '<div class="submit-success-anim">' +
                                '<div class="success-checkmark">✓</div>' +
                                '<h3>Application Submitted!</h3>' +
                                '<p>Your application for ' + escHtml(job.title || 'this role') + ' has been sent successfully.</p>' +
                            '</div>';
                    }
                    var footer = root.querySelector('.modal-footer');
                    if (footer) footer.innerHTML = '<button class="btn btn-primary" data-close-modal>Done</button>';
                    root.querySelector('[data-close-modal]').addEventListener('click', closeModal);
                    setTimeout(closeModal, 2500);
                }
            });
        });
    }
    window.openApplyModal = openApplyModal;

    // ─── Search / Filter / Sort ───
    var companyFilterPopulated = false;

    function initSearchFilter() {
        var searchInput = document.getElementById('jobs-search');
        var sortSelect = document.getElementById('jobs-sort');
        var locationFilter = document.getElementById('jobs-location-filter');
        var salaryFilter = document.getElementById('jobs-salary-filter');
        var companyFilter = document.getElementById('jobs-company-filter');
        var skillsFilter = document.getElementById('jobs-skills-filter');
        var searchPanel = document.getElementById('jobs-search-panel');

        if (!searchInput || !window.jobsApp) return;

        // Show search panel for candidate job views
        if (searchPanel) searchPanel.classList.remove('hidden');

        var debounceTimer;
        function triggerFilter() {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(function () {
                window.dispatchEvent(new CustomEvent('jobs-filter-change'));
            }, 200);
        }

        searchInput.addEventListener('input', triggerFilter);
        if (sortSelect) sortSelect.addEventListener('change', triggerFilter);
        if (locationFilter) locationFilter.addEventListener('change', triggerFilter);
        if (salaryFilter) salaryFilter.addEventListener('change', triggerFilter);
        if (companyFilter) companyFilter.addEventListener('change', triggerFilter);
        if (skillsFilter) skillsFilter.addEventListener('input', triggerFilter);
        
        var typeFilter = document.getElementById('jobs-type-filter');
        var expFilter = document.getElementById('jobs-experience-filter');
        if (typeFilter) typeFilter.addEventListener('change', triggerFilter);
        if (expFilter) expFilter.addEventListener('change', triggerFilter);
    }

    function populateCompanyFilter(jobs) {
        if (companyFilterPopulated) return;
        var select = document.getElementById('jobs-company-filter');
        if (!select || !jobs || !jobs.length) return;

        var companies = {};
        jobs.forEach(function (job) {
            var name = (job.companyName || '').trim();
            if (name && !companies[name.toLowerCase()]) {
                companies[name.toLowerCase()] = name;
            }
        });

        var sorted = Object.values(companies).sort();
        sorted.forEach(function (name) {
            var option = document.createElement('option');
            option.value = name.toLowerCase();
            option.textContent = name;
            select.appendChild(option);
        });

        companyFilterPopulated = true;
    }

    function getFilterState() {
        return {
            query: (document.getElementById('jobs-search')?.value || '').toLowerCase().trim(),
            sort: document.getElementById('jobs-sort')?.value || 'newest',
            location: document.getElementById('jobs-location-filter')?.value || 'all',
            jobType: document.getElementById('jobs-type-filter')?.value || 'all',
            experienceLevel: document.getElementById('jobs-experience-filter')?.value || 'all',
            salary: document.getElementById('jobs-salary-filter')?.value || 'all',
            company: document.getElementById('jobs-company-filter')?.value || 'all',
            skills: (document.getElementById('jobs-skills-filter')?.value || '').toLowerCase().trim(),
        };
    }

    function parseSalary(job) {
        var raw = (job.salary || job.salaryRange || '').replace(/[^\d.]/g, ' ').trim();
        var nums = raw.split(/\s+/).map(Number).filter(function (n) { return !isNaN(n) && n > 0; });
        return nums.length ? Math.max.apply(null, nums) : 0;
    }

    function parseSalaryRange(rangeStr) {
        if (rangeStr === 'all') return null;
        if (rangeStr.endsWith('+')) {
            return { min: parseInt(rangeStr), max: Infinity };
        }
        var parts = rangeStr.split('-').map(Number);
        return { min: parts[0] || 0, max: parts[1] || Infinity };
    }

    function filterAndSortJobs(jobs) {
        var f = getFilterState();
        var filtered = jobs;

        // Populate company filter on first call
        populateCompanyFilter(jobs);

        // Text search
        if (f.query) {
            filtered = filtered.filter(function (job) {
                var title = (job.title || '').toLowerCase();
                var company = (job.companyName || '').toLowerCase();
                var skills = (job.skills || job.roleSkills || '').toLowerCase();
                var description = (job.description || '').toLowerCase();
                return title.includes(f.query) || company.includes(f.query) || skills.includes(f.query) || description.includes(f.query);
            });
        }

        // Location / WorkMode filter
        if (f.location !== 'all') {
            filtered = filtered.filter(function (job) {
                if (job.workMode) {
                    return job.workMode === f.location;
                }
                var loc = (job.location || '').toLowerCase();
                if (f.location === 'REMOTE') return loc.includes('remote');
                if (f.location === 'ONSITE' || f.location === 'HYBRID') return !loc.includes('remote');
                return true;
            });
        }
        
        // Job Type filter
        if (f.jobType !== 'all') {
            filtered = filtered.filter(function (job) {
                return job.jobType === f.jobType;
            });
        }

        // Experience Level filter
        if (f.experienceLevel !== 'all') {
            filtered = filtered.filter(function (job) {
                return job.experienceLevel === f.experienceLevel;
            });
        }

        // Salary range filter
        if (f.salary !== 'all') {
            var range = parseSalaryRange(f.salary);
            if (range) {
                filtered = filtered.filter(function (job) {
                    var salary = parseSalary(job);
                    if (salary === 0) return true; // Include jobs with no salary specified
                    return salary >= range.min && salary <= range.max;
                });
            }
        }

        // Company filter
        if (f.company !== 'all') {
            filtered = filtered.filter(function (job) {
                return (job.companyName || '').toLowerCase() === f.company;
            });
        }

        // Skills filter
        if (f.skills) {
            var skillKeywords = f.skills.split(/[,\s]+/).filter(Boolean);
            filtered = filtered.filter(function (job) {
                var jobSkills = (job.skills || job.roleSkills || '').toLowerCase();
                return skillKeywords.some(function (kw) { return jobSkills.includes(kw); });
            });
        }

        // Sort
        if (f.sort === 'salary-high') {
            filtered = filtered.slice().sort(function (a, b) {
                return parseSalary(b) - parseSalary(a);
            });
        } else if (f.sort === 'title-az') {
            filtered = filtered.slice().sort(function (a, b) {
                return (a.title || '').localeCompare(b.title || '');
            });
        } else if (f.sort === 'best-match') {
            // Sort by stored Application.matchScore (populated from DB, not dynamic AI calls)
            filtered = filtered.slice().sort(function (a, b) {
                var scoreA = (window._matchScoreCache && window._matchScoreCache[String(a.id)]) || 0;
                var scoreB = (window._matchScoreCache && window._matchScoreCache[String(b.id)]) || 0;
                if (scoreA !== scoreB) return scoreB - scoreA;
                return (a.title || '').localeCompare(b.title || '');
            });
        }
        // 'newest' is already default sort from fetchAllJobs

        return filtered;
    }

    window.hiringFlow = {
        showToast: showToast,
        openApplyModal: openApplyModal,
        initSearchFilter: initSearchFilter,
        filterAndSortJobs: filterAndSortJobs,
        getFilterState: getFilterState,
        populateCompanyFilter: populateCompanyFilter,
    };

    // ─── Status tabs for applications view ───
    function initStatusTabs() {
        var tabsPanel = document.getElementById('jobs-status-tabs-panel');
        if (!tabsPanel) return;
        window.addEventListener('apps-tabs-render', function (e) {
            var apps = e.detail?.applications || [];
            var activeTab = e.detail?.activeTab || 'all';
            var counts = { all: apps.length, applied: 0, shortlisted: 0, rejected: 0, scheduled: 0 };
            apps.forEach(function (app) {
                var state = window.jobsApp.getApplicationDisplayState(app);
                if (counts[state.key] !== undefined) counts[state.key]++;
            });
            tabsPanel.classList.remove('hidden');
            var tabs = document.getElementById('jobs-status-tabs');
            tabs.innerHTML = ['all', 'applied', 'shortlisted', 'scheduled', 'rejected'].map(function (key) {
                var labels = { all: 'All', applied: 'Under Review', shortlisted: 'Shortlisted', scheduled: 'Interview', rejected: 'Rejected' };
                var icons = { all: '📋', applied: '⏳', shortlisted: '✅', scheduled: '📅', rejected: '❌' };
                var active = activeTab === key ? ' active' : '';
                return '<button class="status-tab' + active + '" data-tab="' + key + '">' +
                    '<span style="margin-right:.2rem">' + icons[key] + '</span> ' +
                    labels[key] + '<span class="tab-count">' + (counts[key] || 0) + '</span></button>';
            }).join('');
        });
    }

    // ─── Schedule Interview Modal ───
    function openScheduleModal(applicant, onSchedule) {
        var root = document.getElementById('schedule-modal-root');
        if (!root) return;

        var name = applicant.candidateName || 'Candidate';
        var email = applicant.candidateEmail || '';
        var initial = name.charAt(0).toUpperCase();
        var existingDate = '';
        var existingTime = '';
        if (applicant.interviewScheduledAt) {
            var parts = window.jobsApp.toInputDateTimeParts(applicant.interviewScheduledAt);
            existingDate = parts.date;
            existingTime = parts.time;
        }

        var todayDateString = new Date().toLocaleDateString('en-CA'); // 'YYYY-MM-DD' in local time

        root.innerHTML =
            '<div class="modal-overlay" id="schedule-modal-overlay">' +
                '<div class="modal" style="max-width:480px">' +
                    '<div class="modal-header">' +
                        '<h2>Schedule Interview</h2>' +
                        '<button class="modal-close" data-close-schedule aria-label="Close">✕</button>' +
                    '</div>' +
                    '<div class="modal-body">' +
                        '<div class="schedule-candidate-info">' +
                            '<div class="schedule-candidate-avatar">' + escHtml(initial) + '</div>' +
                            '<div class="schedule-candidate-detail">' +
                                '<strong>' + escHtml(name) + '</strong>' +
                                '<span>' + escHtml(email) + ' · ' + escHtml(applicant.jobTitle || '') + '</span>' +
                            '</div>' +
                        '</div>' +
                        '<div class="schedule-form-grid">' +
                            '<div class="ln-field">' +
                                '<label for="schedule-date">Interview Date</label>' +
                                '<input type="date" id="schedule-date" value="' + escAttr(existingDate) + '" min="' + todayDateString + '" required>' +
                            '</div>' +
                            '<div class="ln-field">' +
                                '<label for="schedule-time">Interview Time</label>' +
                                '<input type="time" id="schedule-time" value="' + escAttr(existingTime) + '" required>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                    '<div class="modal-footer">' +
                        '<button class="btn btn-outline" data-close-schedule>Cancel</button>' +
                        '<button class="btn btn-primary" id="schedule-submit-btn">' +
                            (applicant.interviewScheduledAt ? 'Reschedule' : 'Schedule Interview') +
                        '</button>' +
                    '</div>' +
                '</div>' +
            '</div>';

        function closeScheduleModal() {
            var overlay = document.getElementById('schedule-modal-overlay');
            if (overlay) {
                overlay.classList.add('closing');
                setTimeout(function () { root.innerHTML = ''; }, 250);
            }
        }

        root.querySelectorAll('[data-close-schedule]').forEach(function (el) {
            el.addEventListener('click', closeScheduleModal);
        });
        root.querySelector('.modal-overlay').addEventListener('click', function (e) {
            if (e.target === e.currentTarget) closeScheduleModal();
        });

        document.getElementById('schedule-submit-btn').addEventListener('click', function () {
            var dateVal = document.getElementById('schedule-date').value;
            var timeVal = document.getElementById('schedule-time').value;
            if (!dateVal || !timeVal) {
                showToast('Please select both date and time.', 'error');
                return;
            }

            var now = new Date();
            var selectedDateTime = new Date(dateVal + 'T' + timeVal);
            
            // Check if selected date is in the past (timezone safe by comparing YYYY-MM-DD strings)
            if (dateVal < todayDateString) {
                showToast('Interview date cannot be in the past.', 'error');
                return;
            }

            // If selected date is today, check if time is in the past
            if (dateVal === todayDateString && selectedDateTime < now) {
                showToast('Interview time must be in the future.', 'error');
                return;
            }

            var btn = this;
            btn.disabled = true;
            btn.textContent = 'Scheduling...';

            var scheduledAt = dateVal + 'T' + timeVal + ':00';
            if (typeof onSchedule === 'function') {
                onSchedule(applicant, scheduledAt, closeScheduleModal, function () {
                    btn.disabled = false;
                    btn.textContent = applicant.interviewScheduledAt ? 'Reschedule' : 'Schedule Interview';
                });
            }
        });
    }
    window.openScheduleModal = openScheduleModal;

    // ─── Init on DOMContentLoaded ───
    document.addEventListener('DOMContentLoaded', function () {
        if (document.body.dataset.page === 'jobs') {
            initSearchFilter();
            initStatusTabs();

            // Status tabs click
            document.addEventListener('click', function (e) {
                var tab = e.target.closest('.status-tab[data-tab]');
                if (tab) {
                    window.dispatchEvent(new CustomEvent('apps-tab-change', { detail: { tab: tab.dataset.tab } }));
                }
            });
        }
    });

    // ─── Helpers ───
    function escHtml(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }
    function escAttr(s) { return escHtml(s); }
})();
