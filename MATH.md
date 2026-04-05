# Math Behind Plane Spotter

A breakdown of the geometry, trigonometry, and sensor math that makes the AR flight tracker work.

---

## 1. Haversine Distance

**Problem:** Given two GPS coordinates (you and an aircraft), how far apart are they?

GPS gives us latitude/longitude on a sphere. Straight-line Euclidean distance doesn't work because the Earth is curved. The **Haversine formula** calculates the great-circle distance — the shortest path along the surface of a sphere.

```
a = sin²(Δlat/2) + cos(lat1) · cos(lat2) · sin²(Δlon/2)
distance = R · 2 · atan2(√a, √(1-a))
```

Where:
- `Δlat = lat2 - lat1` (in radians)
- `Δlon = lon2 - lon1` (in radians)
- `R = 6371 km` (Earth's mean radius)
- Result is in kilometers

**Why atan2 instead of asin?** `atan2(√a, √(1-a))` is numerically more stable than `asin(√a)` for very small and very large distances.

**Example:** You're at 44.8°N, 20.4°E (Belgrade). A plane is at 45.0°N, 20.6°E. Haversine gives ~25km.

---

## 2. Bearing (Azimuth to Aircraft)

**Problem:** Which compass direction do I need to look to see the aircraft?

Given your position and the aircraft's position, the **initial bearing** (forward azimuth) tells you which direction to face:

```
y = sin(Δlon) · cos(lat2)
x = cos(lat1) · sin(lat2) - sin(lat1) · cos(lat2) · cos(Δlon)
bearing = atan2(y, x)
```

Result is converted from radians to degrees, then normalized to 0-360°:
```
bearing = (degrees(atan2(y, x)) + 360) % 360
```

**Compass convention:** 0° = North, 90° = East, 180° = South, 270° = West.

**Why this works:** Imagine standing at your GPS position on a globe. The formula computes the angle of the great-circle arc to the aircraft, measured clockwise from true north. The `y` component captures the east-west contribution, and `x` captures the north-south contribution adjusted for convergence of meridians.

---

## 3. Elevation Angle

**Problem:** How far above the horizon is the aircraft from my point of view?

This is simpler than you might think. We treat it as a right triangle:

```
           aircraft
           /|
          / |
         /  | altitude (meters)
        /   |
       / θ  |
you --/-----|
   ground distance (meters)
```

```
elevation = atan2(altitude, ground_distance)
```

Where:
- `altitude` = aircraft altitude in meters (converted from feet via the API)
- `ground_distance` = haversine distance × 1000 (converted to meters)

**Example:** A plane at 10,000m altitude, 25km away:
```
θ = atan2(10000, 25000) = atan2(0.4) ≈ 21.8°
```

**Simplification:** We ignore Earth's curvature for elevation because at <100km distances the error is negligible (<0.05°). We also ignore our own altitude (assumed ~0m compared to aircraft at thousands of meters).

---

## 4. Angular Difference (Wrapping)

**Problem:** How do I calculate the difference between two compass angles?

Naive subtraction breaks at the 0°/360° boundary. For example, the difference between 350° and 10° should be 20°, not -340°.

```
angleDiff(a, b) = ((b - a + 540) % 360) - 180
```

This returns a value between -180° and +180°:
- Positive = target is clockwise (to the right)
- Negative = target is counterclockwise (to the left)

**Why +540 and -180?** Adding 540 (= 360 + 180) before modulo ensures we stay positive, then subtracting 180 shifts the range from [0, 360) to [-180, +180). This is a standard trick for signed angular difference.

**Examples:**
- `angleDiff(350, 10)` = ((10 - 350 + 540) % 360) - 180 = (200 % 360) - 180 = 200 - 180 = **+20°** (to the right)
- `angleDiff(10, 350)` = ((350 - 10 + 540) % 360) - 180 = (880 % 360) - 180 = 160 - 180 = **-20°** (to the left)

---

## 5. Angle Smoothing (Low-Pass Filter)

**Problem:** Sensor readings are noisy. Raw compass heading jitters by several degrees between frames.

We use **exponential smoothing** (a first-order low-pass filter) that respects angle wrapping:

```
diff = angleDiff(current, target)    // signed, wrap-safe
smoothed = (current + diff × factor + 360) % 360
```

Where `factor = 0.08` (8% of the difference per frame).

**Why not just average?** A simple `current = current × 0.92 + target × 0.08` breaks when current=350° and target=10°. You'd get a weighted average around 180° (wrong side of the compass). By computing the signed angular difference first, we always interpolate the short way around.

**Tradeoff:** Lower factor = smoother but more lag. At 0.08 with ~30Hz sensor rate, it takes roughly 40 frames (~1.3 seconds) to reach 95% of a new value. This feels responsive enough while eliminating visible jitter.

---

## 6. Phone Orientation from Sensors

**Problem:** Which direction is the phone's camera pointing? We need compass heading (azimuth) and how far up/down it's tilted (elevation).

### The Rotation Vector Sensor

Android's `TYPE_ROTATION_VECTOR` is a **fused sensor** — the system combines accelerometer, gyroscope, and magnetometer data using a Kalman filter to produce a stable rotation quaternion. We don't need to do our own sensor fusion.

### The Coordinate System Problem

Android's default coordinate system assumes the phone is **lying flat on a table**:
- X = right
- Y = up (towards top of phone)
- Z = out of the screen (towards the sky)

But we hold the phone **upright in portrait mode** (like a camera). In this position, Y points to the sky and Z points behind the phone. If we use `getOrientation()` without remapping, we get gimbal lock — the azimuth becomes unreliable near ±90° pitch.

### The Fix: remapCoordinateSystem

```
SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values)
SensorManager.remapCoordinateSystem(rotationMatrix, AXIS_X, AXIS_Z, remappedMatrix)
SensorManager.getOrientation(remappedMatrix, orientationAngles)
```

`remapCoordinateSystem(R, AXIS_X, AXIS_Z, outR)` means:
- New X axis = old X axis (still points right)
- New Y axis = old Z axis (now "up" means "out of screen" = the direction the camera faces when phone is upright)

This effectively rotates the coordinate system 90° around the X axis, transforming from "flat on table" to "held upright." Now `getOrientation()` returns:
- `[0]` = azimuth: compass heading of the camera direction (-π to π, 0 = North)
- `[1]` = pitch: tilt up/down (negative = looking up)
- `[2]` = roll: tilt left/right

**This was the whole reason for going native.** Browser APIs (AbsoluteOrientationSensor) extract Euler angles from the quaternion in a way that hits gimbal lock when the phone is upright (90° pitch). Android's `remapCoordinateSystem` avoids this by redefining what "up" means before extracting angles.

### Converting to Degrees

```
azimuth = (degrees(orientationAngles[0]) + 360) % 360    // 0-360°
elevation = -degrees(orientationAngles[1])                 // negate because pitch negative = looking up
```

The negation on elevation: Android reports negative pitch when tilting the top of the phone away from you (i.e., looking upward). We want positive elevation = looking up, so we negate.

---

## 7. Screen Projection

**Problem:** Given where I'm looking (azimuth, elevation) and where the aircraft is (bearing, elevation angle), where should I draw the flight label on screen?

### Camera as a Window

Think of the phone screen as a rectangular window into 3D space. The window has a field of view:
- Horizontal FOV ≈ 60° (typical phone camera)
- Vertical FOV ≈ 45°

The center of the screen corresponds to exactly where the camera is pointing (our azimuth and elevation).

### The Projection

```
dAz = angleDiff(myAzimuth, flightBearing)     // horizontal offset in degrees
dEl = flightElevation - myElevation            // vertical offset in degrees

pxPerDegH = screenWidth / FOV_H               // pixels per degree, horizontal
pxPerDegV = screenHeight / FOV_V              // pixels per degree, vertical

x = screenWidth/2  + dAz × pxPerDegH
y = screenHeight/2 - dEl × pxPerDegV          // minus because screen Y is inverted
```

**Why this works:** For small angles (<30°), the relationship between angular offset and screen position is approximately linear. A flight 10° to the right of center appears at `screenWidth/2 + 10 × pxPerDegH`. This is a **rectilinear projection** — the same as a normal camera lens (not fisheye).

**Screen Y inversion:** In screen coordinates, Y increases downward. But elevation increases upward. So we subtract: a flight 10° above where you're looking should be 10° worth of pixels above center (lower Y value).

### Visibility Check

A flight is "visible" if its projected position falls within the screen bounds (plus a 200px margin so labels can partially slide in/out rather than popping):

```
visible = x ∈ (-200, screenWidth+200) AND y ∈ (-200, screenHeight+200)
```

---

## 8. Putting It All Together

Here's the full pipeline, every frame (~30Hz):

```
1. SENSOR: Phone orientation → azimuth (compass heading), elevation (tilt)
   └─ Rotation vector → rotation matrix → remap for portrait → getOrientation → smooth

2. GPS: Your position → (lat, lon)

3. API (every 10s): Aircraft list → for each aircraft: (lat, lon, altitude, heading, speed)

4. FOR EACH AIRCRAFT:
   a. bearing    = haversine_bearing(myPos, aircraftPos)        → 0-360°
   b. distance   = haversine_distance(myPos, aircraftPos)       → km
   c. elevation  = atan2(altitude, distance)                     → degrees above horizon
   d. dAzimuth   = angleDiff(myAzimuth, bearing)                → signed degrees left/right
   e. dElevation = aircraftElevation - myElevation               → signed degrees up/down
   f. screenX    = screenWidth/2  + dAzimuth × (screenWidth/FOV_H)
   g. screenY    = screenHeight/2 - dElevation × (screenHeight/FOV_V)
   h. if (screenX, screenY) is within bounds → draw label at that position

5. RENDER: Camera feed + HUD overlay + flight labels at computed positions
```

---

## 9. What We Ignore (and Why It's Fine)

| Simplification | Error at 100km | Why OK |
|---|---|---|
| Earth curvature for elevation | ~0.05° | Smaller than sensor noise |
| Our altitude (assume 0m) | ~0.5° at 10km for 100m elevation | Negligible vs aircraft at 10,000m |
| Rectilinear vs spherical projection | <1% at ±30° | Within camera FOV it's imperceptible |
| Wind/aircraft maneuvering | Position up to 10s stale | API refreshes every 10s, acceptable |
| Magnetic vs true north | ~4° in Serbia | Rotation vector sensor uses true north |

---

## 10. Key Constants

| Constant | Value | Source |
|---|---|---|
| Earth radius | 6,371 km | WGS-84 mean |
| Horizontal FOV | 60° | Typical phone camera |
| Vertical FOV | 45° | Typical phone camera |
| Smoothing factor | 0.08 | Tuned empirically |
| Search radius | 100 km | ADSB.lol API parameter |
| Refresh interval | 10 seconds | Balance: freshness vs API load |
| Sensor rate | SENSOR_DELAY_GAME (~30Hz) | Smooth AR without battery drain |
