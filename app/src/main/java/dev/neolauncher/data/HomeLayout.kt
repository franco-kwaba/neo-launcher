package dev.neolauncher.data

import androidx.compose.runtime.Immutable

/** Onde um app pode estar (ou ser solto ao arrastar). */
sealed interface AppSlot {
    data object Drawer : AppSlot
    data class Home(val cell: Int) : AppSlot
    data class Dock(val index: Int) : AppSlot
    data object Remove : AppSlot
}

/**
 * Posição dos apps na tela inicial (grade, célula -> app) e na barra de atalhos (lista ordenada).
 * Cada app fica em um lugar só.
 */
@Immutable
data class HomeLayout(
    val dock: List<String> = emptyList(),
    val home: Map<Int, String> = emptyMap(),
) {
    fun slotOf(key: String): AppSlot =
        dock.indexOf(key).takeIf { it >= 0 }?.let { AppSlot.Dock(it) }
            ?: home.entries.firstOrNull { it.value == key }?.let { AppSlot.Home(it.key) }
            ?: AppSlot.Drawer

    fun firstFreeCell(cellCount: Int): Int? = (0 until cellCount).firstOrNull { it !in home }

    /**
     * Move o app para [to]. Se o destino estiver ocupado, o app que estava lá troca de lugar
     * com o que chegou (ou vai para a primeira célula livre, quando veio da gaveta).
     */
    fun move(key: String, to: AppSlot, cellCount: Int): HomeLayout {
        val from = slotOf(key)
        if (from == to) return this
        val dock = dock.toMutableList().apply { remove(key) }
        val home = home.filterValues { it != key }.toMutableMap()

        fun rehome(displaced: String) {
            if (from is AppSlot.Home) {
                home[from.cell] = displaced
            } else {
                (0 until cellCount).firstOrNull { it !in home }?.let { home[it] = displaced }
            }
        }

        when (to) {
            AppSlot.Drawer, AppSlot.Remove -> Unit
            is AppSlot.Home -> {
                val occupant = home.put(to.cell, key)
                if (occupant != null) {
                    if (from is AppSlot.Dock) dock.add(from.index.coerceAtMost(dock.size), occupant) else rehome(occupant)
                }
            }
            is AppSlot.Dock -> {
                if (dock.size < MAX_DOCK) {
                    dock.add(to.index.coerceIn(0, dock.size), key)
                } else {
                    rehome(dock.set(to.index.coerceIn(0, dock.lastIndex), key))
                }
            }
        }
        return HomeLayout(dock, home)
    }

    companion object {
        const val MAX_DOCK = 4
    }
}
