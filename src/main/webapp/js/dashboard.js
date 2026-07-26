
function formatDate(dateObj) {
    if (!dateObj) return 'N/A';
    if (typeof dateObj === 'string') return dateObj;
    if (Array.isArray(dateObj)) {
        return `${dateObj[0]}-${String(dateObj[1]).padStart(2, '0')}-${String(dateObj[2]).padStart(2, '0')}`;
    }
    if (typeof dateObj === 'object') {
        const year = dateObj.year;
        const month = dateObj.month ?? dateObj.monthValue;
        const day = dateObj.day ?? dateObj.dayOfMonth;
        if (year !== undefined && month !== undefined && day !== undefined) {
            return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        }
        return JSON.stringify(dateObj);
    }
    return String(dateObj);
}

// map left half
const map = L.map('map').setView([7.8731, 80.7718], 8);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors'
}).addTo(map);

const lostLayer = L.layerGroup().addTo(map);
const foundLayer = L.layerGroup().addTo(map);
const disasterZoneLayer = L.layerGroup().addTo(map);

L.control.layers(null, {
    "Lost persons": lostLayer,
    "Found persons": foundLayer,
    "Disaster zones": disasterZoneLayer
}).addTo(map);

const lostColor = '#e74c3c';
const foundColor = '#2ecc71';

function createPersonIcon(photoUrl, borderColor) {
    const innerHtml = photoUrl
        ? `<div class="person-marker" style="background-image:url('${photoUrl}'); border-color:${borderColor};"></div>`
        : `<div class="person-marker" style="border-color:${borderColor}; background-color:${borderColor};">
             <svg viewBox="0 0 24 24" width="18" height="18" fill="#fff"><path d="M12 12c2.7 0 5-2.3 5-5s-2.3-5-5-5-5 2.3-5 5 2.3 5 5 5zm0 2c-3.3 0-10 1.7-10 5v3h20v-3c0-3.3-6.7-5-10-5z"/></svg>
           </div>`;

    return L.divIcon({
        className: 'person-div-icon',
        html: innerHtml,
        iconSize: [36, 36],
        iconAnchor: [18, 18],
        popupAnchor: [0, -18]
    });
}

function buildPopupHtml(person) {
    const photoHtml = person.photoUrl ? `<img src="${person.photoUrl}" class="popup-photo" />` : '';
    const nicLine = person.nicNumber ? `<br>NIC: ${person.nicNumber}` : '';
    return `<div class="popup-content">${photoHtml}<b>${person.fullName}</b><br>Age: ${person.age}, ${person.gender}${nicLine}<br>Status: ${person.status}</div>`;
}

async function loadPersons() {
    const response = await fetch('api/persons');
    const persons = await response.json();
    lostLayer.clearLayers();
    foundLayer.clearLayers();

    persons.forEach(person => {
        const isFound = person.status === 'FOUND';
        const location = isFound ? person.foundLocation : person.lastKnownLocation;
        if (!location) return;

        const borderColor = isFound ? foundColor : lostColor;
        const icon = createPersonIcon(person.photoUrl, borderColor);

        L.marker([location.latitude, location.longitude], { icon })
            .bindPopup(buildPopupHtml(person))
            .addTo(isFound ? foundLayer : lostLayer);
    });
}

async function loadDisasterZones() {
    const response = await fetch('api/disasters');
    const disasters = await response.json();
    disasterZoneLayer.clearLayers();

    disasters.forEach(disaster => {
        L.circle([disaster.location.latitude, disaster.location.longitude], {
            radius: disaster.radiusKm * 1000, color: 'red', fillColor: '#f03', fillOpacity: 0.15
        }).bindPopup(`<b>${disaster.type}</b><br>${disaster.description}`).addTo(disasterZoneLayer);
    });
}

loadDisasterZones();
loadPersons();

//mini map

const pickMap = L.map('pick-map').setView([7.8731, 80.7718], 8);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '&copy; OpenStreetMap contributors' }).addTo(pickMap);

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

// photo url uploading
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

//submit report and drop down

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
    // Reset all validation messages first
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
            resetReportForm();
            loadPersons();
            loadMySubmissions();
        } else {
            alert('Submission failed. Please try again.');
        }
    } catch (err) {
        alert('Submission failed. Please try again.');
    }
}

function resetReportForm() {
    document.getElementById('type').value = 'LOST';
    document.getElementById('reportDisasterId').value = '';
    document.getElementById('name').value = '';
    document.getElementById('age').value = '';
    document.getElementById('gender').value = '';
    document.getElementById('description').value = '';
    document.getElementById('nicNumber').value = '';
    document.getElementById('nicError').style.display = 'none';
    document.getElementById('nameError').style.display = 'none';
    document.getElementById('ageError').style.display = 'none';
    document.getElementById('genderError').style.display = 'none';
    document.getElementById('locationError').style.display = 'none';
    document.getElementById('extra').value = '';
    document.getElementById('photoFile').value = '';
    document.getElementById('photoUrl').value = '';
    document.getElementById('photoPreview').style.display = 'none';
    document.getElementById('latitude').value = '';
    document.getElementById('longitude').value = '';
    document.getElementById('address').value = '';
    if (selectedMarker) {
        pickMap.removeLayer(selectedMarker);
        selectedMarker = null;
    }
}

