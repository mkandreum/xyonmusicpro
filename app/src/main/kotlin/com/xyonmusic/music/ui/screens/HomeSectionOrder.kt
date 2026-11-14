/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.random.Random

private val DEFAULT_HOME_SECTION_ORDER = mapOf(
    HomeSection.Hero to 110,
    HomeSection.ListenAgain to 100,
    HomeSection.QuickPicks to 90,
    HomeSection.SpeedDial to 80,
    HomeSection.FromTheCommunity to 70,
    HomeSection.DailyDiscover to 60,
    HomeSection.AccountPlaylists to 50,
    HomeSection.ForgottenFavorites to 40,
    HomeSection.MoodAndGenres to 10,
)

/** Builds Home's ordered section list: default weights, a saved custom order, or a seeded shuffle. */
internal fun buildHomeSections(
    homeState: com.xyonmusic.music.viewmodels.HomeUiState,
    heroCardEnabled: Boolean,
    randomizeHomeOrder: Boolean,
    savedSectionOrder: String,
    hiddenSectionIds: String,
    randomSeed: Long,
): List<HomeSection> {
    val quickPicks = homeState.quickPicks
    val communityPlaylists = homeState.communityPlaylists
    val dailyDiscover = homeState.dailyDiscover
    val listenAgain = homeState.listenAgain
    val topSongs = homeState.topSongs
    val accountPlaylists = homeState.accountPlaylists
    val forgottenFavorites = homeState.forgottenFavorites
    val similarRecommendations = homeState.similarRecommendations
    val homePage = homeState.homePage
    val explorePage = homeState.explorePage
    val hidden = hiddenSectionIds.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toSet()
    val list = mutableListOf<HomeSection>()

    // Inicio, simplified: "Para ti" on top and at most four rows under it. Community, daily discover,
    // forgotten favourites, the "similar to" rows and moods are gone (moods live in Search).
    if (heroCardEnabled && quickPicks?.isNotEmpty() == true) list.add(HomeSection.Hero)
    if (listenAgain?.items?.isNotEmpty() == true) list.add(HomeSection.ListenAgain)
    if (quickPicks?.isNotEmpty() == true) list.add(HomeSection.QuickPicks)
    if (accountPlaylists?.isNotEmpty() == true) list.add(HomeSection.AccountPlaylists)
    // "New from your artists": the first section of YouTube Music's own home feed.
    homePage?.sections?.indices?.take(1)?.forEach { i -> list.add(HomeSection.HomePageSection(i)) }

    // Apply hidden-section filtering once after building the available sections.
    list.retainAll { it.id !in hidden }

    val customOrder = savedSectionOrder.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toList()

    return if (randomizeHomeOrder) {
        list.map { section -> section to randomizedSectionWeight(section, randomSeed) }
            .sortedByDescending { it.second }
            .map { it.first }
    } else if (customOrder.isNotEmpty()) {
        val customOrderRank = customOrder.distinct()
            .withIndex()
            .associate { (index, id) -> id to index }

        // Keep unlisted sections after the saved ones, ordered by their default weight.
        list.sortedWith(
            compareBy(
                { section -> customOrderRank[section.id] ?: Int.MAX_VALUE },
                { section -> -section.baseWeight },
            ),
        )
    } else {
        list.sortedByDescending { section ->
            when (section) {
                is HomeSection.SimilarRecommendation -> 30 - section.index
                is HomeSection.HomePageSection -> 20 - section.index
                else -> DEFAULT_HOME_SECTION_ORDER[section] ?: 0
            }
        }
    }
}

private fun randomizedSectionWeight(section: HomeSection, seed: Long): Int {
    // Seed each section independently so other sections appearing or disappearing won't reshuffle it.
    val sectionRandom = Random(seed + section.id.hashCode())

    // Keep main sections close enough to overlap after applying the random modifier.
    val base = when (section) {
        // Pinned above everything, even with shuffling on.
        HomeSection.Hero -> 10_000

        HomeSection.SpeedDial,
        HomeSection.QuickPicks,
        HomeSection.DailyDiscover -> 500

        HomeSection.ListenAgain,
        HomeSection.AccountPlaylists,
        HomeSection.ForgottenFavorites,
        HomeSection.FromTheCommunity -> 300

        else -> 100
    }

    val modifier = when (section) {
        HomeSection.SpeedDial,
        HomeSection.QuickPicks,
        HomeSection.DailyDiscover -> sectionRandom.nextInt(-200, 400)

        HomeSection.ListenAgain,
        HomeSection.AccountPlaylists,
        HomeSection.ForgottenFavorites,
        HomeSection.FromTheCommunity -> sectionRandom.nextInt(-100, 400)

        else -> sectionRandom.nextInt(-50, 50)
    }

    return base + modifier
}
