package com.minar.birday.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.edit
import androidx.core.view.doOnPreDraw
import androidx.core.widget.addTextChangedListener
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.adapters.EventAdapter
import com.minar.birday.databinding.FragmentHomeBinding
import com.minar.birday.model.EventDataItem
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.addInsetsByPadding
import com.minar.birday.viewmodels.MainViewModel

class HomeFragment : Fragment() {
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: EventAdapter
    private lateinit var act: MainActivity
    private lateinit var sharedPrefs: SharedPreferences
    private var currentEvents: List<EventResult> = emptyList()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        act = activity as MainActivity
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        adapter = EventAdapter(onItemClick = ::onItemClick)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()

        val recycler = binding.eventRecycler
        val search = binding.homeSearch
        recycler.adapter = adapter
        recycler.addInsetsByPadding(bottom = true)

        search.addTextChangedListener { text ->
            mainViewModel.searchStringChanged(text?.toString().orEmpty())
        }
        binding.homeSortButton.setOnClickListener(::showSortMenu)

        mainViewModel.allEvents.observe(viewLifecycleOwner) { events ->
            currentEvents = events
            submitCurrentEvents()
            binding.noEvents.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
            recycler.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
            recycler.doOnPreDraw { startPostponedEnterTransition() }
        }

        val restoredSearch = mainViewModel.searchString.value.orEmpty()
        if (restoredSearch.isNotBlank()) search.setText(restoredSearch)
    }

    private fun showSortMenu(anchor: View) {
        val orderAlphabetically = sharedPrefs.getBoolean("order_alphabetically", false)
        PopupMenu(requireContext(), anchor).apply {
            menu.add(0, SORT_BY_DATE, 0, R.string.sort_by_date).isCheckable = true
            menu.add(0, SORT_BY_NAME, 1, R.string.sort_by_name).isCheckable = true
            menu.findItem(if (orderAlphabetically) SORT_BY_NAME else SORT_BY_DATE).isChecked = true
            setOnMenuItemClickListener { item ->
                val alphabetical = item.itemId == SORT_BY_NAME
                sharedPrefs.edit { putBoolean("order_alphabetically", alphabetical) }
                submitCurrentEvents()
                true
            }
            show()
        }
    }

    private fun submitCurrentEvents() {
        adapter.prepareAndSubmitList(
            currentEvents,
            orderAlphabetically = sharedPrefs.getBoolean("order_alphabetically", false),
            surnameFirst = sharedPrefs.getBoolean("surname_first", false)
        )
    }

    private fun onItemClick(position: Int) {
        if (findNavController().currentDestination?.label != "fragment_home") return
        val item = adapter.getItem(position)
        if (item !is EventDataItem.EventItem) return

        act.vibrate()
        val viewHolder = binding.eventRecycler.findViewHolderForAdapterPosition(position) ?: return
        val fullView = viewHolder.itemView
        val image = fullView.findViewById<ImageView>(R.id.eventImage)
        val action = HomeFragmentDirections.actionNavigationMainToDetailsFragment(
            item.eventResult,
            position
        )
        val extras: FragmentNavigator.Extras =
            if (sharedPrefs.getBoolean("hide_images", false)) {
                FragmentNavigatorExtras(fullView to "shared_full_view$position")
            } else {
                FragmentNavigatorExtras(image to "shared_image$position")
            }
        findNavController().navigate(action, extras)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val SORT_BY_DATE = 1
        const val SORT_BY_NAME = 2
    }
}
