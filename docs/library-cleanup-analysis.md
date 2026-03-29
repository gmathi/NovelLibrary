# Library Cleanup Analysis

This document analyzes third-party libraries used in the app and provides recommendations for removal or replacement with built-in Android components.

## Summary

| Library | Status | Recommendation | Effort |
|---------|--------|----------------|--------|
| Glide | ✅ KEEP | Primary image loading, widely used | N/A |
| Coil | ⚠️ CONSOLIDATE | Duplicate of Glide, only used in Compose | Medium |
| EventBus | ❌ REMOVE | Replace with LiveData/StateFlow | High |
| Material Dialogs | ⚠️ CONSIDER | Can use native AlertDialog | Medium |
| RxJava/RxAndroid | ❌ REMOVE | Replace with Coroutines/Flow | High |
| Lottie | ✅ KEEP | Used for animations, no native alternative | N/A |
| PhotoView | ✅ KEEP | Essential for image zooming | N/A |
| SmartTabLayout | ❌ REMOVE | Replace with TabLayout | Low |
| RecyclerView Animators | ❌ REMOVE | Minimal usage, can use native | Low |
| SnackProgressBar | ⚠️ CONSIDER | Can use native Snackbar | Low |
| Konfetti | ❌ REMOVE | Only used for easter egg | Low |
| FancyButtons | ❌ REMOVE | Only 1 usage, use MaterialButton | Low |

---

## Detailed Analysis

### 1. Image Loading Libraries

#### Glide (v4.16.0) - ✅ KEEP
**Usage:** Heavily used throughout the app (14+ files)
- SearchTermFragment, LibraryFragment, ExtensionsFragment
- NovelDetailsActivity, ImagePreviewActivity
- TTSPlayer for notification images

**Recommendation:** KEEP - This is the primary image loading library and is deeply integrated.

#### Coil (v2.7.0) - ⚠️ CONSOLIDATE
**Usage:** Only used in 2 places
- Compose components (NovelItem.kt)
- NetworkHelper for image caching setup

**Recommendation:** CONSOLIDATE - Since you're using Glide everywhere else, consider:
- Use Glide's Compose integration instead of Coil
- This reduces dependency count and APK size
- Effort: Medium (need to migrate Compose image loading)

---

### 2. Event Communication

#### EventBus (v3.3.1) - ❌ REMOVE
**Usage:** Used in 6 files for inter-component communication
- ChaptersFragment, LibraryFragment, WebPageDBFragment
- ChaptersPagerActivity, ReaderDBPagerActivity, LibrarySearchActivity

**Recommendation:** REMOVE - Replace with modern Android architecture:
- Use LiveData or StateFlow for reactive data
- Use ViewModel for shared state between fragments
- Use callback interfaces for direct communication
- Effort: High (requires architectural refactoring)

**Benefits:**
- Better lifecycle awareness
- Type-safe communication
- Follows modern Android best practices
- Reduces dependencies

---

### 3. Reactive Programming

#### RxJava (v1.3.8) + RxAndroid (v1.2.1) - ❌ REMOVE
**Usage:** Used in 11+ files for async operations
- HttpSource, CatalogueSource, Source interfaces
- ExtensionsFragment, NovelUpdatesSource
- Extension management and network operations

**Recommendation:** REMOVE - Replace with Kotlin Coroutines and Flow:
- You already have Coroutines in the project
- Modern Kotlin approach with better performance
- Simpler syntax and better error handling
- Effort: High (requires significant refactoring)

**Migration Path:**
- Observable → Flow
- Single → suspend function
- Schedulers → Dispatchers
- subscribeOn/observeOn → flowOn/withContext

---

### 4. UI Components

#### Material Dialogs (v3.3.0) - ⚠️ CONSIDER
**Usage:** Extensively used (20+ files) for dialogs
- All settings activities
- Novel management activities
- Various fragments

**Recommendation:** CONSIDER keeping or gradual migration
- Native AlertDialog.Builder can replace most usage
- Material Dialogs provides nicer API and styling
- Effort: Medium-High (many usages to migrate)

**Decision:** Keep for now, migrate gradually if needed

---

#### SmartTabLayout (v2.0.0) - ❌ REMOVE
**Usage:** Used in 5 XML layouts
- content_chapters_pager.xml
- fragment_search.xml
- content_extensions_pager.xml
- content_recent_novels_pager.xml
- content_library_pager.xml

**Recommendation:** REMOVE - Replace with Material TabLayout
- TabLayout is part of Material Components (already included)
- Provides same functionality with better Material Design support
- Effort: Low (simple XML and code changes)

**Migration:**
```xml
<!-- Replace -->
<com.ogaclejapan.smarttablayout.SmartTabLayout />
<!-- With -->
<com.google.android.material.tabs.TabLayout />
```

---

#### PhotoView (v2.3.0) - ✅ KEEP
**Usage:** Used in activity_image_preview.xml
- Essential for pinch-to-zoom functionality
- No native equivalent with same features

