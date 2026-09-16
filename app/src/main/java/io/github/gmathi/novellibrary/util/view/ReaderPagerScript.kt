package io.github.gmathi.novellibrary.util.view

/**
 * JavaScript injected into the reader WebView when page mode is on.
 *
 * The chapter body is wrapped in a fixed-height, multi-column container whose column width equals
 * the viewport width, so every column is one screen-sized page. Pages are turned by translating the
 * container horizontally. Input handling:
 *  - horizontal swipe: next / previous page
 *  - tap on the left or right third of the screen: previous / next page
 *  - tap in the middle: `HTMLOUT.onCenterTap()` (toggles the reader menu)
 *  - turning past the first or last page: `HTMLOUT.onChapterBoundary("prev" | "next")`
 *  - every page change: `HTMLOUT.onPageChanged(page, total)` so the position can be remembered
 *
 * `window.__nlPager` exposes `next()`, `prev()`, `goTo(page)` and `relayout()` for native callers
 * (volume keys, text size changes). Running the script again on an already paged document only
 * re-lays out the pages.
 */
object ReaderPagerScript {

    private const val INITIAL_PAGE_TOKEN = "__INITIAL_PAGE__"
    private const val PAD_TOP_TOKEN = "__PAD_TOP__"
    private const val PAD_BOTTOM_TOKEN = "__PAD_BOTTOM__"
    private const val SAFE_BOTTOM_TOKEN = "__SAFE_BOTTOM__"

    /** Height reserved at the bottom of every page for the "page / total" counter, in CSS px. */
    private const val COUNTER_HEIGHT = 22

    /**
     * @param initialPage page to open at (clamped by the script).
     * @param safeTopCss extra top padding in CSS px so text clears a display cutout or status bar.
     * @param safeBottomCss extra bottom padding in CSS px so text clears the navigation bar.
     */
    fun build(initialPage: Int, safeTopCss: Int = 0, safeBottomCss: Int = 0): String = SCRIPT
        .replace(INITIAL_PAGE_TOKEN, initialPage.coerceAtLeast(0).toString())
        .replace(PAD_TOP_TOKEN, (16 + safeTopCss.coerceAtLeast(0)).toString())
        .replace(PAD_BOTTOM_TOKEN, (8 + COUNTER_HEIGHT + safeBottomCss.coerceAtLeast(0)).toString())
        .replace(SAFE_BOTTOM_TOKEN, safeBottomCss.coerceAtLeast(0).toString())

    private val SCRIPT = """
(function () {
  var INITIAL = __INITIAL_PAGE__;
  var doc = document, html = doc.documentElement, body = doc.body;
  if (!body) return;
  if (window.__nlPager) { window.__nlPager.relayout(); return; }

  var wrap = doc.getElementById('nl-pager-wrap');
  if (!wrap) {
    wrap = doc.createElement('div');
    wrap.id = 'nl-pager-wrap';
    while (body.firstChild) wrap.appendChild(body.firstChild);
    body.appendChild(wrap);
  }
  if (!doc.getElementById('nl-pager-style')) {
    var style = doc.createElement('style');
    style.id = 'nl-pager-style';
    style.textContent =
      'html,body{height:100% !important;margin:0 !important;padding:0 !important;overflow:hidden !important;}' +
      'body{position:relative !important;}' +
      '#nl-pager-wrap{position:absolute;left:0;top:0;height:100vh;width:100vw;box-sizing:border-box;' +
      'padding:__PAD_TOP__px 14px __PAD_BOTTOM__px 14px;column-width:calc(100vw - 28px);column-gap:28px;column-fill:auto;' +
      'will-change:transform;transition:transform 120ms ease-out;}' +
      '#nl-pager-wrap img{max-width:100% !important;max-height:85vh !important;object-fit:contain;}' +
      '#nl-pager-wrap pre,#nl-pager-wrap table{white-space:pre-wrap;max-width:100%;}' +
      '#nl-pager-count{position:fixed;left:0;right:0;bottom:__SAFE_BOTTOM__px;height:22px;line-height:22px;' +
      'text-align:center;font-size:12px;font-family:sans-serif;opacity:0.55;pointer-events:none;' +
      'color:inherit;z-index:2147483647;}';
    doc.head.appendChild(style);
  }

  // "page / total" indicator for the current chapter. Lives outside the column container so it
  // never takes part in the page flow; refreshed on every page change and relayout.
  var counter = doc.getElementById('nl-pager-count');
  if (!counter) {
    counter = doc.createElement('div');
    counter.id = 'nl-pager-count';
    counter.setAttribute('tts-disable', 'true');
    body.appendChild(counter);
  }

  var page = 0, total = 1;

  function pageWidth() { return html.clientWidth || window.innerWidth || 1; }

  function notify() {
    counter.textContent = (page + 1) + ' / ' + total;
    if (window.HTMLOUT && HTMLOUT.onPageChanged) HTMLOUT.onPageChanged(page, total);
  }

  function apply() {
    wrap.style.transform = 'translateX(' + (-page * pageWidth()) + 'px)';
    notify();
  }

  function relayout() {
    var w = pageWidth();
    total = Math.max(1, Math.ceil((wrap.scrollWidth - 1) / w));
    if (page > total - 1) page = total - 1;
    if (page < 0) page = 0;
    apply();
  }

  function next() {
    if (page < total - 1) { page++; apply(); }
    else if (window.HTMLOUT && HTMLOUT.onChapterBoundary) HTMLOUT.onChapterBoundary('next');
  }

  function prev() {
    if (page > 0) { page--; apply(); }
    else if (window.HTMLOUT && HTMLOUT.onChapterBoundary) HTMLOUT.onChapterBoundary('prev');
  }

  function goTo(p) {
    p = p | 0;
    if (p < 0) p = total - 1;
    page = Math.max(0, Math.min(total - 1, p));
    apply();
  }

  var sx = 0, sy = 0, st = 0, moved = false, tracking = false;

  doc.addEventListener('touchstart', function (e) {
    if (e.touches.length !== 1) { tracking = false; return; }
    tracking = true; moved = false;
    sx = e.touches[0].clientX; sy = e.touches[0].clientY; st = Date.now();
  }, { passive: true });

  doc.addEventListener('touchmove', function (e) {
    if (!tracking) return;
    var dx = e.touches[0].clientX - sx, dy = e.touches[0].clientY - sy;
    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true;
    if (e.cancelable) e.preventDefault();
  }, { passive: false });

  doc.addEventListener('touchend', function (e) {
    if (!tracking) return;
    tracking = false;
    var t = e.changedTouches[0];
    var dx = t.clientX - sx, dy = t.clientY - sy, dt = Date.now() - st;
    if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy) * 1.5) {
      if (dx < 0) next(); else prev();
      return;
    }
    if (moved || dt > 400) return;
    var target = e.target;
    if (target && target.closest && target.closest('a,button,input,textarea,select,video,audio')) return;
    var w = pageWidth();
    if (t.clientX < w / 3) prev();
    else if (t.clientX > w * 2 / 3) next();
    else if (window.HTMLOUT && HTMLOUT.onCenterTap) HTMLOUT.onCenterTap();
  }, { passive: true });

  window.addEventListener('resize', function () { setTimeout(relayout, 50); });

  Array.prototype.forEach.call(doc.images, function (img) {
    if (!img.complete) img.addEventListener('load', function () { setTimeout(relayout, 0); });
  });

  window.__nlPager = { next: next, prev: prev, relayout: relayout, goTo: goTo };
  relayout();
  goTo(INITIAL);
})();
""".trimIndent()
}
