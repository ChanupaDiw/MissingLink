
function requireLogin() {
    const role = sessionStorage.getItem('userRole');
    if (!role) {
        window.location.href = 'login.html';
    }
    return role;
}

function getPermissions() {
    const raw = sessionStorage.getItem('userPermissions');
    return raw ? JSON.parse(raw) : [];
}

function hasPermission(permission) {
    return getPermissions().includes(permission);
}

function logout() {
    fetch('api/logout', { method: 'POST' }).finally(function () {
        sessionStorage.clear();
        window.location.href = 'login.html';
    });
}


function renderUserBar() {
    const fullName = sessionStorage.getItem('userFullName');
    const role = sessionStorage.getItem('userRole');

    const bar = document.createElement('div');
    bar.className = 'user-pill';
    bar.innerHTML = `<span>${fullName} <span style="color:var(--text-muted); font-weight:400;">(${role})</span></span>` +
        `<a href="#" class="logout-btn" onclick="logout(); return false;">Log out</a>`;

    const slot = document.getElementById('user-bar-container');
    if (slot) {
        slot.appendChild(bar);
    } else {
        bar.style.position = 'fixed';
        bar.style.top = '10px';
        bar.style.right = '10px';
        bar.style.zIndex = '1000';
        document.body.appendChild(bar);
    }
}