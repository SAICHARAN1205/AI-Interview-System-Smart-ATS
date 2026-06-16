document.addEventListener('DOMContentLoaded', () => {
    // 1. Enforce Admin Role Access
    if (!window.api.isAuthenticated() || window.api.getRole().toUpperCase() !== 'ADMIN') {
        window.location.href = 'access-denied.html';
        return;
    }

    // 2. Sidebar Navigation Logic
    const navItems = document.querySelectorAll('.admin-nav-item');
    const sections = document.querySelectorAll('.admin-section');

    navItems.forEach(item => {
        item.addEventListener('click', () => {
            // Update active nav
            navItems.forEach(n => n.classList.remove('active'));
            item.classList.add('active');

            // Show target section
            const targetId = item.getAttribute('data-target');
            sections.forEach(sec => sec.classList.remove('active'));
            document.getElementById(targetId).classList.add('active');
            
            // Load specific section data
            if (targetId === 'users') loadUsers();
            else if (targetId === 'recruiters') loadRecruiters();
            else if (targetId === 'ai-configs') loadAiConfigs();
            else if (targetId === 'audit-logs') {
                loadAuditSummary();
                loadAuditLogs(0);
            }
        });
    });

    // Handle URL hash navigation (e.g. admin.html#users)
    if (window.location.hash) {
        const targetId = window.location.hash.substring(1);
        const navItem = document.querySelector(`.admin-nav-item[data-target="${targetId}"]`);
        if (navItem) navItem.click();
    } else {
        loadDashboardStats();
    }
});

// Admin API Fetch Wrapper
async function fetchAdminData(endpoint, method = 'GET', body = null) {
    try {
        if (method === 'GET') {
            return await window.api.get(`/api/admin/${endpoint}`);
        } else if (method === 'PUT') {
            return await window.api.put(`/api/admin/${endpoint}`, body);
        } else if (method === 'POST') {
            return await window.api.post(`/api/admin/${endpoint}`, body);
        } else if (method === 'DELETE') {
            return await window.api.del(`/api/admin/${endpoint}`);
        }
        return null;
    } catch (err) {
        if (err.status === 403 || err.status === 401) {
            window.location.href = 'access-denied.html';
        }
        throw err;
    }
}

async function loadDashboardStats() {
    // Show loading text in stats temporarily
    const statIds = ['stat-users', 'stat-recruiters', 'stat-ai'];
    statIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.textContent = '...';
    });

    try {
        const data = await fetchAdminData('dashboard/overview');

        if (data) {
            document.getElementById('stat-users').textContent = data.totalUsers ?? data.data?.totalUsers ?? '--';
            document.getElementById('stat-recruiters').textContent = data.activeRecruiters ?? data.data?.activeRecruiters ?? '--';
            document.getElementById('stat-ai').textContent = data.aiProvidersActive ?? data.data?.aiProvidersActive ?? '--';
            
            const jobsEl = document.getElementById('stat-jobs');
            const appsEl = document.getElementById('stat-apps');
            if (jobsEl) jobsEl.textContent = data.totalJobs ?? data.data?.totalJobs ?? '--';
            if (appsEl) appsEl.textContent = data.totalApplications ?? data.data?.totalApplications ?? '--';
        } else {
            if (window.showToast) showToast('Failed to load dashboard data', 'error');
            statIds.forEach(id => {
                const el = document.getElementById(id);
                if (el) el.textContent = '--';
            });
        }
    } catch (err) {
        if (window.showToast) showToast('Failed to load dashboard data', 'error');
    }
}

async function loadUsers() {
    const tbody = document.getElementById('users-tbody');
    tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">Loading...</td></tr>';
    
    const res = await fetchAdminData('users');
    if (!res) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:red;">Failed to load users</td></tr>';
        return;
    }

    tbody.innerHTML = '';
    res.forEach(user => {
        const tr = document.createElement('tr');
        
        const statusClass = user.accountStatus === 'ACTIVE' ? 'status-active' : 
                            (user.accountStatus === 'SUSPENDED' ? 'status-suspended' : 'status-pending');
        
        tr.innerHTML = `
            <td>${user.name || '-'}</td>
            <td>${user.email}</td>
            <td><strong>${user.role}</strong></td>
            <td><span class="status-badge ${statusClass}">${user.accountStatus || 'PENDING'}</span></td>
            <td>
                <select onchange="updateUserStatus(${user.id}, this.value)" style="padding:4px;">
                    <option value="" disabled selected>Change Status</option>
                    <option value="ACTIVE">Activate</option>
                    <option value="SUSPENDED">Suspend</option>
                    <option value="BANNED">Ban</option>
                </select>
            </td>
            <td>
                <button onclick="deleteUser(${user.id})" class="action-btn danger" title="Delete User"><i class="fas fa-trash"></i></button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

window.updateUserStatus = async function(userId, status) {
    const confirmed = window.smartUi
        ? await window.smartUi.confirm({
            title: 'Change User Status',
            description: `Are you sure you want to change this user's status to ${status}?`,
            confirmLabel: 'Change Status',
            confirmClass: status === 'BANNED' || status === 'SUSPENDED' ? 'btn-danger' : 'btn-primary',
            cancelLabel: 'Cancel'
          })
        : confirm(`Are you sure you want to change user status to ${status}?`);
    
    if (!confirmed) return;
    
    try {
        await fetchAdminData(`users/${userId}/status`, 'PUT', { status });
        loadUsers();
    } catch (err) {
        if (window.showToast) showToast(err.message || 'Failed to update status', 'error');
    }
};

