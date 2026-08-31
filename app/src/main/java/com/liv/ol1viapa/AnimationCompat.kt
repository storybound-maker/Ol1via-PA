package com.liv.ol1viapa

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition

internal fun leauTogetherWith(
    enter: EnterTransition,
    exit: ExitTransition
): Pair<EnterTransition, ExitTransition> = enter to exit
