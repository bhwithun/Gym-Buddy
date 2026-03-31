package com.gymbuddy

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.RadioButton
import android.widget.TextView
import com.gymbuddy.databinding.ItemMakeupDayBinding

class MakeupDayAdapter(
    private val context: Context,
    private val dayNames: Array<String>,
    private val isRestDay: BooleanArray
) : BaseAdapter() {

    private var selectedPosition = -1

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int = selectedPosition

    override fun getCount(): Int = dayNames.size

    override fun getItem(position: Int): Any = dayNames[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: ItemMakeupDayBinding
        val view: View

        if (convertView == null) {
            binding = ItemMakeupDayBinding.inflate(LayoutInflater.from(context), parent, false)
            view = binding.root
            view.tag = binding
        } else {
            binding = convertView.tag as ItemMakeupDayBinding
            view = convertView
        }

        binding.dayText.text = dayNames[position]

        if (isRestDay[position]) {
            // Rest day - hide radio button
            binding.radioButton.visibility = View.GONE
        } else {
            // Workout day - show radio button
            binding.radioButton.visibility = View.VISIBLE
            binding.radioButton.isChecked = (position == selectedPosition)
        }

        return view
    }
}