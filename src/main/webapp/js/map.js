// --- Map setup ---

const map = L.map('map').setView([7.8731, 80.7718], 8); //map centering on sri lanka

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors'
}).addTo(map);

// lost,fond,disaster zones layers
const lostLayer = L.layerGroup().addTo(map);
const foundLayer = L.layerGroup().addTo(map);
const disasterZoneLayer = L.layerGroup().addTo(map);

L.control.layers(null, {
    "Lost persons": lostLayer,
    "Found persons": foundLayer,
    "Disaster zones": disasterZoneLayer
}).addTo(map);

// default icon
const lostFallbackIcon = L.icon({
    iconUrl: 'images/pin-red.png',
    iconSize: [32, 32],
    iconAnchor: [16, 32]
});

const foundFallbackIcon = L.icon({
    iconUrl: 'images/pin-green.png',
    iconSize: [32, 32],
    iconAnchor: [16, 32]
});

//photo marker
function createPhotoIcon(photoUrl, borderColor) {
    return L.divIcon({
        html: `<img src="${photoUrl}" class="photo-marker"
                    style="width:42px; height:42px; border: 3px solid ${borderColor};" />`,
        className: '',
        iconSize: [42, 42],
        iconAnchor: [21, 42]
    });
}

// popup

function buildPopupHtml(person) {
    const photoHtml = person.photoUrl
        ? `<img src="${person.photoUrl}" class="popup-photo" />`
        : '';

    return `
        <div class="popup-content">
            ${photoHtml}
            <b>${person.fullName}</b><br>
            Age: ${person.age}, ${person.gender}<br>
            Status: ${person.status}
        </div>
    `;
}



async function loadPersons() {
    const response = await fetch('api/persons');
    const persons = await response.json();

    persons.forEach(person => {
        const isFound = person.status === 'FOUND';
        const location = isFound ? person.foundLocation : person.lastKnownLocation;

        if (!location) {
            return; 
        }

        const borderColor = isFound ? '#2ecc71' : '#e74c3c';
        const icon = person.photoUrl
            ? createPhotoIcon(person.photoUrl, borderColor)
            : (isFound ? foundFallbackIcon : lostFallbackIcon);

        const marker = L.marker([location.latitude, location.longitude], { icon })
            .bindPopup(buildPopupHtml(person));

        marker.addTo(isFound ? foundLayer : lostLayer);
    });
}

async function loadDisasterZones() {
    const response = await fetch('api/disasters');
    const disasters = await response.json();

    disasters.forEach(disaster => {
        L.circle([disaster.location.latitude, disaster.location.longitude], {
            radius: disaster.radiusKm * 1000, // Leaflet circles use meters
            color: 'red',
            fillColor: '#f03',
            fillOpacity: 0.15
        })
            .bindPopup(`<b>${disaster.type}</b><br>${disaster.description}`)
            .addTo(disasterZoneLayer);
    });
}

loadDisasterZones();
loadPersons();