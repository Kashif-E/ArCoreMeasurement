package com.infinity.shapefactorymeasurement.views


import android.graphics.Color.rgb
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3.zero
import com.google.ar.sceneform.rendering.*

import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.infinity.shapefactorymeasurement.R
import com.infinity.shapefactorymeasurement.viewmodel.ViewModel
import com.infinity.shapefactorymeasurement.databinding.FragmentArBinding

import java.util.*
import kotlin.collections.ArrayList


class ArFragment: Fragment(R.layout.fragment_ar) {
    private lateinit var viewRenderable: ViewRenderable
    private lateinit var distanceInMeters: CardView
    private lateinit var lineBetween: AnchorNode
    private var lengthLabel: AnchorNode? = null
    private var node2Pos: Vector3? = null
    private var node1Pos: Vector3? = null
    private var initialAnchor: AnchorNode? = null
    lateinit var binding: FragmentArBinding
    lateinit var arFragment: ArFragment
    private var anchorNodeTemp: AnchorNode? = null
    private var pointRender: ModelRenderable? = null
    private var aimRender: ModelRenderable? = null
    private var widthLineRender: ModelRenderable? = null

    private var heightLineRender: ModelRenderable? = null
    private val currentAnchorNode = ArrayList<AnchorNode>()
    private val labelArray: ArrayList<AnchorNode> = ArrayList()
    private val currentAnchor = ArrayList<Anchor?>()
    private var totalLength = 0f
    private var difference: Vector3? = null
    private val tempAnchorNodes: ArrayList<AnchorNode> = arrayListOf()
    private val viewModel: ViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentArBinding.bind(view)
        arFragment = childFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment

        binding.btnClear.setOnClickListener {
            clearAnchors()
        }

        binding.floatingActionButton2.setOnClickListener {
            viewModel.setLists(tempAnchorNodes)
            viewModel.setRenderables(pointRender, widthLineRender,heightLineRender)
            findNavController().navigate(R.id.action_arFragment_to_sceneViewFragment)
        }

        initObjects()

        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->

