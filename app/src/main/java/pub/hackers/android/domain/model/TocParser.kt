package pub.hackers.android.domain.model

private const val MAX_TOC_DEPTH = 16

fun parseTocJson(value: Any?): List<TocItem> = parseTocJson(value, depth = 0)

private fun parseTocJson(value: Any?, depth: Int): List<TocItem> {
    if (depth >= MAX_TOC_DEPTH) return emptyList()
    val list = value as? List<*> ?: return emptyList()
    return list.mapNotNull { parseTocItem(it, depth) }
}

private fun parseTocItem(value: Any?, depth: Int): TocItem? {
    val map = value as? Map<*, *> ?: return null
    val id = map["id"] as? String ?: return null
    val title = map["title"] as? String ?: return null
    val level = (map["level"] as? Number)?.toInt() ?: return null
    val children = parseTocJson(map["children"], depth + 1)
    return TocItem(id = id, level = level, title = title, children = children)
}
