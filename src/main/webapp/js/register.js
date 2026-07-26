const roleSelect = document.getElementById('role');
const adminNote = document.getElementById('admin-note');

function applyRoleView() {
    document.querySelectorAll('.role-fields').forEach(el => el.classList.remove('active'));
    document.getElementById('fields-' + roleSelect.value).classList.add('active');
    adminNote.style.display = (roleSelect.value === 'ADMIN') ? 'block' : 'none';
}

roleSelect.addEventListener('change', applyRoleView);


(function preselectRoleFromQueryString() {
    const params = new URLSearchParams(window.location.search);
    const requestedRole = params.get('role');
    if (requestedRole && document.getElementById('fields-' + requestedRole)) {
        roleSelect.value = requestedRole;
    }
    applyRoleView();
})();

async function register() {
    const errorMessage = document.getElementById('error-message');
    const successMessage = document.getElementById('success-message');
    errorMessage.style.display = 'none';
    successMessage.style.display = 'none';

    const contactNumber = document.getElementById('contactNumber').value.trim();
    if (!contactNumber) {
        errorMessage.textContent = 'Contact number is required.';
        errorMessage.style.display = 'block';
        return;
    }

    const role = roleSelect.value;

    const payload = {
        role: role,
        fullName: document.getElementById('fullName').value,
        username: document.getElementById('username').value,
        password: document.getElementById('password').value,
        contactNumber: contactNumber
    };

    //feilds only access
    if (role === 'REPORTER') {
        payload.relationshipToMissingPerson = document.getElementById('relationshipToMissingPerson').value;
    } else if (role === 'RESCUE_TEAM') {
        payload.unitName = document.getElementById('unitName').value;
        payload.assignedZone = document.getElementById('assignedZone').value;
    } else if (role === 'POLICE_STATION') {
        payload.stationName = document.getElementById('stationName').value;
        payload.stationCode = document.getElementById('stationCode').value;
        payload.jurisdictionArea = document.getElementById('jurisdictionArea').value;
    } else if (role === 'ADMIN') {
        payload.department = document.getElementById('department').value;
    }

    let response, result;
    try {
        response = await fetch('api/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        result = await response.json();
    } catch (err) {
        errorMessage.textContent = 'Registration failed due to a server error. Please try again, and check with an admin if it keeps happening.';
        errorMessage.style.display = 'block';
        return;
    }

    if (!result.success) {
        errorMessage.textContent = result.message || 'Registration failed. Please try again.';
        errorMessage.style.display = 'block';
        return;
    }

    successMessage.textContent = result.message;
    successMessage.style.display = 'block';

 
    setTimeout(function () {
        window.location.href = result.pendingApproval ? 'admin-login.html' : 'login.html';
    }, 2500);
}