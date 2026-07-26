const pickMap = L.map('pick-map').setView([7.8731, 80.7718], 8);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors'
}).addTo(pickMap);

let selectedMarker = null;

pickMap.on('click', function (e) {
    document.getElementById('latitude').value = e.latlng.lat;
    document.getElementById('longitude').value = e.latlng.lng;

    if (selectedMarker) {
        selectedMarker.setLatLng(e.latlng);
    } else {
        selectedMarker = L.marker(e.latlng).addTo(pickMap);
    }
});

//photo uploading and url saving
document.getElementById('photoFile').addEventListener('change', async function () {
    const file = this.files[0];
    const preview = document.getElementById('photoPreview');
    const photoUrlField = document.getElementById('photoUrl');

    if (!file) {
        preview.style.display = 'none';
        photoUrlField.value = '';
        return;
    }

    if (file.size > 2 * 1024 * 1024) {
        alert('Please choose an image smaller than 2MB.');
        this.value = '';
        preview.style.display = 'none';
        photoUrlField.value = '';
        return;
    }

    const formData = new FormData();
    formData.append('photo', file);

    try {
        const response = await fetch('api/upload', { method: 'POST', body: formData });
        const result = await response.json();

        if (!response.ok) {
            alert(result.error || 'Photo upload failed.');
            this.value = '';
            preview.style.display = 'none';
            photoUrlField.value = '';
            return;
        }

        photoUrlField.value = result.photoUrl;
        preview.src = result.photoUrl;
        preview.style.display = 'block';
    } catch (err) {
        alert('Photo upload failed. Please try again.');
        this.value = '';
        preview.style.display = 'none';
        photoUrlField.value = '';
    }
});

// swappimng different "circumstances"/"condition" label depending on report type
document.getElementById('type').addEventListener('change', function () {
    const label = document.getElementById('extra-label');
    const extraField = document.getElementById('extra');

    if (this.value === 'LOST') {
        label.textContent = 'Circumstances';
        extraField.placeholder = 'How/when they went missing';
    } else {
        label.textContent = 'Condition at discovery';
        extraField.placeholder = 'e.g. injured, stable, unconscious';
    }
});

// disaster dropdown
async function loadDisasterDropdown() {
    const dropdown = document.getElementById('reportDisasterId');
    const response = await fetch('api/disasters');
    if (response.ok) {
        const disasters = await response.json();
        dropdown.innerHTML = '<option value="">Other (No specific disaster)</option>';
        disasters.forEach(d => {
            const option = document.createElement('option');
            option.value = d.disasterId;
            option.textContent = `${d.type} - ${d.location.address || 'Unknown Area'}`;
            dropdown.appendChild(option);
        });
    }
}
loadDisasterDropdown();

async function submitReport() {
    const nameError = document.getElementById('nameError');
    const ageError = document.getElementById('ageError');
    const genderError = document.getElementById('genderError');
    const nicError = document.getElementById('nicError');
    const locationError = document.getElementById('locationError');
    [nameError, ageError, genderError, nicError, locationError].forEach(el => el.style.display = 'none');

    const name = document.getElementById('name').value.trim();
    const age = parseFloat(document.getElementById('age').value);
    const gender = document.getElementById('gender').value;
    const nicNumber = document.getElementById('nicNumber').value.trim();
    const latitude = parseFloat(document.getElementById('latitude').value);
    const longitude = parseFloat(document.getElementById('longitude').value);
    const NIC_PATTERN = /^(\d{12}|\d{9}[Vv])$/;
    const NAME_PATTERN = /^[A-Za-z\s]+$/;

    if (!name) {
        nameError.textContent = 'Full name is required.';
        nameError.style.display = 'block';
        alert('Submission failed: full name is required.');
        return;
    }

    if (!NAME_PATTERN.test(name)) {
        nameError.textContent = 'Full name must contain letters only, no numbers or symbols.';
        nameError.style.display = 'block';
        alert('Submission failed: full name must contain letters only, no numbers or symbols.');
        return;
    }

    if (isNaN(age) || age <= 0 || age >= 100) {
        ageError.style.display = 'block';
        alert('Submission failed: please add a valid age.');
        return;
    }

    if (!gender) {
        genderError.style.display = 'block';
        alert('Submission failed: gender is required.');
        return;
    }

    if (nicNumber && !NIC_PATTERN.test(nicNumber)) {
        nicError.style.display = 'block';
        alert('Submission failed: NIC must be exactly 12 digits, or 9 digits followed by \'V\'.');
        return;
    }

    if (isNaN(latitude) || isNaN(longitude)) {
        locationError.style.display = 'block';
        alert('Submission failed: please click the map to set a location.');
        return;
    }

    const type = document.getElementById('type').value;
    const selectedDisaster = document.getElementById('reportDisasterId').value;
    const payload = {
        type: type,
        name: name,
        age: age,
        gender: gender,
        description: document.getElementById('description').value,
        nicNumber: nicNumber || null,
        latitude: latitude,
        longitude: longitude,
        address: document.getElementById('address').value,
        photoUrl: document.getElementById('photoUrl').value || null,
        disasterId: selectedDisaster ? parseInt(selectedDisaster) : null
    };

    if (type === 'LOST') {
        payload.circumstances = document.getElementById('extra').value;
    } else {
        payload.condition = document.getElementById('extra').value;
    }

    try {
        const response = await fetch('api/persons', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            alert('Report submitted successfully.');
            window.location.href = 'index.html';
        } else {
            alert('Submission failed. Please try again.');
        }
    } catch (err) {
        alert('Submission failed. Please try again.');
    }
}