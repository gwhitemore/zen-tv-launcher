package com.gwhitemore.zentv

import android.content.Context
import android.graphics.Color
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.gwhitemore.zentv.databinding.ItemAppBinding

class AppAdapter(
    private var apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo) -> Unit,
    private val onAppFocus: ((AppInfo, Int) -> Unit)? = null
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private val springInterpolator = DecelerateInterpolator(1.8f)
    var focusedPosition: Int = 0
        private set

    var reorderingPackage: String? = null
        private set

    fun getApps(): List<AppInfo> = apps

    fun setReorderingApp(pkg: String?) {
        reorderingPackage = pkg
        notifyDataSetChanged()
    }

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

            // Move indicator badge
            binding.tvReorderBadge.visibility = if (reorderingPackage == app.packageName) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            // TV Remote Click & Long Press
            binding.cardContainer.setOnClickListener {
                onAppClick(app)
            }

            binding.cardContainer.setOnLongClickListener {
                onAppLongClick(app)
                true
            }

            // Apple TV tvOS Style Focus, Ambient Glow & Momentum Centering
            binding.cardContainer.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    focusedPosition = position
                    binding.tvAppTitle.isSelected = true
                    binding.tvAppTitle.setTextColor(Color.parseColor("#FFFFFF"))
                    binding.tvAppTitle.animate().scaleX(1.08f).scaleY(1.08f).setDuration(160).start()

                    view.animate()
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .translationZ(20f)
                        .setDuration(180)
                        .setInterpolator(springInterpolator)
                        .start()

                    onAppFocus?.invoke(app, position)

                    // Glides the focused app card smoothly to the center of the screen
                    val rv = view.parent?.parent as? RecyclerView ?: view.parent as? RecyclerView
                    rv?.let { recyclerView ->
                        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                        layoutManager?.let { lm ->
                            val scroller = CenterSmoothScroller(view.context)
                            scroller.targetPosition = position
                            lm.startSmoothScroll(scroller)
                        }
                    }
                } else {
                    binding.tvAppTitle.isSelected = false
                    binding.tvAppTitle.setTextColor(Color.parseColor("#B3FFFFFF"))
                    binding.tvAppTitle.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start()

                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationZ(0f)
                        .setDuration(160)
                        .setInterpolator(springInterpolator)
                        .start()
                }
            }
        }
    }

    // Apple TV Weighted Center Scroller
    class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {
        override fun calculateDtToFit(
            viewStart: Int,
            viewEnd: Int,
            boxStart: Int,
            boxEnd: Int,
            snapPreference: Int
        ): Int {
            return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
        }

        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
            // Smooth Apple TV inertia glide speed
            return 0.18f
        }
    }
}
