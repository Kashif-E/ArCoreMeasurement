package com.infinity.shapefactorymeasurement


import android.graphics.Color.*
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.ar.core.Anchor
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import com.infinity.shapefactorymeasurement.databinding.FragmentSceneViewBinding
import java.util.*

class SceneViewFragment : Fragment(R.layout.fragment_scene_view) {


    private lateinit var transformationSystem: TransformationSystem
    private val currentAnchorNode = ArrayList<AnchorNode>()

    private val currentAnchor = ArrayList<Anchor?>()

    private var anchorNodeTemp: AnchorNode? = null

    private var totalLength = 0f
    private var difference: Vector3? = null
    private lateinit var viewModel: ViewModel
    lateinit var binding: FragmentSceneViewBinding
    lateinit var groundNode: TransformableNode
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        viewModel = (activity as MainActivity).viewModel
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSceneViewBinding.bind(view)
        transformationSystem = TransformationSystem(resources.displayMetrics, FootprintSelectionVisualizer())

        groundNode= TransformableNode(transformationSystem)
        groundNode.worldPosition= Vector3(0f,  0.8f, - 7.0f)
        groundNode.translationController.isEnabled = true
        groundNode.rotationController.isEnabled = true
        binding.SceneView.renderer!!.setClearColor(Color(LTGRAY))
       binding.SceneView.scene
                .addOnPeekTouchListener { hitTestResult: HitTestResult?, motionEvent: MotionEvent? ->
                    transformationSystem.onTouch(
                            hitTestResult,
                            motionEvent
                    )
                }
        addShape()
    }

    fun addShape(){
        Log.e("size", viewModel.getLabels().size.toString())
        for (anchorNodes in viewModel.getLabels()) {
            anchorNodeTemp = anchorNodes
            Log.e("opppo","oppo")
            addfromAim()
        }
        transformationSystem.selectNode(groundNode)
        binding.SceneView.scene.addChild(groundNode)
    }


    private  fun addfromAim() {
        if (anchorNodeTemp != null) {

            Log.e("add", "add")
            val worldPosition = anchorNodeTemp!!.worldPosition
            val worldRotation = anchorNodeTemp!!.worldRotation


            worldPosition.x += 0.0000001f
            val confirmedAnchorNode = AnchorNode()
            confirmedAnchorNode.worldPosition = worldPosition
            confirmedAnchorNode.worldRotation = worldRotation

            val anchor = confirmedAnchorNode.anchor
            confirmedAnchorNode.setParent(groundNode)

            groundNode.addChild(confirmedAnchorNode)

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
                lineBetween.setParent(groundNode)
                lineBetween.worldPosition = Vector3.add(node1Pos, node2Pos).scaled(.5f)
                lineBetween.worldRotation = rotationFromAToB
                lineBetween.localScale = Vector3(1f, 1f, difference!!.length())
                val lineNode = TransformableNode(transformationSystem)
                lineNode.setParent(lineBetween)
                lineNode.renderable = viewModel.heightlinerender


            }
        }

    }

    override fun onResume() {
        super.onResume()
        try {
            binding.SceneView.resume()
        }
        catch (e: CameraNotAvailableException)
        {
            e.message
        }


    }

    override fun onPause() {
        super.onPause()
        binding.SceneView.pause()
    }
}
