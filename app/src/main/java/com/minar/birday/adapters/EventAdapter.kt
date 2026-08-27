package com.minar.birday.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.minar.birday.R
import com.minar.birday.databinding.EventRowBinding
import com.minar.birday.databinding.MonthHeaderRowBinding
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventDataItem
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.formatDaysRemaining
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.getNextYears
import com.minar.birday.utilities.getRemainingDays
import com.minar.birday.utilities.setEventImageOrPlaceholder
import com.minar.birday.utilities.zodiacSignFor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_EVENT = 1

enum class EventSortOrder {
    DATE,
    NAME,
    AGE,
    ZODIAC
}

class EventAdapter(
    private val context: Context,
    private val onItemClick: (position: Int) -> Unit
) : ListAdapter<EventDataItem, RecyclerView.ViewHolder>(EventsDiffCallback()) {
    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is EventDataItem.IndexHeader -> ITEM_VIEW_TYPE_HEADER
        is EventDataItem.EventItem -> ITEM_VIEW_TYPE_EVENT
    }

    fun prepareAndSubmitList(
        list: List<EventResult>?,
        sortOrder: EventSortOrder = EventSortOrder.DATE,
        reversed: Boolean = false,
        surnameFirst: Boolean = false,
        onCommitted: (() -> Unit)? = null
    ) {
        if (list.isNullOrEmpty()) {
            submitList(emptyList()) { onCommitted?.invoke() }
            return
        }

        val organizedEvents = mutableListOf<EventDataItem>()
        val sortedEvents = when (sortOrder) {
            EventSortOrder.DATE -> list
            EventSortOrder.NAME -> if (surnameFirst) {
                list.sortedWith(compareBy({ it.surname.orEmpty() }, { it.name }))
            } else {
                list.sortedWith(compareBy({ it.name }, { it.surname.orEmpty() }))
            }
            EventSortOrder.AGE -> list.sortedWith(
                compareBy<EventResult>(
                    { it.yearMatter != true },
                    { if (it.yearMatter == true) it.originalDate.year else Int.MAX_VALUE },
                    { it.originalDate.monthValue },
                    { it.originalDate.dayOfMonth },
                    { formatName(it, surnameFirst) }
                )
            )
            EventSortOrder.ZODIAC -> list.sortedWith(
                compareBy<EventResult>(
                    { zodiacSignFor(it.originalDate).ordinal },
                    { it.nextDate ?: LocalDate.MAX },
                    { formatName(it, surnameFirst) }
                )
            )
        }.let { if (reversed) it.reversed() else it }

        if (sortOrder == EventSortOrder.NAME) {
            var previousLetter: String? = null
            sortedEvents.forEach { event ->
                val displayedName = formatName(event, surnameFirst)
                val letter = displayedName.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                if (letter != previousLetter) {
                    organizedEvents.add(EventDataItem.IndexHeader(letter))
                    previousLetter = letter
                }
                organizedEvents.add(EventDataItem.EventItem(event))
            }
        } else if (sortOrder == EventSortOrder.DATE) {
            var previousMonth: String? = null
            sortedEvents.forEach { event ->
                val nextDate = event.nextDate ?: return@forEach
                val month = nextDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                val header = "$month ${nextDate.year}"
                if (header != previousMonth) {
                    organizedEvents.add(EventDataItem.IndexHeader(header))
                    previousMonth = header
                }
                organizedEvents.add(EventDataItem.EventItem(event))
            }
        } else if (sortOrder == EventSortOrder.AGE) {
            organizedEvents.addAll(sortedEvents.map(EventDataItem::EventItem))
        } else {
            var previousSign = -1
            sortedEvents.forEach { event ->
                val sign = zodiacSignFor(event.originalDate)
                if (sign.ordinal != previousSign) {
                    organizedEvents.add(
                        EventDataItem.IndexHeader(
                            headerTitle = context.getString(sign.labelRes),
                            iconRes = sign.iconRes
                        )
                    )
                    previousSign = sign.ordinal
                }
                organizedEvents.add(EventDataItem.EventItem(event))
            }
        }
        submitList(organizedEvents) { onCommitted?.invoke() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_EVENT -> EventViewHolder(
                EventRowBinding.inflate(LayoutInflater.from(context), parent, false)
            )
            ITEM_VIEW_TYPE_HEADER -> IndexHeaderViewHolder(
                MonthHeaderRowBinding.inflate(LayoutInflater.from(context), parent, false)
            )
            else -> error("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is IndexHeaderViewHolder -> holder.bind(getItem(position) as EventDataItem.IndexHeader)
            is EventViewHolder -> holder.bind(getItem(position) as EventDataItem.EventItem)
        }
    }

    public override fun getItem(position: Int): EventDataItem = super.getItem(position)

    inner class IndexHeaderViewHolder(binding: MonthHeaderRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val monthHeaderText = binding.eventDateHeader
        private val headerIcon = binding.eventHeaderIcon

        fun bind(indexHeader: EventDataItem.IndexHeader) {
            monthHeaderText.text = indexHeader.headerTitle
            headerIcon.visibility = if (indexHeader.iconRes == null) View.GONE else View.VISIBLE
            indexHeader.iconRes?.let(headerIcon::setImageResource)
        }
    }

    inner class EventViewHolder(private val binding: EventRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onItemClick(position)
            }
        }

        fun bind(eventItem: EventDataItem.EventItem) = with(binding) {
            val event = eventItem.eventResult
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val personName = formatName(event, sharedPrefs.getBoolean("surname_first", false))
            val dateText = if (event.yearMatter == true) {
                event.originalDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
            } else {
                event.originalDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
            }.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            val countdownText = event.nextDate?.let {
                formatDaysRemaining(getRemainingDays(it), context)
            }.orEmpty()
            val nextAge = if (event.yearMatter == true) getNextYears(event) else 0
            val showAgeProgression =
                event.yearMatter == true && event.type != EventCode.NAME_DAY.name && nextAge > 0

            eventPerson.text = personName
            eventDate.text = dateText
            eventAgeProgression.visibility = if (showAgeProgression) View.VISIBLE else View.GONE
            if (showAgeProgression) {
                eventAgeCurrent.text = context.getString(
                    R.string.age_progression_current,
                    nextAge - 1
                )
                eventAgeNext.text = context.resources.getQuantityString(
                    R.plurals.years,
                    nextAge,
                    nextAge
                )
            }
            eventCountdown.text = countdownText
            val dateDescription = if (showAgeProgression) {
                context.getString(
                    R.string.event_date_with_age_description,
                    dateText,
                    nextAge - 1,
                    nextAge
                )
            } else {
                dateText
            }
            root.contentDescription = context.getString(
                R.string.event_row_description,
                personName,
                dateDescription,
                countdownText
            )

            val daysRemaining = event.nextDate?.let(::getRemainingDays) ?: Int.MAX_VALUE
            eventCountdown.setTextColor(
                MaterialColors.getColor(
                    eventCountdown,
                    if (daysRemaining <= 1) {
                        R.attr.colorPrimary
                    } else {
                        R.attr.colorOnSurfaceVariant
                    }
                )
            )

            val hideImages = sharedPrefs.getBoolean("hide_images", false)
            if (hideImages) {
                ViewCompat.setTransitionName(root, "shared_full_view$bindingAdapterPosition")
                eventImage.visibility = View.GONE
            } else {
                ViewCompat.setTransitionName(eventImage, "shared_image$bindingAdapterPosition")
                eventImage.visibility = View.VISIBLE
                setEventImageOrPlaceholder(event, eventImage)
            }
        }
    }
}

class EventsDiffCallback : DiffUtil.ItemCallback<EventDataItem>() {
    override fun areItemsTheSame(oldItem: EventDataItem, newItem: EventDataItem): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: EventDataItem, newItem: EventDataItem): Boolean =
        oldItem == newItem
}
