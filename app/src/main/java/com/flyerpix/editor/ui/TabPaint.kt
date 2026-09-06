package com.flyerpix.editor.ui


import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.flyerpix.editor.R
import com.flyerpix.editor.databinding.FragmentTabPaintBinding

/**
 * Tab Paint Fragment.
 */
class TabPaint : Fragment() {
    private var colorsRadioGroup: RadioGroup? = null
    private var currentColor: Int = 0
    lateinit var tabPaintListener: TabPaintListener
    private var binding: FragmentTabPaintBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTabPaintBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //initializeColorPalette()
        initializeStrokeListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initializeStrokeListener() {
        val strokeSeekBar = binding!!.strokeSeekBar
        val strokeText = binding!!.strokeText
        strokeSeekBar.max = 32
        strokeSeekBar.min = 8
        strokeText.text = strokeSeekBar.progress.toString()
        strokeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val stroke = Math.max(8, Math.min(32, progress))
                strokeText.text = stroke.toString()
                tabPaintListener.onStrokeChanged(stroke.toFloat())
            }
        })
    }

    /*
    private fun initializeColorPalette() {
        palettePaint.setFixedColumnCount(resources.getIntArray(R.array.palette).size)
        //Select color
        if (currentColor == 0) {
            palettePaint.setSelectedColor(Color.BLACK)
        } else {
            palettePaint.setSelectedColor(currentColor)
        }
        //On color click
        palettePaint.setOnColorSelectedListener { color ->
            currentColor = color
            tabPaintListener.onColorSelected(currentColor)
        }
    }*/

    interface TabPaintListener {
        fun onStrokeChanged(currentStroke: Float)
    }
}