**Recommendation:** KEEP - Provides essential image viewing features

---

#### Lottie (v6.6.2) - ✅ KEEP
**Usage:** Used in 3 layouts for animations
- generic_error_view.xml
- generic_empty_view.xml
- generic_loading_view.xml

**Recommendation:** KEEP - Industry standard for animations
- No native alternative for JSON animations
- Provides professional loading/empty states

---

#### FancyButtons (v1.8.3) - ❌ REMOVE
**Usage:** Only 1 usage in content_novel_details.xml
- "Add to Library" button

**Recommendation:** REMOVE - Replace with MaterialButton
- MaterialButton provides same features (icons, styling)
- Already included in Material Components
- Effort: Low (single button replacement)

**Migration:**
```xml
<!-- Replace -->
<mehdi.sakout.fancybuttons.FancyButton />
<!-- With -->
<com.google.android.material.button.MaterialButton
    app:icon="@drawable/ic_add"
    app:iconGravity="textStart" />
```

---

#### RecyclerView Animators (v4.0.2) - ❌ REMOVE
**Usage:** Only 1 usage in RecyclerViewExt.kt
- SlideInRightAnimator for RecyclerView items

**Recommendation:** REMOVE - Use native ItemAnimator
- RecyclerView has built-in DefaultItemAnimator
- Can create custom animations if needed
- Effort: Low (minimal usage)

**Migration:**
```kotlin
// Replace
recyclerView.itemAnimator = SlideInRightAnimator()
// With
recyclerView.itemAnimator = DefaultItemAnimator()
// Or custom animation
```

---

#### SnackProgressBar (v6.4.2) - ⚠️ CONSIDER
**Usage:** Used in 6 files for progress notifications
- LibraryFragment, ImportLibraryActivity
- BackupSettingsActivity, ChaptersPagerActivity
- SyncSettingsActivity, Utils

**Recommendation:** CONSIDER keeping or replace with native Snackbar
- Native Snackbar can show progress with custom views
- SnackProgressBar provides better queue management
- Effort: Low-Medium (depends on features needed)

**Decision:** Keep if queue management is important, otherwise migrate

---

#### Konfetti (v1.3.2) - ❌ REMOVE
**Usage:** Only used in MainSettingsActivity
- Easter egg confetti animation

**Recommendation:** REMOVE - Non-essential feature
- Only used for decorative easter egg
- Can remove feature or implement simple alternative
- Effort: Low (remove feature or simple replacement)

---

## Recommended Action Plan

### Phase 1: Quick Wins (Low Effort)
1. **Remove FancyButtons** → Replace with MaterialButton
2. **Remove Konfetti** → Remove easter egg or simple alternative
3. **Remove RecyclerView Animators** → Use DefaultItemAnimator
4. **Remove SmartTabLayout** → Replace with TabLayout

**Estimated Time:** 1-2 days
**APK Size Reduction:** ~500KB

### Phase 2: Consolidation (Medium Effort)
5. **Consolidate Coil → Glide** → Use Glide for Compose
6. **Evaluate SnackProgressBar** → Migrate if not critical

**Estimated Time:** 2-3 days
**APK Size Reduction:** ~300KB

### Phase 3: Architecture Refactoring (High Effort)
7. **Remove RxJava** → Migrate to Coroutines/Flow
8. **Remove EventBus** → Use LiveData/StateFlow
9. **Evaluate Material Dialogs** → Gradual migration if desired

**Estimated Time:** 1-2 weeks
**APK Size Reduction:** ~1-2MB
**Benefits:** Modern architecture, better maintainability

---

## Expected Benefits

### After Phase 1:
- Reduced APK size by ~500KB
- Fewer dependencies to maintain
- More consistent UI with Material Design

### After Phase 2:
- Reduced APK size by ~800KB total
- Single image loading solution
- Simplified dependency management

### After Phase 3:
- Reduced APK size by ~2-3MB total
- Modern Android architecture
- Better performance and maintainability
- Easier to onboard new developers
- Better IDE support and tooling

---

## Libraries to Keep

These libraries provide essential functionality without good native alternatives:

1. **Glide** - Industry standard image loading
2. **PhotoView** - Essential zoom functionality
3. **Lottie** - Professional animations
4. **jsoup** - HTML parsing (no native alternative)
5. **OkHttp/Retrofit** - Network layer (industry standard)
6. **Gson/Kotson** - JSON parsing
7. **Crux** - Article extraction
8. **Injekt** - Lightweight DI
9. **LeakCanary** - Debug tool
10. **Conscrypt** - Security provider

---

## Conclusion

By removing 6-8 third-party libraries and consolidating image loading, you can:
- Reduce APK size by 2-3MB
- Improve app performance
- Simplify maintenance
- Follow modern Android best practices
- Reduce potential security vulnerabilities

Start with Phase 1 for quick wins, then evaluate if Phase 2 and 3 are worth the effort based on your development resources and timeline.
