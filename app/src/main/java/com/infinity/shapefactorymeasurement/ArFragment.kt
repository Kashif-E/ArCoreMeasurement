package com.infinity.shapefactorymeasurement


import android.graphics.Color.BLUE
import android.graphics.Color.WHITE
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene.OnUpdateListener
import com.google.ar.sceneform.Sun
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3.zero
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.infinity.shapefactorymeasurement.databinding.FragmentArBinding
import java.util.*
import kotlin.collections.ArrayList


class ArFragment: Fragment(R.layout.fragment_ar) , OnUpdateListener {

    private val currentAnchorNode = ArrayList<AnchorNode>()
    private val labelArray: ArrayList<AnchorNode> = ArrayList()
    private val currentAnchor = ArrayList<Anchor?>()
    private lateinit var  binding : FragmentArBinding
    private lateinit var arFragment : ArFragment
    private var anchorNodeTemp: AnchorNode? = null
    private var pointRender: ModelRenderable? = null
    private var aimRender: ModelRenderable? = null
    private var widthLineRender: ModelRenderable? = null
    private var heightLineRender: ModelRenderable? = null
    private var totalLength = 0f
    private var difference: Vector3? = null
    private val tempAnchorNodes : ArrayList<AnchorNode> = arrayListOf()
    lateinit var viewModel : ViewModel


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentArBinding.bind(view)
        viewModel = (activity as MainActivity).viewModel
        arFragment = childFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment

       binding.btnClear.setOnClickListener {
           clearAnchors()
       }

        binding.floatingActionButton2.setOnClickListener {
            viewModel.setLists(tempAnchorNodes)
            viewModel.setRenderables(pointRender, widthLineRender,heightLineRender)
            findNavController().navigate(R.id.action_arFragment_to_sceneViewFragment)
        }
      binding.btnAdd.setOnClickListener { addFromAim() }
        initModel()

        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->

            refreshAim(hitResult, plane, motionEvent)


        }



    }

    private fun initModel() {
        MaterialFactory.makeOpaqueWithColor(requireContext(), Color(BLUE))
                .thenAccept { material: Material? ->
                    pointRender = ShapeFactory.makeCylinder(0.018f, 0.0001f, zero(), material)
                    pointRender!!.isShadowCaster = false
                    pointRender!!.isShadowReceiver = false
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

        MaterialFactory.makeOpaqueWithColor(requireContext(), Color(WHITE))
                .thenAccept { material: Material? ->
                    widthLineRender = ShapeFactory.makeCube(Vector3(.015f, 0f, 1f), zero(), material)
                    widthLineRender!!.apply {
                        isShadowCaster = false
                        isShadowReceiver = false
                    }
                }

        MaterialFactory.makeOpaqueWithColor(requireContext(), Color(WHITE))
                .thenAccept { material: Material? ->
                   heightLineRender = ShapeFactory.makeCube(Vector3(.015f, 1f, 1f), zero(), material)
                   heightLineRender!!.apply {
                        isShadowCaster = false
                        isShadowReceiver = false
                    }
                }
    }

    // render the label with a distance
    private fun initTextBox(meters: Float, tN: TransformableNode) {
        ViewRenderable.builder()
                .setView(requireContext(), R.layout.distance)
                .build()
                .thenAccept { renderable: ViewRenderable ->
                    renderable.apply {
                        isShadowCaster = false
                        isShadowReceiver = false
                        verticalAlignment = ViewRenderable.VerticalAlignment.BOTTOM
                    }

                    val distanceInMeters = renderable.view as TextView
                    val metersString: String = if (meters < 1f) {
                        String.format(Locale.ENGLISH, "%.0f", meters * 100) + " cm"
                    } else {
                        String.format(Locale.ENGLISH, "%.2f", meters) + " m"
                    }
                    distanceInMeters.text = metersString
                    tN.renderable = renderable
                }
    }



   override fun onUpdate(frameTime: FrameTime) {
        labelsRotation()
        touchScreenCenterConstantly()
    }

    fun clearAnchors() {
        val children: List<Node> = ArrayList(arFragment.arSceneView.scene.children)
        for (node in children) {
            if (node is AnchorNode) {
                if (node.anchor != null) {
                    node.apply {
                        anchor!!.detach()
                        setParent(null)
                        renderable = null
                    }
                }
            }
            if (node !is Camera && node !is Sun) {
                node.setParent(null)
            }
        }
        currentAnchorNode.clear()
        currentAnchor.clear()
        labelArray.clear()
        totalLength = 0f

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

    // crosshair sliding across the surface
    private fun refreshAim(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        if (motionEvent.metaState == 0) {
            if (anchorNodeTemp != null) {
                anchorNodeTemp!!.anchor!!.detach()
            }
            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)
            val transformableNode = TransformableNode(arFragment.transformationSystem)
            transformableNode.renderable = aimRender
            transformableNode.setParent(anchorNode)
            arFragment.arSceneView.scene.addOnUpdateListener(this)
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
            Log.e("add", "add")
            val worldPosition = anchorNodeTemp!!.worldPosition
            val worldRotation = anchorNodeTemp!!.worldRotation


            // add point
            worldPosition.x += 0.0000001f
            val confirmedAnchorNode = AnchorNode()
            confirmedAnchorNode.worldPosition = worldPosition
            confirmedAnchorNode.worldRotation = worldRotation

            val anchor = confirmedAnchorNode.anchor
            confirmedAnchorNode.setParent(arFragment.arSceneView.scene)

            val transformableNode = TransformableNode(arFragment.transformationSystem).apply {
              renderable = pointRender
              setParent(confirmedAnchorNode)
            }

            arFragment.arSceneView.scene.addChild(confirmedAnchorNode)

            currentAnchor.add(anchor)
            currentAnchorNode.add(confirmedAnchorNode)

            if (currentAnchorNode.size >= 2) {
                val node1Pos = currentAnchorNode[currentAnchorNode.size - 2].worldPosition
                val node2Pos = currentAnchorNode[currentAnchorNode.size - 1].worldPosition

                difference = Vector3.subtract(node1Pos, node2Pos)
                totalLength += difference!!.length()
                val rotationFromAToB = Quaternion.lookRotation(difference!!.normalized(), Vector3.up())


                //setting lines between points
                val lineBetween = AnchorNode()
                lineBetween.setParent(arFragment.arSceneView.scene)
                lineBetween.worldPosition = Vector3.add(node1Pos, node2Pos).scaled(.5f)
                lineBetween.worldRotation = rotationFromAToB
                lineBetween.localScale = Vector3(1f, 1f, difference!!.length())
                val lineNode = TransformableNode(arFragment.transformationSystem)
                lineNode.setParent(lineBetween)
                lineNode.renderable = widthLineRender


                //setting labels with distances
                val lengthLabel = AnchorNode()
                lengthLabel.setParent(arFragment.arSceneView.scene)
                lengthLabel.worldPosition = Vector3.add(node1Pos, node2Pos).scaled(.5f)
                val distanceNode = TransformableNode(arFragment.transformationSystem)
                distanceNode.setParent(lengthLabel)
                initTextBox(difference!!.length(), distanceNode)
                labelArray.add(lengthLabel)

            }
        }
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

