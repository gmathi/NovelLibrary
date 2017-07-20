package com.mgn.bingenovelreader.event

import com.mgn.bingenovelreader.model.WebPage


class NovelEvent(var type: EventType, var novelId: Long = -1L, var webPage: WebPage? = null) 
