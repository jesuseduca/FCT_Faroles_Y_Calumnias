package com.example.fct_faroles_y_calumnias_app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.fct_faroles_y_calumnias_app.R

class InfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCerrar = view.findViewById<Button>(R.id.btnCerrarInfo)

        btnCerrar.setOnClickListener {
            requireActivity().findViewById<androidx.fragment.app.FragmentContainerView>(R.id.nav_host_fragment)
                .visibility = View.GONE
            parentFragmentManager.popBackStack()
        }
    }
}