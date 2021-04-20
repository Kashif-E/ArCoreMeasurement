package com.infinity.shapefactorymeasurement

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.ViewModel
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import java.util.ArrayList

class ViewModel() :ViewModel(){
    lateinit var widthlinerender: ModelRenderable
    lateinit var pointrender: ModelRenderable
    lateinit var heightlinerender: ModelRenderable
    private var labelArray: ArrayList<AnchorNode> = ArrayList()

    fun setLists(label: ArrayList<AnchorNode>){
        labelArray = ArrayList<AnchorNode>()
        labelArray.addAll(label)

    }

    fun getLabels(): ArrayList<AnchorNode> {
        return labelArray
    }



    fun setRenderables(pointRender: ModelRenderable?, widthLineRender: ModelRenderable?, heightLineRender: ModelRenderable?) {
        this.pointrender = pointRender!!
        this.widthlinerender = widthLineRender!!
        this.heightlinerender = heightLineRender!!

    }
}