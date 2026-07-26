async function adminLogin() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('error-message');

    const response = await fetch('api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });

    const result = await response.json();

    if (!result.success) {
        errorMessage.textContent = result.message || 'Login failed. Please try again.';
        errorMessage.style.display = 'block';
        return;
    }

    // admin only validation
    if (result.role !== 'ADMIN') {
        errorMessage.textContent = 'This login is for administrators only. Please use the regular sign-in page.';
        errorMessage.style.display = 'block';
     
        fetch('api/logout', { method: 'POST' });
        return;
    }

    sessionStorage.setItem('userFullName', result.fullName);
    sessionStorage.setItem('userRole', result.role);
    sessionStorage.setItem('userPermissions', JSON.stringify(result.permissions));

    window.location.href = 'admin-dashboard.html';
}