window.deleteUser = async function(userId) {
    const confirmed = window.smartUi
        ? await window.smartUi.confirm({
            title: 'Delete User',
            description: `Are you sure you want to completely delete this user? This action cannot be undone.`,
            confirmLabel: 'Delete',
            confirmClass: 'btn-danger',
            cancelLabel: 'Cancel'
          })
        : confirm(`Are you sure you want to completely delete this user?`);
    
    if (!confirmed) return;
    
    try {
        await fetchAdminData(`users/${userId}`, 'DELETE');
        loadUsers();
    } catch (err) {
        if (window.showToast) showToast(err.message || 'Failed to delete user', 'error');
    }
};

async function loadRecruiters() {
    const tbody = document.getElementById('recruiters-tbody');
    tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">Loading...</td></tr>';
    
    const res = await fetchAdminData('recruiters');
    if (!res) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:red;">Failed to load profiles</td></tr>';
        return;
    }

    tbody.innerHTML = '';
    if(res.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">No recruiter profiles found.</td></tr>';
        return;
    }
    
    res.forEach(profile => {
        const tr = document.createElement('tr');
        const statusClass = profile.verificationStatus === 'APPROVED' ? 'status-active' : 
                           (profile.verificationStatus === 'REJECTED' ? 'status-suspended' : 'status-pending');
        
        tr.innerHTML = `
            <td><strong>${profile.companyName || '-'}</strong></td>
            <td>${profile.user ? profile.user.email : 'Unknown'}</td>
            <td><a href="${profile.companyWebsite}" target="_blank">Link</a></td>
            <td><span class="status-badge ${statusClass}">${profile.verificationStatus}</span></td>
            <td>
                <button onclick="verifyRecruiter(${profile.id}, 'APPROVED')" class="action-btn" title="Approve"><i class="fas fa-check"></i></button>
                <button onclick="verifyRecruiter(${profile.id}, 'REJECTED')" class="action-btn danger" title="Reject"><i class="fas fa-times"></i></button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

window.verifyRecruiter = async function(profileId, status) {
    const confirmed = window.smartUi
        ? await window.smartUi.confirm({
            title: 'Update Recruiter Status',
            description: `Mark this recruiter profile as ${status}?`,
            confirmLabel: status === 'REJECTED' ? 'Reject' : 'Approve',
            confirmClass: status === 'REJECTED' ? 'btn-danger' : 'btn-primary',
            cancelLabel: 'Cancel'
          })
        : confirm(`Mark this recruiter profile as ${status}?`);
    
    if (!confirmed) return;
    
    try {
        await fetchAdminData(`recruiters/${profileId}/verify`, 'PUT', { status });
        loadRecruiters();
    } catch (err) {
        if (window.showToast) showToast(err.message || 'Failed to update recruiter status', 'error');
    }
};

async function loadAiConfigs() {
    const tbody = document.getElementById('ai-tbody');
    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">Loading...</td></tr>';
    
    const res = await fetchAdminData('ai/configs');
    if (!res) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:red;">Failed to load configs</td></tr>';
        return;
    }

    tbody.innerHTML = '';
    res.forEach(config => {
        const tr = document.createElement('tr');
        const statusClass = config.enabled ? 'status-active' : 'status-suspended';
        
        tr.innerHTML = `
            <td><input type="number" value="${config.priorityOrder}" style="width: 60px" onchange="updateAiConfig(${config.id}, ${config.enabled}, this.value)"></td>
            <td><strong>${config.providerName}</strong></td>
            <td><span class="status-badge ${statusClass}">${config.enabled ? 'ENABLED' : 'DISABLED'}</span></td>
            <td>
                <button onclick="updateAiConfig(${config.id}, ${!config.enabled}, ${config.priorityOrder})" class="action-btn ${config.enabled ? 'danger' : ''}">
                    ${config.enabled ? 'Disable' : 'Enable'}
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

window.updateAiConfig = async function(configId, isEnabled, priorityOrder) {
    await fetchAdminData(`ai/configs/${configId}`, 'PUT', { 
        enabled: isEnabled, 
        priorityOrder: parseInt(priorityOrder) 
    });
    loadAiConfigs(); // Refresh
};

// ─────────────────────────────────────────────────────────────────
// AUDIT LOGS
// ─────────────────────────────────────────────────────────────────

let auditCurrentPage = 0;
const AUDIT_PAGE_SIZE = 25;

function getAuditFilters() {
    return {
        search:     (document.getElementById('audit-search')?.value || '').trim(),
        role:       document.getElementById('audit-role')?.value || '',
        actionType: document.getElementById('audit-action')?.value || '',
        status:     document.getElementById('audit-status')?.value || '',
        from:       document.getElementById('audit-from')?.value || '',
        to:         document.getElementById('audit-to')?.value || '',
    };
}

async function loadAuditSummary() {
    try {
        const d = await window.api.get('/api/admin/logs/summary');
        const set = (id, v) => { const el = document.getElementById(id); if (el) el.textContent = (v ?? '--'); };
        set('kpi-total',        d.totalLogs);
        set('kpi-logins-24h',   d.loginSuccessLast24h);
        set('kpi-failures-24h', d.loginFailureLast24h);
        set('kpi-security-7d',  d.securityEventsLast7d);
    } catch (e) {
        // Silently fail — KPI cards will retain default values
    }
}

async function loadAuditLogs(page = 0) {
    auditCurrentPage = page;
    const tbody = document.getElementById('audit-tbody');
    if (tbody) tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">Loading...</td></tr>';

    const f = getAuditFilters();
    const params = new URLSearchParams({ page, size: AUDIT_PAGE_SIZE });
    if (f.search)     params.append('search', f.search);
    if (f.role)       params.append('role', f.role);
    if (f.actionType) params.append('actionType', f.actionType);
    if (f.status)     params.append('status', f.status);
    if (f.from)       params.append('from', f.from);
    if (f.to)         params.append('to', f.to);

    try {
        const pageData = await window.api.get(`/api/admin/logs?${params}`);

        const content       = pageData.content || [];
        const totalPages    = pageData.totalPages ?? 1;
        const totalElements = pageData.totalElements ?? content.length;
        const isLast        = pageData.last ?? (page >= totalPages - 1);
        const isFirst       = pageData.first ?? (page === 0);

        const prevBtn  = document.getElementById('btn-audit-prev');
        const nextBtn  = document.getElementById('btn-audit-next');
        const pageInfo = document.getElementById('audit-page-info');
        if (prevBtn)  prevBtn.disabled  = isFirst;
        if (nextBtn)  nextBtn.disabled  = isLast;
        if (pageInfo) pageInfo.textContent = `Page ${page + 1} of ${totalPages} (${totalElements} total)`;

        if (!tbody) return;

        if (content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color: var(--text-secondary);">No activity logs found for the selected filters.</td></tr>';
            return;
        }

        tbody.innerHTML = '';
        content.forEach(log => {
            const tr = document.createElement('tr');

            const status = (log.status || '').toUpperCase();
            let statusClass = 'status-unknown';
            if (status === 'SUCCESS')             statusClass = 'status-success';
            else if (status === 'FAILURE')        statusClass = 'status-failure';
            else if (status === 'SECURITY_EVENT') statusClass = 'status-security';

            const ts = log.createdAt
                ? new Date(log.createdAt).toLocaleString('en-GB', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit', day: '2-digit', month: 'short', year: 'numeric' })
                : '--';

            const email = log.userEmail ? `<span title="${log.userEmail}">${log.userEmail}</span>` : '<span style="color:var(--text-secondary)">—</span>';
            const ip    = log.ipAddress  || '<span style="color:var(--text-secondary)">—</span>';
            const desc  = log.actionDescription
                ? `<span title="${log.actionDescription}">${log.actionDescription.length > 70 ? log.actionDescription.substring(0, 67) + '...' : log.actionDescription}</span>`
                : '<span style="color:var(--text-secondary)">—</span>';

            tr.innerHTML = `
                <td style="font-size:0.82rem; color:var(--text-secondary); white-space:nowrap">${ts}</td>
                <td style="font-size:0.85rem">${email}</td>
                <td><span class="status-badge status-pending" style="font-size:0.75rem">${log.userRole || '—'}</span></td>
                <td><span class="log-action-badge">${log.actionType || '—'}</span></td>
                <td><span class="status-badge ${statusClass}">${log.status || '—'}</span></td>
                <td style="font-size:0.82rem; color:var(--text-secondary)">${ip}</td>
                <td style="font-size:0.82rem">${desc}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        if (tbody) tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color:red;">Failed to load audit logs.</td></tr>';
    }
}

// Wire up audit log buttons once DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('btn-audit-search')?.addEventListener('click', () => loadAuditLogs(0));

    document.getElementById('btn-audit-reset')?.addEventListener('click', () => {
        ['audit-search','audit-role','audit-action','audit-status','audit-from','audit-to']
            .forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });
        loadAuditLogs(0);
    });

    document.getElementById('btn-audit-prev')?.addEventListener('click', () => {
        if (auditCurrentPage > 0) loadAuditLogs(auditCurrentPage - 1);
    });

    document.getElementById('btn-audit-next')?.addEventListener('click', () => {
        loadAuditLogs(auditCurrentPage + 1);
    });

    // Allow pressing Enter in search box
    document.getElementById('audit-search')?.addEventListener('keydown', e => {
        if (e.key === 'Enter') loadAuditLogs(0);
    });
});

