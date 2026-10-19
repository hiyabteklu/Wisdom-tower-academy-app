package com.wisdomtower.academy.ui.packages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomInk

/**
 * Packages tab, mirrors website src/app/packages/page.tsx + components/PackagesCatalog.tsx:
 * title "Academy packages", headings "Grades 9–12", "Other branches", "Special packages".
 * Every card is the shared [OpenCard] (picture, title, Open button) in a two-column grid.
 * D3 (day mode): the headings use [WisdomInk] (white at night, deep navy by day) instead of Color.White.
 */
@Composable
fun PackagesScreen(
    modifier: Modifier = Modifier,
    packageList: List<NativePackage> = CATALOG_PACKAGES,
    onSelectPackage: (NativePackage) -> Unit,
    onNavigateToUrl: (String) -> Unit
) {
    val grades = remember(packageList) { packageList.filter { it.group == "grades" } }
    val branches = remember(packageList) { packageList.filter { it.group == "branch" } }
    val specials = remember(packageList) { packageList.filter { it.group == "special" } }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "packages_title_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Academy packages",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WisdomInk,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (grades.isNotEmpty()) {
                item(key = "section_grades_heading") {
                    Text(
                        text = "Grades 9–12",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WisdomInk,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                openGrid("grades", grades) { p ->
                    OpenCard(p.name, p.assetImage) { onSelectPackage(p) }
                }
            }

            if (branches.isNotEmpty()) {
                item(key = "section_branches_heading") {
                    Text(
                        text = "Other branches",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WisdomInk,
                        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                    )
                }
                openGrid("branches", branches) { p ->
                    OpenCard(p.name, p.assetImage) { onSelectPackage(p) }
                }
            }

            if (specials.isNotEmpty()) {
                item(key = "section_specials_heading") {
                    Text(
                        text = "Special packages",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WisdomInk,
                        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                    )
                }
                openGrid("specials", specials) { p ->
                    OpenCard(p.name, p.assetImage) { onSelectPackage(p) }
                }
            }

            item(key = "packages_bottom_spacer") {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
