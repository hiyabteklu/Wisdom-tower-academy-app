# ECE Year 1/2 content: alias map (E2c-c, 2026-11-23)

Structure only: no keys, no student data. Source: read-only `select` on `public.learning_resources` (live project, nothing changed) plus `LearningContentRepository.kt`.

## How a hub list picks its materials (verified in code)
- `LearningContentRepository.list(context, scopePath, hub)` asks `learning_resources_public` for rows with `scope_path` = the given scope (exact match, `scopeFilter`), `hub` = the hub, `published = true`, ordered by `sort_order`. Same for `getHubCounts` and the offline `catalogRows`.
- Only two aliases exist today: (1) regex `^ece/(sem-[12])/([^/]+)$` (an ECE course is looked up under BOTH `ece/sem-1/<slug>` and `ece/sem-2/<slug>`, then an `ilike *<slug>*` fallback); (2) `freshman/math-natural` = `freshman/mathematics`.
- A scope like `ece/y1-sem-1/<slug>` matches NEITHER alias and no row, so Year 1/2 hubs would be EMPTY until an alias or rows exist.
- `ResourceScopeIndex.kt` and `ResourceCatalogIndex.kt` only serve the status counts (any `ece*` row counts as picker `ece`).

## Live `scope_path` values (checked 2026-11-23)
- `ece/sem-1/<slug>` only (Year 3, package_id `ece-y3-sem-1`): eceg3051, eceg3052, eceg3061, eceg3071, eceg3081, eceg3082, eceg3092, eceg3094, eceg3096, eceg3101, eceg3102, econ1011, meng3052. NO row exists for any Year 1 or Year 2 ECE course code (so `EceTreeData.kt` comment "eceg1052 is already used by its published content" is wrong: no `eceg1052` rows).
- `freshman/<slug>` (package_id `freshman`, all published): anthropology, applied-math-1, biology, chemistry, civics, cpp-programming, economics, emerging-technology, english-1, english-2, entrepreneurship, geography, global-trends, history, inclusiveness, logic, math-natural, math-social, physical-fitness, physics, psychology.

## Proposed alias: Year 1/2 course id -> freshman scope (titles from `EceTreeData.kt`)
| ECE course id | Title | Freshman scope | Sure? |
|---|---|---|---|
| flen1011 | Communicative English I | freshman/english-1 | yes |
| loct1011 | Critical Thinking | freshman/logic | UNSURE (logic and critical thinking) |
| math1011 | Mathematics for Natural Sciences | freshman/math-natural | yes |
| phys1011 | General Physics | freshman/physics | yes |
| gees1011 | Geography of Ethiopia and the Horn | freshman/geography | yes |
| psch1011 | General Psychology | freshman/psychology | yes |
| spsc1011 | Physical Fitness | freshman/physical-fitness | yes |
| flen1012 | Communicative English II | freshman/english-2 | yes |
| anth1012 | Social Anthropology | freshman/anthropology | yes |
| math1042 | Applied Mathematics I | freshman/applied-math-1 | yes |
| mgmt1012 | Entrepreneurship | freshman/entrepreneurship | yes |
| emte1012 | Emerging Technologies | freshman/emerging-technology | yes |
| mcie1012 | Moral and Civic Education | freshman/civics | UNSURE (civics) |
| eceg1052 | Computer Programming | freshman/cpp-programming | UNSURE (owner: same content as freshman) |
| hist1012 | History of Ethiopia and the Horn | freshman/history | yes |
| snie1012 | Inclusiveness (Year 2 S1) | freshman/inclusiveness | yes |
| gltr1012 | Global Trend (Year 2 S2) | freshman/global-trends | yes |

Year 2 engineering courses (eceg2041, eceg2043, meng2061, ceng2051, math2061, eceg2061, math2062, meng2052, eceg2070, eceg2052, eceg2072, eceg2042) have NO content anywhere yet: their hubs stay empty until the owner uploads materials. Not invented.

## BLOCKER to decide before any alias patch
Freshman rows carry `package_id = freshman`. The checked door (`open_resource_v2`) and downloads (`content_guard.resource_visible`) decide by the ROW's package. A student who bought only `ece-y1-sem-*` would probably be refused (Locked) on a `freshman` row, and the owner wants different ownership (freshman buyers must not get ECE and the reverse). UNSURE: not read yet (database functions). Two ways, owner decides:
1. **Alias in app + server rule (no copied content):** app maps `ece/y1-sem-N/<slug>` to the freshman scope; the database functions must also accept `ece-y1-sem-*` ownership for those freshman rows. Touches live Supabase rules (merge with E2c-d, needs approval).
2. **Copy rows (owner's first wording "create the Year 1 courses again"):** insert copies of the freshman rows under `ece/y1-sem-N/<slug>` with package_id `ece-y1-sem-N` (same files/body, new ids). No server rule change, but content is duplicated in the database and every later edit must be made twice.

## Also still unknown
How the course path `.../y1-sem-1/<slug>` becomes the scope string passed to `list()` (`inferPackageAndScope` in `ContentAccess.kt`; the workspace wiring is in `MainActivity.kt`, do not read). Next chat: read only `inferPackageAndScope` in `ContentAccess.kt` (35.7 KB, read once, do not resend).
