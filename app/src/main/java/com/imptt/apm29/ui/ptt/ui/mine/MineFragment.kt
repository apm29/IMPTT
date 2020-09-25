package com.imptt.apm29.ui.ptt.ui.mine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.imptt.apm29.R

class MineFragment : Fragment() {

    private lateinit var mineViewModel: MineViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mineViewModel =
            ViewModelProviders.of(this).get(MineViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_mine, container, false)
        val textView: TextView = root.findViewById(R.id.text_mine)
        mineViewModel.text.observe(viewLifecycleOwner, {
            textView.text = it
        })
        return root
    }
}