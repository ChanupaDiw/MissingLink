package interfaces;

import model.Location;

// Implemented by any entity that has a position on the map
public interface Locatable {
    Location getLocation();
}
