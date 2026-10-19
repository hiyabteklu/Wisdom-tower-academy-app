package com.wisdomtower.academy.data.admin

// ADMIN-native N8b. Kotlin port of website-reference/src/lib/notification-paths.ts: the cascading picker data for
// "where should the student land?" (category New material release). It is built FROM ADMIN_CONTENT_TREE
// (AdminContentTree.kt), so it only offers subjects and hubs the Course Content screen also knows.
// Public path shapes (read in the website file, and the app's own route code uses the same slug):
//   grade     /academy/grades/{grade}/{subject}[/{hub}]
//   freshman  /academy/freshman/{subject}[/{hub}]
//   ECE       /academy/special-packages/electrical-computer-engineering/{sem-1|sem-2}/{course}[/{hub}]
//   entrance  /academy/entrance-questions/{stream}/{subject}/{year}  (the year page IS the exams hub: nothing below it)
// HYPOTHESIS: the single ECE package slug is electrical-computer-engineering (seen in PackagesData.kt and
// HomeScreen.kt of this app; the website file looks the slug up by semester).
// STATUS: `[~]` written, NOT compiled, NOT tested on a device.
// Note: do not write a slash followed by a star inside any comment, Kotlin nests comments.

/** One step of the picker. path is missing on pure grouping nodes (for example Grades 9-12). */
class PathNode(val id: String, val label: String, val path: String?, val children: List<PathNode>)

object NotificationPaths {
    private const val ECE_SLUG = "electrical-computer-engineering"

    private fun publicPathForScope(scopePath: String): String? {
        val p = scopePath.split("/").filter { it.isNotEmpty() }
        if (p.size == 3 && p[0] == "grade") return "/academy/grades/${p[1]}/${p[2]}"
        if (p.size == 2 && p[0] == "freshman") return "/academy/freshman/${p[1]}"
        if (p.size == 2 && p[0] == "remedial") return "/academy/remedial/${p[1]}"
        if (p.size == 3 && p[0] == "ece") return "/academy/special-packages/$ECE_SLUG/${p[1]}/${p[2]}"
        if (p.size == 4 && p[0] == "entrance") return "/academy/entrance-questions/${p[1]}/${p[2]}/${p[3]}"
        return null
    }

    private fun convert(node: AdminNavNode, parentScope: String?, parentPath: String?): PathNode? {
        var path: String? = null
        val scope = node.scopePath
        if (scope != null) {
            if (parentPath != null && parentScope != null && scope == parentScope) {
                // A hub node sits under its course with the same scope path. Entrance year pages ARE the exams hub.
                if (parentScope.startsWith("entrance/")) return null
                path = parentPath + "/" + node.id
            } else {
                path = publicPathForScope(scope)
            }
        }
        val children = ArrayList<PathNode>()
        for (child in node.children) {
            val converted = convert(child, node.scopePath, path)
            if (converted != null) children.add(converted)
        }
        if (path == null && children.isEmpty()) return null
        return PathNode(node.id, node.label, path, children)
    }

    /** The cascading picker data: package group, subject or course, hub. */
    val tree: List<PathNode> by lazy {
        val out = ArrayList<PathNode>()
        for (node in ADMIN_CONTENT_TREE) {
            val converted = convert(node, null, null)
            if (converted != null) out.add(converted)
        }
        out
    }

    /** The choices below the nodes picked so far (empty list when nothing is deeper). */
    fun nodesAfter(trail: List<String>): List<PathNode> {
        var level = tree
        for (id in trail) {
            val node = level.firstOrNull { it.id == id } ?: return emptyList()
            level = node.children
        }
        return level
    }

    /** The deepest picked node that has a public path, or null. */
    fun pathFromTrail(trail: List<String>): String? {
        var level = tree
        var path: String? = null
        for (id in trail) {
            val node = level.firstOrNull { it.id == id } ?: break
            if (node.path != null) path = node.path
            level = node.children
        }
        return path
    }

    /** Labels from the first pick down to the deepest picked node that has a path (for the "this opens" line). */
    fun crumbsFor(trail: List<String>): List<String> {
        var level = tree
        val labels = ArrayList<String>()
        var lastWithPath = -1
        for (id in trail) {
            val node = level.firstOrNull { it.id == id } ?: break
            labels.add(node.label)
            if (node.path != null) lastWithPath = labels.size - 1
            level = node.children
        }
        return if (lastWithPath < 0) emptyList() else labels.take(lastWithPath + 1)
    }

    private fun findTrail(nodes: List<PathNode>, path: String, trail: List<String>): List<String>? {
        for (node in nodes) {
            val next = trail + node.id
            if (node.path == path) return next
            val found = findTrail(node.children, path, next)
            if (found != null) return found
        }
        return null
    }

    /** Picker trail (node ids) that ends on the node with this public path, or null. */
    fun trailForPath(path: String): List<String>? = findTrail(tree, path, emptyList())
}
