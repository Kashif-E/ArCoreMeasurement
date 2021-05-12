package com.infinity.shapefactorymeasurement.views


import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import com.google.ar.core.Anchor
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformationSystem
import com.infinity.shapefactorymeasurement.MainActivity
import com.infinity.shapefactorymeasurement.R
import com.infinity.shapefactorymeasurement.viewmodel.ViewModel
import com.infinity.shapefactorymeasurement.databinding.FragmentSceneViewBinding
import com.infinity.shapefactorymeasurement.nodes.DragTransformableNode
import java.util.*

class SceneViewFragment : Fragment(R.layout.fragment_scene_view) {



    private lateinit var transformationSystem: TransformationSystem
    private val currentAnchorNode = ArrayList<AnchorNode>()

    private val currentAnchor = ArrayList<Anchor?>()



    private var totalLength = 0f
    private var difference: Vector3? = null
    private lateinit var viewModel: ViewModel
    lateinit var binding: FragmentSceneViewBinding
    lateinit var groundNode: DragTransformableNode
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        viewModel = (activity as MainActivity).viewModel
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSceneViewBinding.bind(view)
        transformationSystem = TransformationSystem(resources.displayMetrics, FootprintSelectionVisualizer())

        val transformationSystem = makeTransformationSystem()
         groundNode = DragTransformableNode(1f, transformationSystem)


        groundNode.select()
        binding.SceneView.scene
            .addOnPeekTouchListener { hitTestResult: HitTestResult?, motionEvent: MotionEvent? ->
                transformationSystem.onTouch(
                    hitTestResult,
                    motionEvent
                )
            }



        addShape()
    }


    private fun makeTransformationSystem(): TransformationSystem {
        val footprintSelectionVisualizer = FootprintSelectionVisualizer()
        return TransformationSystem(resources.displayMetrics, footprintSelectionVisualizer)
    }
    fun addShape(){
        Log.e("size", viewModel.getLabels().size.toString())
        for (anchorNode in viewModel.getLabels()) {

            Log.e("opppo","oppo")
            addfromAim(anchorNode)
        }
        transformationSystem.selectNode(groundNode)
        binding.SceneView.scene.addChild(groundNode)
    }


    private  fun addfromAim(anchorNode: AnchorNode) {

        val anchor = anchorNode.anchor
        anchorNode.setParent(groundNode)

        groundNode.addChild(anchorNode)
        currentAnchor.add(anchor)
        currentAnchorNode.add(anchorNode)
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
            lineBetween.renderable = viewModel.heightlinerender


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
