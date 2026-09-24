package com.example.catslist.presentation.components

/**
 * What a screen's refresh request is doing. [Failed] describes the request that last
 * *finished*; success is inferred from a run that ended without it, so there is no `Succeeded`.
 */
enum class RefreshSignal { Idle, Running, Failed }
