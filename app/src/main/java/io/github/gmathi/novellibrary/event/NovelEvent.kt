package io.github.gmathi.novellibrary.event

import io.github.gmathi.novellibrary.model.WebPage


class NovelEvent(var type: EventType, var novelId: Long = -1L, var webPage: WebPage? = null) 
