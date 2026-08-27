package com.minar.birday.model

// A wrapper around EventResult, to also consider the month header as an object
sealed class EventDataItem {
    data class EventItem(val eventResult: EventResult) : EventDataItem() {
        override val id = eventResult.id.toLong()
    }

    data class IndexHeader(
        val headerTitle: String,
        val iconRes: Int? = null
    ) : EventDataItem() {
        override val id = Long.MIN_VALUE + (headerTitle.hashCode().toLong() and 0xffffffffL)
    }

    abstract val id: Long
}