            refreshAim(hitResult, motionEvent)
        }
        binding.btnAdd.setOnClickListener {
            addFromAim()
        }
        arFragment.arSceneView.scene.addOnUpdateListener {

            touchScreenCenterConstantly()
            updateDistance()
        }
    }
    private fun updateDistance() {

        anchorNodeTemp?.let {

            if (::lineBetween.isInitialized) {
                arFragment.arSceneView.scene.removeChild(lineBetween)
            }

            if (currentAnchorNode.size < 2) {
                node1Pos = initialAnchor?.worldPosition
                node2Pos = anchorNodeTemp?.worldPosition
            } else {
                node1Pos = currentAnchorNode[currentAnchorNode.size - 1].worldPosition
                node2Pos = anchorNodeTemp?.worldPosition
            }
            calculateDistance(node1Pos!!, node2Pos!!)

        }
    }

    private fun calculateDistance(node1Pos: Vector3, node2Pos: Vector3) {
        difference = Vector3.subtract(node1Pos, node2Pos)
        totalLength += difference!!.length()
        val rotationFromAToB = Quaternion.lookRotation(
                difference!!.normalized(),
                Vector3.up()
        )
        //setting lines between points
        lineBetween = AnchorNode().apply {
            setParent(arFragment.arSceneView.scene)
            worldPosition = Vector3.add(node1Pos, node2Pos).scaled(.5f)
            worldRotation = rotationFromAToB
            localScale = Vector3(1f, 1f, difference!!.length())
            renderable = widthLineRender
        }
        //settinglabel
        if (lengthLabel == null) {
            lengthLabel = AnchorNode()
            lengthLabel!!.setParent(arFragment.arSceneView.scene)
        }
        lengthLabel!!.worldPosition = Vector3.add(node1Pos, node2Pos).scaled(.5f)
        initTextBoxes(difference!!.length(), lengthLabel!!, false)
    }


    private fun initObjects() {
        MaterialFactory.makeOpaqueWithColor(requireContext(), Color(rgb(219, 68, 55)))
                .thenAccept { material: Material? ->
                    heightLineRender = ShapeFactory.makeCube(Vector3(.015f, 1f, 1f), zero(), material)
                    heightLineRender!!.apply {
                        isShadowCaster = false
                        isShadowReceiver = false
                    }
                }

        MaterialFactory.makeTransparentWithColor(requireContext(), Color(rgb(23, 107, 230)))
                .thenAccept { material: Material? ->
                    pointRender = ShapeFactory.makeCylinder(0.02f, 0.0003f, zero(), material)
                    pointRender!!.isShadowCaster = false
                    pointRender!!.isShadowReceiver = false
                }

        ViewRenderable.builder()
                .setView(requireContext() , R.layout.distance)
                .build()
                .thenAccept { renderable: ViewRenderable ->
                    renderable.apply {
                        isShadowCaster = false
                        isShadowReceiver = false
                        verticalAlignment = ViewRenderable.VerticalAlignment.BOTTOM
                    }
                    viewRenderable = renderable
                }

        Texture.builder()
                .setSource(requireContext(), R.drawable.aim)
                .build().thenAccept { texture ->
                    MaterialFactory.makeTransparentWithTexture(requireContext(), texture)
                            .thenAccept { material: Material? ->
                                aimRender = ShapeFactory.makeCylinder(0.08f, 0f, zero(), material)
                                aimRender!!.isShadowCaster = false
                                aimRender!!.isShadowReceiver = false
                            }
                }
        MaterialFactory.makeOpaqueWithColor(requireContext(), Color(rgb(23, 107, 230)))
                .thenAccept { material: Material? ->
                    widthLineRender = ShapeFactory.makeCube(Vector3(.01f, 0f, 1f), zero(), material)
                    widthLineRender!!.apply {
                        isShadowCaster = false
                        isShadowReceiver = false
                    }
                }
    }

    private fun refreshAim(hitResult: HitResult, motionEvent: MotionEvent) {
        if (motionEvent.metaState == 0) {
            if (anchorNodeTemp != null) {
                anchorNodeTemp!!.anchor!!.detach()
            }
            if (anchorNodeTemp == null) {
                initialAnchor = AnchorNode(hitResult.createAnchor())
                currentAnchorNode.add(initialAnchor!!)
                tempAnchorNodes.add(initialAnchor!!)
            }
            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)
            val transformableNode = TransformableNode(arFragment.transformationSystem)
            transformableNode.renderable = aimRender
            transformableNode.setParent(anchorNode)
            arFragment.arSceneView.scene.addChild(anchorNode)
            anchorNodeTemp = anchorNode

        }
    }

    // add points to the surface based on the crosshair position
    // add lines between points
    // add labels
    private fun addFromAim() {
        if (anchorNodeTemp != null) {
            tempAnchorNodes.add(anchorNodeTemp!!)
            val worldPosition = anchorNodeTemp!!.worldPosition
            val worldRotation = anchorNodeTemp!!.worldRotation
            // add point
            worldPosition.x += 0.0000001f
            val confirmedAnchorNode = AnchorNode()
            confirmedAnchorNode.worldPosition = worldPosition
            confirmedAnchorNode.worldRotation = worldRotation
            val anchor = confirmedAnchorNode.anchor
            confirmedAnchorNode.setParent(arFragment.arSceneView.scene)
            TransformableNode(arFragment.transformationSystem).apply {
                renderable = pointRender
                setParent(confirmedAnchorNode)
            }
            arFragment.arSceneView.scene.addChild(confirmedAnchorNode)
            currentAnchor.add(anchor)
            currentAnchorNode.add(confirmedAnchorNode)
            if (currentAnchorNode.size >= 2) {

                difference = Vector3.subtract(node1Pos, node2Pos)
                totalLength += difference!!.length()
                val rotationFromAToB =
                        Quaternion.lookRotation(difference!!.normalized(), Vector3.up())
                //setting lines between points
                AnchorNode().apply {
                    setParent(arFragment.arSceneView.scene)
                    this.worldPosition = Vector3.add(node1Pos, node2Pos).scaled(.5f)
                    this.worldRotation = rotationFromAToB
                    localScale = Vector3(1f, 1f, difference!!.length())
                    renderable = widthLineRender
                }
                //setting labels with distances
                labelArray.add(AnchorNode().apply {
                    setParent(arFragment.arSceneView.scene)
                    this.worldPosition = Vector3.add(node1Pos, node2Pos).scaled(.5f)
                    initTextBoxes(difference!!.length(), this, true)
                })
            }
        }
    }

    private fun initTextBoxes(
            meters: Float,
            transformableNode: AnchorNode,
            isFromCreateNewAnchor: Boolean
    ) {

        if (isFromCreateNewAnchor) {
            ViewRenderable.builder()
                    .setView(requireContext() , R.layout.distance)
                    .build()
                    .thenAccept { renderable: ViewRenderable ->
                        renderable.apply {
                            isShadowCaster = false
                            isShadowReceiver = false
                            verticalAlignment = ViewRenderable.VerticalAlignment.BOTTOM
                        }

                        addDistanceCard(renderable, meters, transformableNode)


                    }
        } else {
            addDistanceCard(viewRenderable, meters, transformableNode)
        }
    }

    private fun addDistanceCard(
            distanceRenderable: ViewRenderable,
            meters: Float,
            transformableNode: AnchorNode
    ) {
        distanceInMeters = distanceRenderable.view as CardView
        val metersString: String = if (meters < 1f) {
            String.format(Locale.ENGLISH, "%.0f", meters * 100) + " cm"
        } else {
            String.format(Locale.ENGLISH, "%.2f", meters) + " m"
        }
        val tv = distanceInMeters.getChildAt(0) as TextView
        tv.text = metersString
        Log.e("meters", metersString)
        transformableNode.renderable = distanceRenderable
    }

    // imitate clicks to the center of the screen (to the crosshair)
    private fun touchScreenCenterConstantly() {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis() + 10
        val x = this.resources.displayMetrics.widthPixels.toFloat() / 2
        val y = this.resources.displayMetrics.heightPixels.toFloat() / 2
        val motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                0
        )
        arFragment.arSceneView.dispatchTouchEvent(motionEvent)
    }






    // rotate labels according to camera movements
    private fun labelsRotation() {
        val cameraPosition = arFragment.arSceneView.scene.camera.worldPosition
        for (labelNode in labelArray) {
            val labelPosition = labelNode.worldPosition
            val direction = Vector3.subtract(cameraPosition, labelPosition)
            val lookRotation = Quaternion.lookRotation(direction, Vector3.up())
            labelNode.worldRotation = lookRotation
        }
    }

    fun clearAnchors() {
     for (i in currentAnchorNode){
         arFragment.arSceneView.scene.removeChild(i)
     }
        currentAnchorNode.clear()
        currentAnchor.clear()
        labelArray.clear()
        totalLength = 0f

    }


    override fun onStart() {
        super.onStart()
        if(::arFragment.isInitialized){
            arFragment.onStart()
        }
    }

    override fun onPause() {
        super.onPause()
        if(::arFragment.isInitialized){
            arFragment.onPause()
        }
    }

    override fun onResume() {
        super.onResume()
        if(::arFragment.isInitialized){
            arFragment.onResume()
        }
    }
}

