
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

//main map
const map = L.map('map').setView([7.8731, 80.7718], 8);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '&copy; OpenStreetMap contributors' }).addTo(map);

const lostLayer = L.layerGroup().addTo(map);
const foundLayer = L.layerGroup().addTo(map);
const disasterZoneLayer = L.layerGroup().addTo(map);

L.control.layers(null, { "Lost persons": lostLayer, "Found persons": foundLayer, "Disaster zones": disasterZoneLayer }).addTo(map);

function buildPopupHtml(person) {
    const photoHtml = person.photoUrl ? `<img src="${person.photoUrl}" class="popup-photo" style="width:50px;"/>` : '';
    const nicLine = person.nicNumber ? `<br>NIC: ${person.nicNumber}` : '';
    return `<div class="popup-content">${photoHtml}<br><b>${person.fullName}</b><br>Age: ${person.age}, ${person.gender}${nicLine}<br>Status: ${person.status}</div>`;
}

function createPersonIcon(person) {
    const isFound = person.status === 'FOUND';
    const color = isFound ? '#2ecc71' : '#e74c3c';

    const innerHtml = person.photoUrl
        ? `<div class="person-marker" style="background-image:url('${person.photoUrl}'); border-color:${color};"></div>`
        : `<div class="person-marker person-marker--placeholder" style="border-color:${color}; background-color:${color};">
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

async function loadPersons() {
    const [personsResponse, historyResponse] = await Promise.all([
        fetch('api/persons'),
        fetch('api/matches?history=true')
    ]);
    const persons = await personsResponse.json();
    const history = await historyResponse.json();


    const resolvedPersonIds = new Set();
    history
        .filter(match => match.status === 'CONFIRMED')
        .forEach(match => {
            resolvedPersonIds.add(match.missingPerson.personId);
            resolvedPersonIds.add(match.foundPerson.personId);
        });

    lostLayer.clearLayers();
    foundLayer.clearLayers();

    persons.forEach(person => {
        if (resolvedPersonIds.has(person.personId)) return;

        const isFound = person.status === 'FOUND';
        const location = isFound ? person.foundLocation : person.lastKnownLocation;
        if (!location) return;

        const marker = L.marker([location.latitude, location.longitude], { icon: createPersonIcon(person) })
            .bindPopup(buildPopupHtml(person));
        marker.addTo(isFound ? foundLayer : lostLayer);
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

// mmini map
const pickMap = L.map('pick-map').setView([7.8731, 80.7718], 8);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(pickMap);
let selectedMarker = null;

pickMap.on('click', function (e) {
    document.getElementById('latitude').value = e.latlng.lat;
    document.getElementById('longitude').value = e.latlng.lng;
    if (selectedMarker) selectedMarker.setLatLng(e.latlng);
    else selectedMarker = L.marker(e.latlng).addTo(pickMap);
});

const disasterPickMap = L.map('disaster-pick-map').setView([7.8731, 80.7718], 8);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(disasterPickMap);
let disasterSelectedMarker = null;

disasterPickMap.on('click', function (e) {
    document.getElementById('disasterLat').value = e.latlng.lat;
    document.getElementById('disasterLng').value = e.latlng.lng;
    if (disasterSelectedMarker) disasterSelectedMarker.setLatLng(e.latlng);
    else disasterSelectedMarker = L.marker(e.latlng).addTo(disasterPickMap);
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

// upload photo and url
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

    if (type === 'LOST') payload.circumstances = document.getElementById('extra').value;
    else payload.condition = document.getElementById('extra').value;

    try {
        const response = await fetch('api/persons', {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
        });

        if (response.ok) {
            alert('Report submitted successfully.');
            resetReportForm();
            loadPersons();
            loadAllSubmissions();
            loadManageDisasters();
        } else alert('Submission failed. Please try again.');
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
    document.getElementById('ageError').style.display = 'none';
    document.getElementById('nameError').style.display = 'none';
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

// all subs
async function deletePerson(personId) {
    if (!confirm('Delete this submission permanently? This cannot be undone.')) return;

    try {
        const response = await fetch(`api/persons?personId=${personId}`, { method: 'DELETE' });
        const result = await response.json();

        if (!response.ok || result.success === false) {
            alert(result.message || 'Failed to delete submission.');
            return;
        }

        loadAllSubmissions();
        loadPersons();
        loadManageDisasters();
    } catch (err) {
        alert('Failed to delete submission. Please try again.');
    }
}

async function loadAllSubmissions() {
    const listContainer = document.getElementById('all-submissions-list');
    const response = await fetch('api/persons');
    if (!response.ok) return;

    const persons = await response.json();
    window.allPersonsData = persons;
    if (persons.length === 0) { listContainer.innerHTML = '<p class="empty-note">No submissions yet.</p>'; return; }

    persons.sort((a, b) => String(b.dateReported || '').localeCompare(String(a.dateReported || '')));

    listContainer.innerHTML = persons.map(person => {
        const reporterName = person.reportedBy ? person.reportedBy.fullName : 'Unknown';
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
                    Reported by: ${reporterName}<br>
                    Reported: ${formatDate(person.dateReported)}<br>
                    NIC: ${person.nicNumber || 'N/A'}
                </div>
                <div class="submission-actions">
                    <button class="btn-sm btn-sm--edit" onclick="openEditPerson(${person.personId})">Edit Details</button>
                    <button class="btn-sm btn-sm--delete" onclick="deletePerson(${person.personId})">Delete</button>
                </div>
            </div>`;
    }).join('');
}

