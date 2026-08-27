package com.minar.birday.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.minar.birday.adapters.EventSortOrder
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
        adapter = EventAdapter(requireContext(), onItemClick = ::onItemClick)
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
            binding.homeSearchCloseButton.contentDescription = getString(
                if (text.isNullOrEmpty()) R.string.close_search else R.string.clear_search
            )
        }
        binding.homeSearchButton.setOnClickListener {
            setSearchVisible(true)
        }
        binding.homeSearchCloseButton.setOnClickListener {
            if (search.text.isNullOrEmpty()) {
                setSearchVisible(false)
            } else {
                search.setText("")
            }
        }
        binding.homeSortButton.setOnClickListener(::showSortMenu)
        binding.homeSettingsButton.setOnClickListener {
            if (findNavController().currentDestination?.label == "fragment_home") {
                findNavController().navigate(R.id.navigationSettings)
            }
        }

        mainViewModel.allEvents.observe(viewLifecycleOwner) { events ->
            currentEvents = events
            submitCurrentEvents()
            binding.noEvents.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
            recycler.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
            recycler.doOnPreDraw { startPostponedEnterTransition() }
        }

        val restoredSearch = mainViewModel.searchString.value.orEmpty()
        if (restoredSearch.isNotBlank()) {
            search.setText(restoredSearch)
            setSearchVisible(visible = true, requestFocus = false)
        }
    }

    private fun setSearchVisible(visible: Boolean, requestFocus: Boolean = true) {
        val search = binding.homeSearch
        val inputMethodManager = requireContext().getSystemService(InputMethodManager::class.java)

        val normalVisibility = if (visible) View.INVISIBLE else View.VISIBLE
        binding.homeTitle.visibility = normalVisibility
        binding.homeSearchButton.visibility = normalVisibility
        binding.homeSortButton.visibility = normalVisibility
        binding.homeSettingsButton.visibility = normalVisibility
        binding.homeSearch.visibility = if (visible) View.VISIBLE else View.GONE
        binding.homeSearchCloseButton.visibility = if (visible) View.VISIBLE else View.GONE

        if (visible && requestFocus) {
            search.requestFocus()
            search.post {
                inputMethodManager?.showSoftInput(search, InputMethodManager.SHOW_IMPLICIT)
            }
        } else if (!visible) {
            search.setText("")
            search.clearFocus()
            inputMethodManager?.hideSoftInputFromWindow(search.windowToken, 0)
        }
    }

    private fun showSortMenu(anchor: View) {
        val sortOrder = getSortOrder()
        val reversed = sharedPrefs.getBoolean(PREF_SORT_REVERSED, false)
        PopupMenu(requireContext(), anchor).apply {
            menu.add(SORT_GROUP, SORT_BY_DATE, 0, R.string.sort_by_date).isCheckable = true
            menu.add(SORT_GROUP, SORT_BY_NAME, 1, R.string.sort_by_name).isCheckable = true
            menu.add(SORT_GROUP, SORT_BY_AGE, 2, R.string.sort_by_age).isCheckable = true
            menu.add(SORT_GROUP, SORT_BY_ZODIAC, 3, R.string.sort_by_zodiac).isCheckable = true
            menu.setGroupCheckable(SORT_GROUP, true, true)
            menu.add(0, SORT_REVERSED, 4, R.string.sort_reversed).apply {
                isCheckable = true
                isChecked = reversed
            }
            menu.findItem(
                when (sortOrder) {
                    EventSortOrder.DATE -> SORT_BY_DATE
                    EventSortOrder.NAME -> SORT_BY_NAME
                    EventSortOrder.AGE -> SORT_BY_AGE
                    EventSortOrder.ZODIAC -> SORT_BY_ZODIAC
                }
            ).isChecked = true
            setOnMenuItemClickListener { item ->
                sharedPrefs.edit {
                    when (item.itemId) {
                        SORT_BY_DATE -> putString(PREF_SORT_ORDER, EventSortOrder.DATE.name)
                        SORT_BY_NAME -> putString(PREF_SORT_ORDER, EventSortOrder.NAME.name)
                        SORT_BY_AGE -> putString(PREF_SORT_ORDER, EventSortOrder.AGE.name)
                        SORT_BY_ZODIAC -> putString(PREF_SORT_ORDER, EventSortOrder.ZODIAC.name)
                        SORT_REVERSED -> putBoolean(PREF_SORT_REVERSED, !reversed)
                    }
                }
                submitCurrentEvents(scrollToTop = true)
                true
            }
            show()
        }
    }

    private fun submitCurrentEvents(scrollToTop: Boolean = false) {
        adapter.prepareAndSubmitList(
            currentEvents,
            sortOrder = getSortOrder(),
            reversed = sharedPrefs.getBoolean(PREF_SORT_REVERSED, false),
            surnameFirst = sharedPrefs.getBoolean("surname_first", false),
            onCommitted = if (scrollToTop) {
                { _binding?.eventRecycler?.scrollToPosition(0) }
            } else {
                null
            }
        )
    }

    private fun getSortOrder(): EventSortOrder {
        val savedOrder = sharedPrefs.getString(PREF_SORT_ORDER, null)
        return savedOrder?.let {
            runCatching { EventSortOrder.valueOf(it) }.getOrNull()
        } ?: if (sharedPrefs.getBoolean("order_alphabetically", false)) {
            EventSortOrder.NAME
        } else {
            EventSortOrder.DATE
        }
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
        const val SORT_GROUP = 1
        const val SORT_BY_DATE = 1
        const val SORT_BY_NAME = 2
        const val SORT_BY_AGE = 3
        const val SORT_BY_ZODIAC = 4
        const val SORT_REVERSED = 5
        const val PREF_SORT_ORDER = "home_sort_order"
        const val PREF_SORT_REVERSED = "home_sort_reversed"
    }
}
