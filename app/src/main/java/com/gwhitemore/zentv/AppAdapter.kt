package com.gwhitemore.zentv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.gwhitemore.zentv.databinding.ItemAppBinding

class AppAdapter(
    private var apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private val interpolator = DecelerateInterpolator(1.5f)
    var focusedPosition: Int = 0
        private set

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app, position)
    }

    override fun getItemCount(): Int = apps.size

    inner class AppViewHolder(val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo, position: Int) {
            binding.tvAppTitle.text = app.label
            if (app.icon != null) {
                binding.ivAppIcon.setImageDrawable(app.icon)
            } else {
                binding.ivAppIcon.setImageResource(R.drawable.banner)
            }

            // TV Remote Click & Long Press
            binding.cardContainer.setOnClickListener {
                onAppClick(app)
            }

            binding.cardContainer.setOnLongClickListener {
                onAppLongClick(app)
                true
            }

            // Remote Focus Animation
            binding.cardContainer.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    focusedPosition = position
                    binding.tvAppTitle.isSelected = true
                    view.animate()
                        .scaleX(1.14f)
                        .scaleY(1.14f)
                        .translationZ(12f)
                        .setDuration(160)
                        .setInterpolator(interpolator)
                        .start()
                } else {
                    binding.tvAppTitle.isSelected = false
                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationZ(0f)
                        .setDuration(160)
                        .setInterpolator(interpolator)
                        .start()
                }
            }
        }
    }
}