async function openEditPerson(personId) {

    const response = await fetch('api/persons');
    if (!response.ok) {
        alert('Could not load this submission. Please try again.');
        return;
    }
    const persons = await response.json();
    window.allPersonsData = persons;

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

    if (person.personType === 'LOST') {
        document.getElementById('editPersonExtraLabel').textContent = 'Circumstances';
        document.getElementById('editPersonExtraInfo').value = person.circumstances || '';
    } else {
        document.getElementById('editPersonExtraLabel').textContent = 'Condition at discovery';
        document.getElementById('editPersonExtraInfo').value = person.conditionAtDiscovery || '';
    }
    document.getElementById('edit-person-form').scrollIntoView({ behavior: 'smooth' });
}

function cancelEditPerson() { document.getElementById('edit-person-form').style.display = 'none'; }

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
        method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
    });
    if (response.ok) {
        alert('Submission updated!');
        cancelEditPerson();
        loadAllSubmissions();
        loadPersons();
        loadManageDisasters();
    } else alert('Update failed.');
}
loadAllSubmissions();

//candidate matching

let currentMatches = [];

async function loadCandidateMatches() {
    const container = document.getElementById('matches-list');
    container.innerHTML = '<p class="empty-note">Loading candidate matches...</p>';

    try {
        const [pendingResponse, historyResponse] = await Promise.all([
            fetch('api/matches'),
            fetch('api/matches?history=true')
        ]);
        const pending = await pendingResponse.json();
        const decided = await historyResponse.json();


        decided.sort((a, b) => String(b.verifiedAt || '').localeCompare(String(a.verifiedAt || '')));

        currentMatches = [...pending, ...decided];

        if (currentMatches.length === 0) {
            container.innerHTML = '<p class="empty-note">No candidate matches right now.</p>';
            return;
        }

        container.innerHTML = currentMatches.map((match, index) => {
            const missing = match.missingPerson;
            const found = match.foundPerson;
            const confidencePercent = Math.round(match.confidenceScore);
            const isDecided = match.status === 'CONFIRMED' || match.status === 'REJECTED';

            const missingPhoto = missing.photoUrl
                ? `<img src="${missing.photoUrl}" class="submission-thumb" alt="${missing.fullName}" />`
                : `<div class="submission-thumb submission-thumb--placeholder"></div>`;
            const foundPhoto = found.photoUrl
                ? `<img src="${found.photoUrl}" class="submission-thumb" alt="${found.fullName}" />`
                : `<div class="submission-thumb submission-thumb--placeholder"></div>`;

            const actionsOrStatus = isDecided
                ? `<div style="margin-top:8px;">
                       <span class="status-badge status-${match.status}">${match.status === 'CONFIRMED' ? 'APPROVED' : 'REJECTED'}</span>
                       <span style="color:#777; margin-left:8px;">${formatDate(match.verifiedAt)}</span>
                   </div>`
                : `<div class="match-actions">
                       <button class="btn-confirm"
                           onclick="decideMatch(${missing.personId}, ${found.personId}, ${match.confidenceScore}, 'confirm', this)">
                           Confirm Match
                       </button>
                       <button class="btn-reject"
                           onclick="decideMatch(${missing.personId}, ${found.personId}, ${match.confidenceScore}, 'reject', this)">
                           Reject
                       </button>
                   </div>`;

            return `
                <div class="match-card">
                    <div class="pair">
                        <div style="display:flex; align-items:center; gap:8px; cursor:pointer;" onclick="showPersonDetails(${index}, 'missing')">
                            ${missingPhoto}
                            <div>
                                <div><b>${missing.fullName}</b></div>
                                <span class="status-badge status-MISSING">MISSING</span>
                            </div>
                        </div>
                        <div style="align-self:center; font-size:18px; color:#999;">&harr;</div>
                        <div style="display:flex; align-items:center; gap:8px; cursor:pointer;" onclick="showPersonDetails(${index}, 'found')">
                            <div style="text-align:right;">
                                <div><b>${found.fullName}</b></div>
                                <span class="status-badge status-FOUND">FOUND</span>
                            </div>
                            ${foundPhoto}
                        </div>
                    </div>
                    <div>Confidence: ${confidencePercent}%</div>
                    <div class="confidence-bar-bg">
                        <div class="confidence-bar-fill" style="width:${confidencePercent}%;"></div>
                    </div>
                    ${actionsOrStatus}
                </div>`;
        }).join('');
    } catch (err) {
        container.innerHTML = '<p class="empty-note">Failed to load candidate matches.</p>';
    }
}

