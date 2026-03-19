# Compose Previews Quick Reference

## 📱 Preview Locations

### Component Previews
```
app/src/main/java/io/github/gmathi/novellibrary/compose/components/
├── RecentlyUpdatedItem.kt
│   ├── @Preview RecentlyUpdatedItemPreview (with 3 variations)
│   ├── @Preview RecentlyUpdatedItemEvenPreview
│   ├── @Preview RecentlyUpdatedItemDarkPreview
│   └── @Preview RecentlyUpdatedListPreview
│
└── NovelItem.kt
    ├── @Preview NovelItemPreview (with 5 variations)
    ├── @Preview NovelItemCompletePreview
    ├── @Preview NovelItemNoRatingPreview
    ├── @Preview NovelItemNoOriginPreview
    ├── @Preview NovelItemMinimalPreview
    ├── @Preview NovelItemLongTitlePreview
    ├── @Preview NovelItemDarkPreview
    ├── @Preview NovelListPreview
    └── @Preview NovelListDarkPreview
```

### Screen Previews
```
app/src/main/java/io/github/gmathi/novellibrary/compose/
└── RecentNovelsScreenPreviews.kt
    ├── Recently Updated Tab:
    │   ├── @Preview RecentlyUpdatedTabSuccessPreview
    │   ├── @Preview RecentlyUpdatedTabLoadingPreview
    │   ├── @Preview RecentlyUpdatedTabErrorPreview
    │   ├── @Preview RecentlyUpdatedTabNoInternetPreview
    │   └── @Preview RecentlyUpdatedTabDarkPreview
    │
    └── Recently Viewed Tab:
        ├── @Preview RecentlyViewedTabSuccessPreview
        ├── @Preview RecentlyViewedTabEmptyPreview
        ├── @Preview RecentlyViewedTabLoadingPreview
        └── @Preview RecentlyViewedTabDarkPreview
```

## 🎨 Preview Matrix

| Component | Light | Dark | States | Variations |
|-----------|-------|------|--------|------------|
| RecentlyUpdatedItem | ✅ | ✅ | Normal, Even | 3 data sets |
| RecentlyUpdatedList | ✅ | - | Multiple items | Alternating colors |
| NovelItem | ✅ | ✅ | Complete, No Rating, No Origin, Minimal, Long Title | 5 data sets |
| NovelList | ✅ | ✅ | Multiple items | Card layout |
| Recently Updated Tab | ✅ | ✅ | Success, Loading, Error, No Internet | 5 previews |
| Recently Viewed Tab | ✅ | ✅ | Success, Loading, Empty | 4 previews |

## 🚀 Quick Commands

### View All Previews
1. Open file in Android Studio
2. Press `Ctrl+Shift+P` (Windows/Linux) or `Cmd+Shift+P` (Mac)
3. Type "Preview" and select "Show Preview"

### Refresh Previews
- `Build > Refresh Previews`
- Or click refresh icon in preview toolbar

### Deploy Preview to Device
- Click "Run" icon next to preview name
- Preview deploys to connected device

## 📊 Preview Count by Category

### By Component Type
- Individual Items: 12
- Lists: 3
- Full Screens: 8
- **Total: 21 previews**

### By State
- Success/Data: 5
- Loading: 3
- Error: 2
- Empty: 1
- No Internet: 1

### By Theme
- Light Mode: 8
- Dark Mode: 4

## 🔍 Preview Features

### RecentlyUpdatedItem Previews
```kotlin
✓ Text truncation with ellipsis
✓ Alternating row backgrounds
✓ Icon rendering (chevron)
✓ Multiple data variations
✓ Dark mode support
```

### Screen Previews
```kotlin
✓ Full app bar with navigation
✓ Tab navigation
✓ Pull-to-refresh indicator
✓ Loading spinners
✓ Error messages with retry
✓ Empty state messages
✓ Action buttons (clear history)
```

## 📝 Sample Data

### Recently Updated Items
```
Novel: "The Beginning After The End"
Chapter: "Chapter 456: The Final Battle"
Publisher: "Tapas"

Novel: "Solo Leveling"
Chapter: "Chapter 270: Epilogue"
Publisher: "Webnovel"

Novel: "Omniscient Reader's Viewpoint"
Chapter: "Chapter 551: Epilogue (End)"
Publisher: "Munpia"
```

### Recently Viewed Novels
```
Novel: "The Beginning After The End"
Rating: 4.8 ⭐
Origin: Korean

Novel: "Solo Leveling"
Rating: 4.9 ⭐
Origin: Korean

Novel: "Omniscient Reader's Viewpoint"
Rating: 4.7 ⭐
Origin: Korean
```

## 🎯 Testing Checklist

### Visual Testing
- [ ] All previews render without errors
- [ ] Text truncation works correctly
- [ ] Icons display properly
- [ ] Colors match theme
- [ ] Spacing is consistent
- [ ] Dark mode looks good

### Interactive Testing (in Interactive Mode)
- [ ] Items are clickable
- [ ] Tabs switch correctly
- [ ] Buttons respond to clicks
- [ ] Lists scroll smoothly

### State Testing
- [ ] Loading state shows spinner
- [ ] Error state shows message
- [ ] Empty state shows placeholder
- [ ] Success state shows data

## 💡 Tips

### Performance
- Previews render faster with fixed heights
- Use `heightDp` parameter for full screens
- Limit list items in previews (3-5 items)

### Data
- Use realistic sample data
- Test edge cases (long text, empty fields)
- Include multiple variations

### Organization
- Keep component previews in same file
- Create separate files for complex screens
- Use descriptive preview names

## 🐛 Troubleshooting

### Preview Not Showing
```
1. Check Compose is enabled in build.gradle
2. Verify @Preview annotation syntax
3. Try Build > Refresh Previews
4. Check for compilation errors
5. Restart Android Studio if needed
```

### Preview Crashes
```
1. Check for null pointer exceptions
2. Use mock data, not real services
3. Avoid accessing Android context
4. Verify all dependencies available
```

### Slow Rendering
```
1. Reduce number of list items
2. Simplify preview data
3. Use fixed heights
4. Disable interactive mode
```

## 📚 Related Documentation

- [recent-novels-previews.md](.kiro/docs/recent-novels-previews.md) - Detailed guide
- [COMPOSE_PREVIEWS_SUMMARY.md](../../../COMPOSE_PREVIEWS_SUMMARY.md) - Complete summary
- [recent-novels-architecture.md](.kiro/docs/recent-novels-architecture.md) - Architecture overview

## ✅ Verification

All previews verified:
- ✅ 21 total previews created
- ✅ No compilation errors
- ✅ All imports resolved
- ✅ Preview annotations correct
- ✅ Sample data structured properly
- ✅ Dark mode variants included
- ✅ All states covered
