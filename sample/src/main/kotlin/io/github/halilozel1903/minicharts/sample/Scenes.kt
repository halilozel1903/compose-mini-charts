package io.github.halilozel1903.minicharts.sample

/** The sample's screens, also used as screenshot scenes through the `scene` intent extra. */
enum class Scene(val key: String, val title: String) {
    Dashboard("dashboard", "Dashboard"),
    Line("line", "Line"),
    Bars("bars", "Bars"),
    Donut("donut", "Donut"),
    ;

    companion object {
        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key }
    }
}
