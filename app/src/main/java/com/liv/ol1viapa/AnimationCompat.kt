package com.liv.ol1viapa

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition

infix fun EnterTransition.togetherWith(exit: ExitTransition): ContentTransform = ContentTransform(this, exit)