function showPersonDetails(matchIndex, side) {
    const match = currentMatches[matchIndex];
    const person = side === 'missing' ? match.missingPerson : match.foundPerson;

    const photoHtml = person.photoUrl
        ? `<img src="${person.photoUrl}" style="width:100%; max-height:220px; object-fit:cover; border-radius:6px; margin-bottom:12px;" />`
        : '';

    const isLost = person.circumstances !== undefined;
    const extraLabel = isLost ? 'Circumstances' : 'Condition at discovery';
    const extraValue = (isLost ? person.circumstances : person.conditionAtDiscovery) || 'N/A';
    const location = person.lastKnownLocation || person.foundLocation;

    document.getElementById('person-details-content').innerHTML = `
        ${photoHtml}
        <h2 style="margin:0 0 6px 0;">${person.fullName}</h2>
        <span class="status-badge status-${person.status}">${person.status}</span>
        <p style="margin-top:12px;"><b>Age:</b> ${person.age} &nbsp; <b>Gender:</b> ${person.gender}</p>
        <p><b>NIC:</b> ${person.nicNumber || 'N/A'}</p>
        <p><b>Description:</b> ${person.physicalDescription || 'N/A'}</p>
        <p><b>${extraLabel}:</b> ${extraValue}</p>
        <p><b>Location:</b> ${location ? location.address : 'N/A'}</p>
        <p><b>Reported:</b> ${formatDate(person.dateReported)}</p>
    `;
    document.getElementById('person-details-modal').style.display = 'flex';
}

function closePersonDetails() {
    document.getElementById('person-details-modal').style.display = 'none';
}

async function decideMatch(missingPersonId, foundPersonId, confidenceScore, action, buttonEl) {
    const card = buttonEl.closest('.match-card');
    card.style.opacity = '0.5';
    card.style.pointerEvents = 'none';

    try {
        const response = await fetch('api/matches', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action, missingPersonId, foundPersonId, confidenceScore })
        });
        const result = await response.json();

        if (!response.ok || result.success === false) {
            alert(result.message || 'Failed to update match.');
            card.style.opacity = '1';
            card.style.pointerEvents = 'auto';
            return;
        }


        loadCandidateMatches();
        loadAllSubmissions();
        loadPersons();
    } catch (err) {
        alert('Failed to update match. Please try again.');
        card.style.opacity = '1';
        card.style.pointerEvents = 'auto';
    }
}

