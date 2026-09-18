package io.github.gmathi.novellibrary.util.view

/**
 * JavaScript that turns a reader chapter into screen-sized pages when page mode is on.
 *
 * The script is embedded at the end of the chapter document itself (see
 * `WebPageDBFragment.loadCreatedDocument`) and starts as soon as the document is parsed, so it does
 * not depend on `onPageFinished`: that callback can arrive for the blank page shown while loading
 * instead of the chapter, and it waits for every image and font, which left chapters without a
 * pager (dead taps, no page turns).
 *
 * The chapter body is wrapped in a fixed-height, multi-column container whose column width equals
 * the viewport width, so every column is one page. Pages are turned by translating the container
 * horizontally. Input handling:
 *  - horizontal swipe: next / previous page
 *  - tap on the left or right third of the screen: previous / next page
 *  - tap in the middle: `HTMLOUT.onCenterTap` (toggles the reader menu)
 *  - turning past the first or last page: `HTMLOUT.onChapterBoundary(…, "prev" | "next")`
 *  - turning onto the last page: `HTMLOUT.onLastPageReached`
 *  - every page change: `HTMLOUT.onPageChanged(…, page, total)` so the position can be remembered
 *
 * Every call to `HTMLOUT` carries the document's generation number so the app can ignore a document
 * that has since been replaced. On start the script asks `HTMLOUT.pagerInit(generation)` for the page
 * to open at and the room to leave for the system bars.
 *
 * `window.__nlPager` exposes `next()`, `prev()`, `goTo(page, instant)`, `relayout()` and
 * `setInsets(top, bottom)` for native callers (volume keys, page slider, text size changes).
 */
object ReaderPagerScript {

    /** Id of the `<script>` element holding the pager, so a rebuilt document can replace it. */
    const val ELEMENT_ID = "nl-pager-script"

    private const val GENERATION_TOKEN = "__GENERATION__"

    /** @param generation identifies this document in the script's calls back to the app. */
    fun build(generation: Int): String = SCRIPT.replace(GENERATION_TOKEN, generation.toString())

