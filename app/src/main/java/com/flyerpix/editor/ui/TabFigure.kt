package com.flyerpix.editor.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.Button
import androidx.fragment.app.Fragment
import com.flyerpix.editor.R
import com.flyerpix.editor.databinding.FragmentTabFigureBinding
import com.flyerpix.editor.editableimageview.figures.Figure.CIRCLE
import com.flyerpix.editor.editableimageview.figures.Figure.SQUARE
import com.flyerpix.editor.editableimageview.figures.Figure.LINE
import com.google.android.material.tabs.TabLayout

/**
 * Tab Figure Fragment.
 */
class TabFigure : Fragment() {
    lateinit var tabFigureListener: TabFigureListener
    private var currentColor: Int = 0
    lateinit var tabPaintListener: TabPaint.TabPaintListener
    private var binding: FragmentTabFigureBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTabFigureBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tabFigureListener.onFigureSelected(0)
        //circlesButton!!.setOnClickListener { tabFigureListener.onFigureSelected(CIRCLE) }
        //squaresButton!!.setOnClickListener { tabFigureListener.onFigureSelected(SQUARE) }
        //linesButton!!.setOnClickListener { tabFigureListener.onFigureSelected(LINE) }
        initializeFigureTabLayout()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun initializeFigureTabLayout() {
        binding!!.figureTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tabFigureListener.onFigureSelected(tab!!.position)
            }

        })
    }

    interface TabFigureListener {
        fun onFigureSelected(currentFigure: Int)
    }
}