// my submission list

async function deletePerson(personId) {
    if (!confirm('Delete this submission permanently? This cannot be undone.')) return;

    try {
        const response = await fetch(`api/persons?personId=${personId}`, { method: 'DELETE' });
        const result = await response.json();

        if (!response.ok || result.success === false) {
            alert(result.message || 'Failed to delete submission.');
            return;
        }

        loadMySubmissions();
        loadPersons();
    } catch (err) {
        alert('Failed to delete submission. Please try again.');
    }
}

async function loadMySubmissions() {
    const listContainer = document.getElementById('my-submissions-list');
    const response = await fetch('api/persons?mine=true');

    if (!response.ok) {
        listContainer.innerHTML = '<p class="empty-note">Could not load your submissions.</p>';
        return;
    }

    const persons = await response.json();
    window.myPersonsData = persons; // Store for editing

    if (persons.length === 0) {
        listContainer.innerHTML = '<p class="empty-note">You haven\'t submitted any reports yet.</p>';
        return;
    }

    listContainer.innerHTML = persons.map(person => {
        const photoHtml = person.photoUrl
            ? `<img src="${person.photoUrl}" class="submission-thumb" alt="${person.fullName}" />`
            : `<div class="submission-thumb submission-thumb--placeholder"></div>`;

        return `
            <div class="submission-card">
                <div style="display:flex; align-items:center; gap:10px;">
                    ${photoHtml}
                    <div style="flex:1;">
                        <div class="name">${person.fullName}</div>
                        <span class="status-badge status-${person.status}">${person.status}</span>
                    </div>
                </div>
                <div style="margin-top:6px; color: var(--text-muted); font-size:12.5px;">
                    Reported: ${formatDate(person.dateReported)}<br>
                    Last updated: ${formatDate(person.dateLastUpdated)}<br>
                    NIC: ${person.nicNumber || 'N/A'}
                </div>
                <div class="submission-actions">
                    <button class="btn-sm btn-sm--edit" onclick="openEditPerson(${person.personId})">Edit Report</button>
                    <button class="btn-sm btn-sm--delete" onclick="deletePerson(${person.personId})">Delete</button>
                </div>
            </div>`;
    }).join('');
}

async function openEditPerson(personId) {

    const response = await fetch('api/persons?mine=true');
    if (!response.ok) {
        alert('Could not load this submission. Please try again.');
        return;
    }
    const persons = await response.json();
    window.myPersonsData = persons;

    const person = persons.find(p => p.personId == personId);
    if (!person) {
        alert('Could not find that submission. Please refresh and try again.');
        return;
    }

    document.getElementById('edit-person-form').style.display = 'block';

    document.getElementById('editPersonId').value = person.personId;
    document.getElementById('editPersonType').value = person.personType;
    document.getElementById('editPersonName').value = person.fullName;
    document.getElementById('editPersonAge').value = person.age;
    document.getElementById('editPersonGender').value = person.gender;
    document.getElementById('editPersonDesc').value = person.physicalDescription || '';

    const loc = person.location || person.lastKnownLocation || person.foundLocation;
    if (loc) {
        document.getElementById('editPersonLat').value = loc.latitude;
        document.getElementById('editPersonLng').value = loc.longitude;
        document.getElementById('editPersonAddress').value = loc.address || '';
    }

    const extraLabel = document.getElementById('editPersonExtraLabel');
    const extraInput = document.getElementById('editPersonExtraInfo');

    if (person.personType === 'LOST') {
        extraLabel.textContent = 'Circumstances';
        extraInput.value = person.circumstances || '';
    } else {
        extraLabel.textContent = 'Condition at discovery';
        extraInput.value = person.conditionAtDiscovery || '';
    }

    document.getElementById('edit-person-form').scrollIntoView({ behavior: 'smooth' });
}

function cancelEditPerson() {
    document.getElementById('edit-person-form').style.display = 'none';
}

async function submitEditPerson() {
    const payload = {
        personId: parseInt(document.getElementById('editPersonId').value),
        type: document.getElementById('editPersonType').value,
        name: document.getElementById('editPersonName').value,
        age: parseInt(document.getElementById('editPersonAge').value),
        gender: document.getElementById('editPersonGender').value,
        description: document.getElementById('editPersonDesc').value,
        latitude: parseFloat(document.getElementById('editPersonLat').value),
        longitude: parseFloat(document.getElementById('editPersonLng').value),
        address: document.getElementById('editPersonAddress').value,
        extraInfo: document.getElementById('editPersonExtraInfo').value
    };

    const response = await fetch('api/persons', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    if (response.ok) {
        alert('Submission updated successfully!');
        cancelEditPerson();
        loadMySubmissions();
        loadPersons();
    } else {
        alert('Failed to update submission.');
    }
}

loadMySubmissions();