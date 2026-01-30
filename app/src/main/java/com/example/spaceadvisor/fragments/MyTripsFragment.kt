package com.example.spaceadvisor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spaceadvisor.R
import com.example.spaceadvisor.adapters.MyTripsAdapter
import com.example.spaceadvisor.databinding.FragmentMyTripsBinding
import com.example.spaceadvisor.viewmodels.TripViewModel
import com.example.spaceadvisor.viewmodels.UserViewModel

class MyTripsFragment : Fragment() {

    private var _binding: FragmentMyTripsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MyTripsAdapter
    private lateinit var userViewModel: UserViewModel
    private lateinit var tripViewModel: TripViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
        tripViewModel = ViewModelProvider(requireActivity())[TripViewModel::class.java]

        setupRecyclerView()
        observeViewModel()

        userViewModel.getCurrentUid()?.let { uid ->
            tripViewModel.fetchUserTrips(uid)
        }
    }

    private fun setupRecyclerView() {
        adapter = MyTripsAdapter(mutableListOf()) { trip ->
            tripViewModel.setCurrentTrip(trip)
            navigateToMyTrip()
        }
        binding.myTripsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.myTripsRecyclerView.adapter = adapter
    }

    private fun navigateToMyTrip() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frame, TripEditorFragment())
            .addToBackStack(null)
            .commit()

        activity?.findViewById<TextView>(R.id.header_title)?.text = "My Trip"
    }

    private fun observeViewModel() {
        tripViewModel.userTrips.observe(viewLifecycleOwner) { trips ->
            adapter.updateData(trips)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