// create and manage disasters
function submitDisasterEvent() {
    const payload = {
        type: document.getElementById('disasterType').value,
        description: document.getElementById('disasterDesc').value,
        radiusKm: parseFloat(document.getElementById('disasterRadius').value),
        latitude: parseFloat(document.getElementById('disasterLat').value),
        longitude: parseFloat(document.getElementById('disasterLng').value),
        address: document.getElementById('disasterAddress').value
    };
    if (!payload.radiusKm || !payload.latitude) { alert("Please fill all fields."); return; }

    fetch('api/disasters', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
        .then(r => {
            if (r.ok) {
                alert("Disaster created!");
                loadDisasterZones();
                loadManageDisasters();
                loadDisasterDropdown();
            }
        });
}

async function loadManageDisasters() {
    const listContainer = document.getElementById('disasters-list-container');


    const timestamp = new Date().getTime();
    const [disastersRes, personsRes] = await Promise.all([
        fetch('api/disasters?t=' + timestamp),
        fetch('api/persons?t=' + timestamp)
    ]);

    if (!disastersRes.ok) return;

    const disasters = await disastersRes.json();
    const persons = await personsRes.json();


    console.log("FRESHEST PERSONS DATA:", persons);

    if (disasters.length === 0) {
        listContainer.innerHTML = '<p class="empty-note">No disasters recorded.</p>';
        return;
    }

    listContainer.innerHTML = disasters.map(d => {

        const targetId = d.disasterId || d.id;

        const related = persons.filter(p => {

            const personDisasterId = p.disasterId || (p.disasterEvent ? (p.disasterEvent.disasterId || p.disasterEvent.id) : null);
            return personDisasterId === targetId;
        });

        const lostCount = related.filter(p => p.status === 'MISSING').length;
        const foundCount = related.filter(p => p.status === 'FOUND').length;

        const safeDesc = d.description ? d.description.replace(/'/g, "&#39;") : "";
        const safeAddress = d.location.address ? d.location.address.replace(/'/g, "&#39;") : "";

        return `
            <div class="disaster-card">
                <div class="disaster-card__header">
                    <span class="disaster-card__type">${d.type}</span>
                    <span class="disaster-card__location">${d.location.address || 'Unknown Area'}</span>
                </div>
                <div class="disaster-card__stats">
                    <div class="disaster-stat disaster-stat--lost">
                        <span class="disaster-stat__value">${lostCount}</span>
                        <span class="disaster-stat__label">Lost</span>
                    </div>
                    <div class="disaster-card__stats-divider"></div>
                    <div class="disaster-stat disaster-stat--found">
                        <span class="disaster-stat__value">${foundCount}</span>
                        <span class="disaster-stat__label">Found</span>
                    </div>
                </div>
                <button class="btn-edit-disaster"
                    onclick="openEditDisaster(${targetId}, '${d.type}', '${safeDesc}', ${d.radiusKm}, ${d.location.latitude}, ${d.location.longitude}, '${safeAddress}')">Edit Disaster</button>
            </div>`;
    }).join('');
}

function openEditDisaster(id, type, desc, radius, lat, lng, address) {
    document.getElementById('edit-disaster-form').style.display = 'block';
    document.getElementById('editDisasterId').value = id;
    document.getElementById('editDisasterType').value = type;
    document.getElementById('editDisasterDesc').value = desc;
    document.getElementById('editDisasterRadius').value = radius;
    document.getElementById('editDisasterLat').value = lat;
    document.getElementById('editDisasterLng').value = lng;
    document.getElementById('editDisasterAddress').value = address;
    document.getElementById('edit-disaster-form').scrollIntoView({ behavior: 'smooth' });
}

function cancelEditDisaster() { document.getElementById('edit-disaster-form').style.display = 'none'; }

async function updateDisasterEvent() {
    const payload = {
        disasterId: parseInt(document.getElementById('editDisasterId').value),
        type: document.getElementById('editDisasterType').value,
        description: document.getElementById('editDisasterDesc').value,
        radiusKm: parseFloat(document.getElementById('editDisasterRadius').value),
        latitude: parseFloat(document.getElementById('editDisasterLat').value),
        longitude: parseFloat(document.getElementById('editDisasterLng').value),
        address: document.getElementById('editDisasterAddress').value
    };
    const response = await fetch('api/disasters', {
        method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
    });
    if (response.ok) {
        alert('Updated!');
        cancelEditDisaster();
        loadManageDisasters();
        loadDisasterZones();
    }
}
loadManageDisasters();
// pending approvals admin
async function loadPendingApprovals() {
    const container = document.getElementById('pending-admins-list');
    container.innerHTML = '<p class="empty-note">Loading pending approvals...</p>';

    try {
        const response = await fetch('api/pending-admins');
        const pending = await response.json();

        if (!Array.isArray(pending) || pending.length === 0) {
            container.innerHTML = '<p class="empty-note">No pending admin approvals right now.</p>';
            return;
        }

        container.innerHTML = pending.map(admin => `
            <div class="submission-card">
                <div class="name">${admin.fullName}</div>
                <div style="margin-top:6px; color: var(--text-muted); font-size:12.5px;">
                    Username: ${admin.username}<br>
                    Contact: ${admin.contactNumber || 'N/A'}<br>
                    Department: ${admin.department || 'N/A'}
                </div>
                <div class="submission-actions">
                    <button class="btn-sm btn-sm--edit" style="flex:1;" onclick="approvePendingAdmin(${admin.id}, this)">Approve</button>
                </div>
            </div>
        `).join('');
    } catch (err) {
        container.innerHTML = '<p class="empty-note">Failed to load pending approvals.</p>';
        console.error('Failed to load pending admins:', err);
    }
}

async function approvePendingAdmin(userId, btn) {
    btn.disabled = true;
    btn.textContent = 'Approving...';
    try {
        const response = await fetch('api/pending-admins', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: userId })
        });
        const result = await response.json();
        if (result.success) {
            loadPendingApprovals();
        } else {
            alert(result.message || 'Failed to approve this account.');
            btn.disabled = false;
            btn.textContent = 'Approve';
        }
    } catch (err) {
        alert('Failed to approve this account.');
        btn.disabled = false;
        btn.textContent = 'Approve';
    }
}

loadPendingApprovals();