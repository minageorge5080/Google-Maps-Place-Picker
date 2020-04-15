package com.minageorge.placepicker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.minageorge.placepicker.data.Result
import org.w3c.dom.Text

class NearByPlacesAdapter : RecyclerView.Adapter<NearByPlacesAdapter.VH>() {

    private val results: MutableList<Result> = ArrayList()
    var listener: OnLocationClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_nearby_location, parent, false)
        )
    }

    override fun getItemCount() = results.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(results[position])
    }

    fun pushData(newResults: List<Result>) {
        this.results.clear()
        this.results.addAll(newResults)
        notifyDataSetChanged()
    }

    inner class VH(private val view: View) : RecyclerView.ViewHolder(view) {

        private lateinit var result: Result

        init {
            itemView.setOnClickListener { listener?.onClick(result) }
        }

        fun bind(result: Result) {
            this.result = result
            view.findViewById<TextView>(R.id.title).text = result.name
            view.findViewById<TextView>(R.id.sub_title).text = result.vicinity

        }
    }

    interface OnLocationClickListener {

        fun onClick(result: Result)
    }
}