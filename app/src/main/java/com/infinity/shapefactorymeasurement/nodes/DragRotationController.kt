package com.infinity.shapefactorymeasurement.nodes
import android.os.Handler
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.BaseTransformationController
import com.google.ar.sceneform.ux.DragGesture
import com.google.ar.sceneform.ux.DragGestureRecognizer


class DragRotationController(
    private val transformableNode: DragTransformableNode,
    gestureRecognizer: DragGestureRecognizer
) :
    BaseTransformationController<DragGesture>(transformableNode, gestureRecognizer) {

    companion object {

        private const val initialLat = 26.15444376319647
        private const val initialLong = 18.995950736105442

        var lat: Double = initialLat
        var long: Double = initialLong
    }

    // Rate that the node rotates in degrees per degree of twisting.
    private var rotationRateDegrees = 0.5f

    public override fun canStartTransformation(gesture: DragGesture): Boolean {
        return transformableNode.isSelected
    }

    private fun getX(lat: Double, long: Double): Float {
        return (transformableNode.radius * Math.cos(Math.toRadians(lat)) * Math.sin(Math.toRadians(long))).toFloat()
    }

    private fun getY(lat: Double, long: Double): Float {
        return transformableNode.radius * Math.sin(Math.toRadians(lat)).toFloat()
    }

    private fun getZ(lat: Double, long: Double): Float {
        return (transformableNode.radius * Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(long))).toFloat()
    }

    override fun onActivated(node: Node?) {
        super.onActivated(node)
        Handler().postDelayed({
            transformCamera(lat, long)
        }, 0)
    }

    public override fun onContinueTransformation(gesture: DragGesture) {

        val rotationAmountY = gesture.delta.y * rotationRateDegrees
        val rotationAmountX = gesture.delta.x * rotationRateDegrees
        val deltaAngleY = rotationAmountY.toDouble()
        val deltaAngleX = rotationAmountX.toDouble()

        long -= deltaAngleX
        lat += deltaAngleY

        //lat = Math.max(Math.min(lat, 90.0), 0.0)

        transformCamera(lat, long)
    }

    private fun transformCamera(lat: Double, long: Double) {
        val camera = transformableNode.scene!!.camera

        var rot = Quaternion.eulerAngles(Vector3(0F, 0F, 0F))
        val pos = Vector3(getX(lat, long), getY(lat, long), getZ(lat, long))
        rot = Quaternion.multiply(rot, Quaternion(Vector3.up(), (long).toFloat()))
        rot = Quaternion.multiply(rot, Quaternion(Vector3.right(), (-lat).toFloat()))
        camera.localRotation = rot
        camera.localPosition = pos
    }

    fun resetInitialState() {
        transformCamera(initialLat, initialLong)
    }



    public override fun onEndTransformation(gesture: DragGesture) {}


}