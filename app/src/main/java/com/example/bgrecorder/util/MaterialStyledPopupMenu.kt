import android.content.Context
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import com.example.bgrecorder.R

class MaterialStyledPopupMenu(
    context: Context,
    anchor: View,
    menuResId: Int,
    private val onItemSelected: (MenuItem) -> Boolean,
    private val iconMarginDp: Int = 4,
) {

    private val popupMenu: PopupMenu = PopupMenu(context, anchor, Gravity.END, 0, R.style.MaterialPopupMenu)

    init {
        val inflater: MenuInflater = popupMenu.menuInflater
        inflater.inflate(menuResId, popupMenu.menu)

        styleIcons(context)

        popupMenu.setOnMenuItemClickListener { item ->
            onItemSelected(item)
        }
    }

    private fun styleIcons(context: Context) {
        try {
            val menu = popupMenu.menu

            val method = menu.javaClass.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(menu, true)

            val iconMarginPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, iconMarginDp.toFloat(), context.resources.displayMetrics
            ).toInt()

            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                val icon = item.icon ?: continue

                item.icon = InsetDrawable(icon, iconMarginPx, 0, iconMarginPx, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun show() {
        popupMenu.show()
    }
}
