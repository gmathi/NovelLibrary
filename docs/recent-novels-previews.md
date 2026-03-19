# Recent Novels Compose Previews

## Overview
Created comprehensive Compose previews for all UI components in the Recent Novels feature. These previews allow developers to see and test UI components in Android Studio without running the app.

## Preview Files

### 1. RecentlyUpdatedItem.kt Previews
Located in: `app/src/main/java/io/github/gmathi/novellibrary/compose/components/RecentlyUpdatedItem.kt`

#### Individual Item Previews
- **RecentlyUpdatedItemPreview** - Shows different item variations using PreviewParameterProvider
  - Standard item
  - Long text truncation
  - Different publishers

- **RecentlyUpdatedItemEvenPreview** - Shows alternating background color (even position)

- **RecentlyUpdatedItemDarkPreview** - Dark mode variant

#### List Preview
- **RecentlyUpdatedListPreview** - Shows multiple items in a list with alternating colors

### 2. RecentNovelsScreenPreviews.kt
Located in: `app/src/main/java/io/github/gmathi/novellibrary/compose/RecentNovelsScreenPreviews.kt`

#### Recently Updated Tab Previews
- **RecentlyUpdatedTabSuccessPreview** - Full tab with loaded data
- **RecentlyUpdatedTabLoadingPreview** - Loading state
- **RecentlyUpdatedTabErrorPreview** - Error state
- **RecentlyUpdatedTabNoInternetPreview** - No internet connection state
- **RecentlyUpdatedTabDarkPreview** - Dark mode with data

#### Recently Viewed Tab Previews
- **RecentlyViewedTabSuccessPreview** - Full tab with novel history
- **RecentlyViewedTabEmptyPreview** - Empty state (no history)
- **RecentlyViewedTabLoadingPreview** - Loading state
- **RecentlyViewedTabDarkPreview** - Dark mode with data

## Preview Data Providers

### RecentlyUpdatedItemsProvider
Provides sample data for Recently Updated items:
```kotlin
- "The Beginning After The End" - Chapter 456
- "Solo Leveling" - Chapter 270
- "Omniscient Reader's Viewpoint" - Chapter 551
- "Reverend Insanity" - Chapter 2334
- "Lord of the Mysteries" - Chapter 1394
```

### RecentlyViewedNovelsProvider
Provides sample Novel objects with:
- Novel names
- Image URLs
- Ratings
- Origin markers (Korean)

## How to Use Previews

### In Android Studio
1. Open any preview file
2. Click "Split" or "Design" view in the top-right
3. Previews will render automatically
4. Use the preview toolbar to:
   - Switch between light/dark mode
   - Change device/screen size
   - Enable interactive mode

### Preview Annotations Used

#### @Preview
Basic preview annotation with options:
- `name` - Preview name shown in Android Studio
- `showBackground` - Shows background color
- `heightDp` - Fixed height for preview
- `uiMode` - UI mode (e.g., night mode)

#### @PreviewParameter
Provides multiple data variations:
```kotlin
@Preview
@Composable
fun MyPreview(
    @PreviewParameter(MyDataProvider::class) data: MyData
) {
    // Preview uses different data variations
}
```

## Preview Coverage

### States Covered
✅ Loading states
✅ Success states with data
✅ Error states
✅ Empty states
✅ No internet states

### UI Variations
✅ Light mode
✅ Dark mode
✅ Different data lengths
✅ Alternating row colors
✅ With/without action buttons

### Screen Sizes
✅ Default phone size
✅ Custom heights (600dp for full screens)

## Benefits

1. **Faster Development** - See UI changes instantly without running the app
2. **Multiple States** - View all UI states side-by-side
3. **Dark Mode Testing** - Easily test dark mode appearance
4. **Data Variations** - Test with different data scenarios
5. **Documentation** - Previews serve as visual documentation
6. **Regression Testing** - Catch UI regressions visually

## Best Practices

### Preview Organization
- Keep previews in the same file as the component (for small components)
- Create separate preview files for complex screens
- Use descriptive preview names

### Preview Data
- Use realistic sample data
- Test edge cases (long text, empty data)
- Include multiple variations

### Preview Parameters
- Use PreviewParameterProvider for data variations
- Keep preview data simple and focused
- Avoid complex business logic in previews

## Running Previews

### Interactive Mode
1. Click the "Interactive" button in preview toolbar
2. Click on interactive elements
3. Test basic interactions without running the app

### Deploy to Device
1. Click the "Run" button on any preview
2. Preview deploys to connected device/emulator
3. Useful for testing on real hardware

## Maintenance

### When to Update Previews
- After changing component UI
- When adding new states
- When modifying data models
- After theme changes

### Preview Checklist
- [ ] All UI states have previews
- [ ] Dark mode previews exist
- [ ] Edge cases are covered
- [ ] Preview names are descriptive
- [ ] Sample data is realistic

## Example Preview Code

### Simple Component Preview
```kotlin
@Preview(name = "My Component", showBackground = true)
@Composable
private fun MyComponentPreview() {
    MaterialTheme {
        MyComponent(
            data = sampleData,
            onClick = {}
        )
    }
}
```

### Preview with Parameter Provider
```kotlin
private class MyDataProvider : PreviewParameterProvider<MyData> {
    override val values = sequenceOf(
        MyData("Short"),
        MyData("Very Long Text That Needs Truncation")
    )
}

@Preview(showBackground = true)
@Composable
private fun MyComponentPreview(
    @PreviewParameter(MyDataProvider::class) data: MyData
) {
    MaterialTheme {
        MyComponent(data = data)
    }
}
```

### Dark Mode Preview
```kotlin
@Preview(
    name = "My Component - Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun MyComponentDarkPreview() {
    MaterialTheme {
        MyComponent(data = sampleData)
    }
}
```

## Troubleshooting

### Previews Not Showing
- Check if Compose is enabled in build.gradle
- Verify preview annotations are correct
- Try "Build > Refresh Previews"
- Check for compilation errors

### Slow Preview Rendering
- Reduce number of items in lists
- Simplify preview data
- Use fixed heights for previews
- Disable interactive mode when not needed

### Preview Crashes
- Check for null pointer exceptions
- Verify all dependencies are available
- Use mock data instead of real services
- Avoid accessing Android context in previews
