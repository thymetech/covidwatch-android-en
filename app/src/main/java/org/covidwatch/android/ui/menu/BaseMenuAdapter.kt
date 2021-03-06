package org.covidwatch.android.ui.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.covidwatch.android.R

open class BaseMenuAdapter(
    private val onClick: (menuItem: MenuItem) -> Unit
) : RecyclerView.Adapter<MenuItemViewHolder>() {

    private val possibleExposuresMenuItem = MenuItem(
        R.string.menu_possible_exposures,
        0,
        PossibleExposures
    )

    private val possibleExposuresHighRiskMenuItem = MenuItem(
        R.string.menu_possible_exposures,
        R.drawable.ic_info_red,
        PossibleExposures
    )

    protected val items = mutableListOf(
        MenuItem(
            R.string.menu_reset_possible_exposures,
            0,
            Destination.None
        ),
        MenuItem(
            R.string.menu_detect_exposures_from_server,
            0,
            Destination.None
        ),
        MenuItem(
            R.string.menu_export_possible_exposures,
            0,
            Destination.None
        ),
        possibleExposuresMenuItem,
        MenuItem(R.string.menu_notify_others, 0, NotifyOthers),
        MenuItem(R.string.menu_how_it_works, 0, HowItWorks),
        MenuItem(
            R.string.menu_health_guidelines,
            R.drawable.ic_exit_to_app,
            Browser(R.string.health_guidelines)
        ),
        MenuItem(
            R.string.menu_covid_watch_website,
            R.drawable.ic_exit_to_app,
            Browser(R.string.covidwatch_website)
        ),
        MenuItem(
            R.string.menu_faq,
            R.drawable.ic_exit_to_app,
            Browser(R.string.faq)
        ),
        MenuItem(
            R.string.menu_terms_of_use,
            R.drawable.ic_exit_to_app,
            Browser(R.string.terms)
        ),
        MenuItem(
            R.string.menu_support,
            R.drawable.ic_exit_to_app,
            Browser(R.string.support)
        )
    )

    fun showHighRiskPossibleExposures() {
        replace(possibleExposuresMenuItem, possibleExposuresHighRiskMenuItem)
    }

    fun showNoRiskPossibleExposures() {
        replace(possibleExposuresHighRiskMenuItem, possibleExposuresMenuItem)
    }

    private fun replace(from: MenuItem, to: MenuItem) {
        items.indexOf(from).takeIf { it != -1 }?.let { index ->
            items[index] = to
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val root = LayoutInflater.from(parent.context).inflate(R.layout.item_menu, parent, false)
        return MenuItemViewHolder(root)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        val menuItem = items[position]
        holder.bind(menuItem)

        holder.itemView.setOnClickListener { onClick(menuItem) }
    }
}