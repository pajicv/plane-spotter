package com.planespotter.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.planespotter.domain.GeoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OrientationData(
    val azimuth: Float = 0f,    // 0-360 compass heading
    val elevation: Float = 0f,  // -90 to +90 above horizon
    val roll: Float = 0f
)

class OrientationProvider(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    private val _orientation = MutableStateFlow(OrientationData())
    val orientation: StateFlow<OrientationData> = _orientation.asStateFlow()

    private var smoothedAz = 0f
    private var smoothedEl = 0f
    private val smoothing = 0.08f

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    val isAvailable: Boolean get() = rotationSensor != null

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR &&
            event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR) return

        // Get rotation matrix from fused rotation vector
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        // Remap for portrait mode (phone held upright like a camera)
        // AXIS_X stays horizontal, AXIS_Z becomes the vertical axis
        // This makes getOrientation return compass heading when phone is in portrait
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            remappedMatrix
        )

        // Extract orientation angles
        SensorManager.getOrientation(remappedMatrix, orientationAngles)

        // [0] = azimuth in radians (-PI to PI, 0 = North)
        // [1] = pitch in radians (negative = looking up)
        // [2] = roll in radians
        val rawAz = ((Math.toDegrees(orientationAngles[0].toDouble()).toFloat()) + 360f) % 360f
        val rawEl = -Math.toDegrees(orientationAngles[1].toDouble()).toFloat() // negate: pitch negative = looking up = positive elevation

        smoothedAz = GeoUtils.smoothAngle(smoothedAz, rawAz, smoothing)
        smoothedEl += (rawEl - smoothedEl) * smoothing

        _orientation.value = OrientationData(
            azimuth = smoothedAz,
            elevation = smoothedEl,
            roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