    private val SCRIPT = """
(function () {
  var GEN = __GENERATION__;
  var bridge = window.HTMLOUT;

  function call(fn) { try { if (bridge) fn(); } catch (e) {} }

  function init() {
    var doc = document, html = doc.documentElement, body = doc.body;
    if (!body || window.__nlPager) return;

    // Page to open at (-1 = last page) and extra top/bottom padding in CSS px for the display
    // cutout and navigation bar. Asked for now rather than baked in, so a page the reader requested
    // while this chapter was loading is honoured.
    var cfg = [];
    call(function () { cfg = String(bridge.pagerInit(GEN)).split(','); });
    var initial = parseInt(cfg[0], 10) || 0;

    var wrap = doc.createElement('div');
    wrap.id = 'nl-pager-wrap';
    while (body.firstChild) wrap.appendChild(body.firstChild);
    body.appendChild(wrap);

    var style = doc.createElement('style');
    style.textContent =
      'html,body{height:100% !important;margin:0 !important;padding:0 !important;overflow:hidden !important;touch-action:none;}' +
      'body{position:relative !important;}' +
      '#nl-pager-wrap{position:absolute;left:0;top:0;height:100vh;width:100vw;box-sizing:border-box;' +
      'padding-left:14px;padding-right:14px;column-width:calc(100vw - 28px);column-gap:28px;column-fill:auto;' +
      'will-change:transform;transition:transform 120ms ease-out;}' +
      '#nl-pager-wrap img{max-width:100% !important;max-height:85vh !important;object-fit:contain;}' +
      '#nl-pager-wrap pre,#nl-pager-wrap table{white-space:pre-wrap;max-width:100%;}';
    (doc.head || html).appendChild(style);

    function setPadding(top, bottom) {
      wrap.style.paddingTop = (16 + (top | 0)) + 'px';
      wrap.style.paddingBottom = (12 + (bottom | 0)) + 'px';
    }
    setPadding(parseInt(cfg[1], 10), parseInt(cfg[2], 10));

    // anchor is the page asked for, by the app or by the reader's last turn (-1 = the last page).
    // Every layout derives the shown page from it, so a count taken before the chapter has settled
    // (fonts or images still loading) cannot pull the reader back a few pages for good.
    var anchor = initial, page = 0, total = 1, laidOut = false, reported = '';

    function pageWidth() { return html.clientWidth || window.innerWidth || 0; }

    function place(instant) {
      var x = 'translateX(' + (-page * pageWidth()) + 'px)';
      if (!instant) { wrap.style.transform = x; return; }
      var transition = wrap.style.transition;
      wrap.style.transition = 'none';
      wrap.style.transform = x;
      void wrap.offsetWidth; // flush so restoring the transition does not animate this move
      wrap.style.transition = transition;
    }

    function report(userTurn) {
      if (!laidOut) return;
      var key = page + '/' + total;
      if (key !== reported) {
        reported = key;
        call(function () { bridge.onPageChanged(GEN, page, total); });
      }
      if (userTurn && page === total - 1) call(function () { bridge.onLastPageReached(GEN); });
    }

    // Counts the pages again after anything that reflows the chapter.
    function relayout() {
      var w = pageWidth();
      if (!w) return; // not laid out yet; the resize listener tries again
      laidOut = true;
      total = Math.max(1, Math.ceil((wrap.scrollWidth - 1) / w));
      page = anchor < 0 ? total - 1 : Math.min(anchor, total - 1);
      place(true);
      report(false);
    }

    var relayoutTimer = 0;
    function relayoutSoon() { clearTimeout(relayoutTimer); relayoutTimer = setTimeout(relayout, 60); }

    // A page turn by the reader (swipe, tap, volume key). Past either end it hands off to the
    // neighbouring chapter.
    function turn(delta) {
      if (!laidOut) { relayout(); if (!laidOut) return; }
      anchor = page;
      if (page + delta >= 0 && page + delta < total) { page += delta; anchor = page; place(false); report(true); return; }
      if (delta > 0) {
        // The count can lag behind a reflow (a font or image still loading): recount before
        // leaving the chapter.
        relayout();
        if (page + 1 < total) { page++; anchor = page; place(false); report(true); return; }
      }
      call(function () { bridge.onChapterBoundary(GEN, delta > 0 ? 'next' : 'prev'); });
    }

    function next() { turn(1); }
    function prev() { turn(-1); }

    // Positions the chapter for the app (page slider, chapter hand-off); p < 0 means the last page.
    function goTo(p, instant) {
      anchor = p | 0;
      if (!laidOut) return; // applied by the first relayout
      page = anchor < 0 ? total - 1 : Math.max(0, Math.min(anchor, total - 1));
      place(!!instant);
      report(false);
    }

    function setInsets(top, bottom) { setPadding(top, bottom); relayout(); }

    // Touch handling. One touch is followed by its identifier, so another contact on the screen (a
    // resting thumb or palm) cannot swallow taps and swipes. A horizontal drag moves the page with
    // the finger; releasing past a quarter of the width, or a quick flick, turns the page, anything
    // less snaps back. At the first/last page the drag has resistance. A short touch without
    // movement is a tap: the outer thirds turn the page, the middle toggles the reader menu.
    // Durations and speeds use the events' own timestamps, so a busy page cannot turn a quick tap
    // into a long press.
    var g = null;

    function touchById(list, id) {
      for (var i = 0; i < list.length; i++) if (list[i].identifier === id) return list[i];
      return null;
    }

    function abortGesture() {
      if (g && g.dragging) { wrap.style.transition = ''; place(false); }
      g = null;
    }

    doc.addEventListener('touchstart', function (e) {
      // A second finger during a drag cancels the drag; otherwise follow the newest touch.
      if (g && g.dragging) { abortGesture(); return; }
      var t = e.changedTouches[0];
      g = { id: t.identifier, x0: t.clientX, y0: t.clientY, t0: e.timeStamp,
            lastX: t.clientX, lastT: e.timeStamp, vx: 0, moved: false, dragging: false };
    }, { passive: true });

    doc.addEventListener('touchmove', function (e) {
      if (e.cancelable) e.preventDefault();
      var t = g && touchById(e.changedTouches, g.id);
      if (!t) return;
      // Release velocity in CSS px per ms, smoothed over the last samples, for flick detection.
      if (e.timeStamp > g.lastT) g.vx = 0.5 * g.vx + 0.5 * (t.clientX - g.lastX) / (e.timeStamp - g.lastT);
      g.lastX = t.clientX; g.lastT = e.timeStamp;
      var dx = t.clientX - g.x0, dy = t.clientY - g.y0;
      if (!g.moved) {
        if (Math.abs(dx) < 10 && Math.abs(dy) < 10) return;
        g.moved = true;
        // Decided once per gesture: mostly horizontal means a page drag.
        g.dragging = Math.abs(dx) > Math.abs(dy);
        if (g.dragging) wrap.style.transition = 'none';
      }
      if (g.dragging) {
        var w = pageWidth();
        var atEdge = (dx > 0 && page === 0) || (dx < 0 && page === total - 1);
        var offset = atEdge ? dx * 0.35 : Math.max(-w, Math.min(w, dx));
        wrap.style.transform = 'translateX(' + (-page * w + offset) + 'px)';
      }
    }, { passive: false });

    doc.addEventListener('touchend', function (e) {
      var t = g && touchById(e.changedTouches, g.id);
      if (!t) return;
      var gesture = g;
      g = null;
      var dx = t.clientX - gesture.x0;
      if (gesture.dragging) {
        wrap.style.transition = '';
        // A flick is a fast release in the direction of the drag. Thresholds are CSS px (about
        // 2.5-3.5 device px each on phones), so they stay small.
        var flick = Math.abs(dx) > 15 && Math.abs(gesture.vx) > 0.2 && (gesture.vx < 0) === (dx < 0);
        if (flick || Math.abs(dx) > pageWidth() * 0.25) turn(dx < 0 ? 1 : -1);
        place(false); // settle the dragged page; a chapter hand-off leaves this page where it was
        return;
      }
      if (gesture.moved || e.timeStamp - gesture.t0 > 500) return;
      var target = e.target;
      if (target && target.closest && target.closest('a,button,input,textarea,select,video,audio')) return;
      var w = pageWidth();
      if (t.clientX < w / 3) turn(-1);
      else if (t.clientX > w * 2 / 3) turn(1);
      else call(function () { bridge.onCenterTap(GEN); });
    }, { passive: true });

    doc.addEventListener('touchcancel', function (e) {
      if (g && touchById(e.changedTouches, g.id)) abortGesture();
    }, { passive: true });

    // Anything that changes the chapter's length re-counts the pages: size changes, late images
    // and fonts, and the page's own scripts expanding footnotes or zooming images.
    window.addEventListener('resize', relayoutSoon);
    Array.prototype.forEach.call(doc.images, function (img) {
      if (!img.complete) { img.addEventListener('load', relayoutSoon); img.addEventListener('error', relayoutSoon); }
    });
    if (doc.fonts) {
      if (doc.fonts.ready) doc.fonts.ready.then(relayoutSoon);
      if (doc.fonts.addEventListener) doc.fonts.addEventListener('loadingdone', relayoutSoon);
    }
    if (window.MutationObserver) {
      new MutationObserver(function (records) {
        for (var i = 0; i < records.length; i++) if (records[i].target !== wrap) { relayoutSoon(); return; }
      }).observe(wrap, { subtree: true, childList: true, attributes: true, attributeFilter: ['class', 'style', 'src', 'hidden'] });
    }

    window.__nlPager = { next: next, prev: prev, goTo: goTo, relayout: relayout, setInsets: setInsets };
    relayout();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
""".trimIndent()
}
