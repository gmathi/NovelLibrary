# Implementation Plan

- [ ] 1. Setup Navigation Component foundation
  - Create new MainActivity extending BaseActivity with @AndroidEntryPoint and Navigation Component integration
  - Add Navigation Component dependencies and configure Safe Args plugin
  - Setup navigation graph structure with existing fragment destinations
  - Create NavigationManager as @Singleton Hilt component for centralized navigation logic
  - _Requirements: 1.1, 1.2, 4.2, 10.3_

- [ ] 2. Create MainActivity with Navigation Component
- [ ] 2.1 Implement MainActivity structure
  - Create MainActivity extending BaseActivity with @AndroidEntryPoint annotation
  - Implement MainActivity layout with NavHostFragment, existing toolbar, and drawer integration
  - Write MainViewModel using existing @HiltViewModel pattern with BaseViewModel
  - Setup Navigation Component with existing drawer navigation structure
  - _Requirements: 1.1, 2.1, 3.2_

- [ ] 2.2 Setup navigation graph and Safe Args
  - Create navigation graph XML with existing fragment destinations (library, search, extensions, settings)
  - Configure Safe Args plugin for type-safe navigation parameters
  - Write navigation actions connecting existing fragments
  - Implement deep link handling using existing intent patterns
  - _Requirements: 1.3, 4.2, 10.5_

- [ ] 2.3 Create navigation utilities
  - Implement NavigationManager as @Singleton Hilt component
  - Create DeepLinkHandler using existing Hilt injection patterns
  - Write navigation extension functions for common navigation patterns
  - Implement back stack management utilities
  - _Requirements: 1.2, 1.3, 4.4_

- [ ] 3. Update existing fragments for Navigation Component
- [ ] 3.1 Update LibraryFragment for Navigation Component
  - Modify existing LibraryFragment to work with Navigation Component navigation
  - Create LibraryViewModel using existing @HiltViewModel pattern and BaseViewModel
  - Implement LibraryUiState using existing UiState sealed class patterns
  - Update existing RecyclerView adapter to use Navigation Component for navigation
  - Integrate with existing DBHelper using Hilt injection
  - _Requirements: 3.1, 3.2, 6.1, 10.1_

- [ ] 3.2 Update LibraryPagerFragment integration
  - Modify existing LibraryPagerFragment to work within Navigation Component
  - Update tab navigation to work with Navigation Component structure
  - Preserve existing library search and filter functionality
  - Maintain existing novel selection and bulk operations
  - Keep integration with existing sync functionality
  - _Requirements: 5.3, 2.3, 10.2_

- [ ] 3.3 Update SearchFragment for Navigation Component
  - Modify existing SearchFragment to use Navigation Component navigation
  - Create SearchViewModel using existing @HiltViewModel and BaseViewModel patterns
  - Update search results navigation to use Navigation Component actions
  - Preserve existing search functionality and PersistentSearchView integration
  - Maintain existing search history and suggestions
  - _Requirements: 4.2, 6.1, 10.1_

- [ ] 4. Convert NovelDetailsActivity to fragment
- [ ] 4.1 Create NovelDetailsFragment from activity
  - Convert NovelDetailsActivity to NovelDetailsFragment extending BaseFragment
  - Implement NovelDetailsViewModel using @HiltViewModel pattern with existing BaseViewModel
  - Create layout fragment_novel_details.xml based on existing activity layout
  - Update UI binding to use ViewBinding following architecture guide patterns
  - Integrate with existing DBHelper and NetworkHelper using Hilt injection
  - _Requirements: 5.1, 3.1, 4.1, 10.1_

- [ ] 4.2 Implement novel details navigation
  - Add NovelDetailsFragment to navigation graph with Safe Args for novel ID parameter
  - Update library and search fragments to navigate to novel details using Navigation Component
  - Implement navigation to chapters and reader fragments from novel details
  - Handle novel details deep links through Navigation Component
  - _Requirements: 1.2, 4.2, 10.5_

- [ ] 4.3 Preserve novel details functionality
  - Maintain existing novel information display and metadata loading
  - Keep existing library management actions (add/remove from library)
  - Preserve existing novel image loading and caching
  - Maintain existing novel description and genre display
  - Keep existing novel rating and status functionality
  - _Requirements: 5.1, 5.3_

- [ ] 5. Convert ChaptersPagerActivity to fragment
- [ ] 5.1 Create ChaptersFragment from activity
  - Convert ChaptersPagerActivity to ChaptersFragment extending BaseFragment
  - Update existing ChaptersFragment to work as main chapters destination
  - Implement ChaptersViewModel using existing @HiltViewModel pattern
  - Create layout based on existing activity layout with ViewBinding
  - Integrate with existing DBHelper for chapter loading using Hilt injection
  - _Requirements: 5.4, 3.1, 4.1, 10.1_

- [ ] 5.2 Update chapters navigation and functionality
  - Add ChaptersFragment to navigation graph with Safe Args for novel ID parameter
  - Update navigation from novel details to chapters using Navigation Component
  - Implement navigation from chapters to reader fragment
  - Preserve existing chapter list display with read status and download indicators
  - Maintain existing chapter selection and bulk operations
  - _Requirements: 1.2, 4.2, 5.4_

- [ ] 5.3 Preserve chapters functionality
  - Keep existing chapter download progress tracking
  - Maintain existing chapter sorting and filtering options
  - Preserve existing chapter context menu actions
  - Keep existing chapter read status management
  - Maintain integration with existing DownloadNovelService
  - _Requirements: 5.4, 6.1_

- [ ] 6. Convert ReaderDBPagerActivity to fragment
- [ ] 6.1 Create ReaderFragment from activity
  - Convert ReaderDBPagerActivity to ReaderFragment extending BaseFragment
  - Implement ReaderViewModel using @HiltViewModel pattern with existing BaseViewModel
  - Create layout fragment_reader.xml based on existing activity layout with ViewBinding
  - Preserve existing WebView-based content display and reader controls
  - Integrate with existing DBHelper and reader settings using Hilt injection
  - _Requirements: 5.2, 3.1, 4.1, 10.1_

- [ ] 6.2 Update reader navigation and functionality
  - Add ReaderFragment to navigation graph with Safe Args for novel ID and chapter ID parameters
  - Update navigation from chapters and novel details to reader using Navigation Component
  - Implement reader navigation between chapters within the fragment
  - Preserve existing chapter pagination and smooth scrolling functionality
  - Maintain existing reading progress tracking and persistence
  - _Requirements: 1.2, 4.2, 5.2_

- [ ] 6.3 Integrate existing TTS functionality
  - Preserve integration with existing TTS service (@AndroidEntryPoint)
  - Maintain existing TTS controls UI within reader fragment
  - Keep existing TTS state management and playback controls
  - Preserve existing TTS settings and voice configuration
  - Maintain existing TTS notification and media session integration
  - _Requirements: 5.6, 6.1_

- [ ] 7. Convert settings activities to fragments
- [ ] 7.1 Create settings navigation graph
  - Create nested navigation graph for settings with all settings destinations
  - Add settings navigation graph to main navigation graph
  - Implement settings entry point navigation from main drawer/menu
  - Configure Safe Args for settings navigation parameters
  - _Requirements: 1.1, 1.2, 4.2_

- [ ] 7.2 Convert core settings activities to fragments
  - Convert GeneralSettingsActivity to GeneralSettingsFragment extending BaseFragment
  - Convert ReaderSettingsActivity to ReaderSettingsFragment extending BaseFragment
  - Convert BackupSettingsActivity to BackupSettingsFragment extending BaseFragment
  - Convert SyncSettingsActivity to SyncSettingsFragment extending BaseFragment
  - Convert TTSSettingsActivity to TTSSettingsFragment extending BaseFragment
  - _Requirements: 5.1, 5.7, 10.1_

- [ ] 7.3 Update settings with existing infrastructure
  - Use existing DataCenter for preference management with Hilt injection
  - Preserve existing settings validation and default value management
  - Maintain existing settings backup and restore functionality using existing services
  - Keep existing settings UI and preference handling patterns
  - Integrate with existing GoogleBackupViewModel using @HiltViewModel pattern
  - _Requirements: 3.4, 3.5, 5.7_

- [ ] 8. Convert NovelDownloadsActivity to fragment
- [ ] 8.1 Create DownloadsFragment from activity
  - Convert NovelDownloadsActivity to DownloadsFragment extending BaseFragment
  - Implement DownloadsViewModel using @HiltViewModel pattern with existing BaseViewModel
  - Create layout fragment_downloads.xml based on existing activity layout with ViewBinding
  - Preserve existing download queue display and progress tracking UI
  - Integrate with existing DBHelper and download management using Hilt injection
  - _Requirements: 5.4, 3.1, 4.1, 10.1_

- [ ] 8.2 Update downloads navigation and preserve functionality
  - Add DownloadsFragment to navigation graph as destination from main drawer/menu
  - Preserve existing download queue management and prioritization
  - Maintain existing download cancellation and retry functionality
  - Keep integration with existing DownloadNovelService (@AndroidEntryPoint)
  - Preserve existing download progress broadcasting and UI updates
  - _Requirements: 1.2, 5.4, 6.1_

- [ ] 9. Convert ExtensionsPagerActivity to fragment navigation
- [ ] 9.1 Update ExtensionsFragment for Navigation Component
  - Modify existing ExtensionsFragment to work as main extensions destination
  - Convert ExtensionsPagerActivity pager functionality to Navigation Component structure
  - Update existing ExtensionsFragment to use Navigation Component navigation
  - Preserve existing extension list display and management UI
  - Maintain integration with existing ExtensionManager using Hilt injection
  - _Requirements: 5.5, 3.1, 10.2_

- [ ] 9.2 Preserve extensions functionality with existing infrastructure
  - Keep existing extension installation and update functionality
  - Maintain existing extension source management and configuration
  - Preserve existing extension testing and validation using existing ExtensionManager
  - Keep integration with existing extension security validation
  - Maintain existing extension configuration and settings management
  - _Requirements: 3.4, 5.5, 10.2_

- [ ] 10. Convert remaining activities to fragments
- [ ] 10.1 Convert ImagePreviewActivity to fragment
  - Convert ImagePreviewActivity to ImagePreviewFragment extending BaseFragment
  - Create layout fragment_image_preview.xml with ViewBinding
  - Preserve existing image zoom and pan functionality
  - Maintain existing image sharing and saving options
  - Keep existing image loading optimization and caching
  - _Requirements: 5.1, 4.1, 10.1_

- [ ] 10.2 Convert WebViewActivity to fragment
  - Convert WebViewActivity to WebViewFragment extending BaseFragment
  - Create layout fragment_webview.xml with ViewBinding
  - Preserve existing WebView configuration and security settings
  - Maintain existing JavaScript injection and content manipulation
  - Keep existing WebView navigation and history management
  - _Requirements: 5.1, 4.1, 10.1_

- [ ] 11. Update deep link and intent handling
- [ ] 11.1 Migrate deep link processing to MainActivity
  - Update AndroidManifest.xml to route all deep links to MainActivity instead of individual activities
  - Implement deep link processing in MainActivity onCreate using existing intent handling patterns
  - Create DeepLinkHandler as @Singleton Hilt component using existing patterns
  - Write deep link navigation to appropriate fragments using Navigation Component
  - Test all existing deep link patterns with new single activity structure
  - _Requirements: 1.4, 9.2, 9.3, 10.5_

- [ ] 11.2 Handle external intents in single activity
  - Update intent filters to work with single activity architecture
  - Implement external intent processing (sharing, file opening) in MainActivity
  - Preserve existing intent data extraction and validation logic
  - Create intent-based navigation to appropriate fragments using Navigation Component
  - Test external app integration scenarios (sharing from other apps, file associations)
  - _Requirements: 9.3, 9.4, 10.5_

- [ ] 12. Implement testing using existing Hilt infrastructure
- [ ] 12.1 Write unit tests using existing test infrastructure
  - Create unit tests for new ViewModels using existing @HiltAndroidTest and HiltAndroidRule
  - Write unit tests for NavigationManager and DeepLinkHandler using existing mock providers
  - Implement unit tests for fragment navigation logic using existing test utilities
  - Create unit tests for Navigation Component integration using existing patterns
  - Write unit tests for state management using existing UiState testing patterns
  - _Requirements: 7.1, 7.2, 7.3_

- [ ] 12.2 Write integration tests using Hilt test modules
  - Create integration tests for fragment navigation flows using existing @TestInstallIn modules
  - Write integration tests for MainActivity and Navigation Component using existing BaseHiltTest
  - Implement integration tests for deep link handling using existing test infrastructure
  - Create integration tests for fragment lifecycle with Navigation Component
  - Write integration tests for external intent handling in single activity
  - _Requirements: 7.1, 7.4_

- [ ] 12.3 Implement UI tests with Navigation Component
  - Create UI tests for main navigation flows using Navigation Component testing utilities
  - Write UI tests for fragment transitions and back stack management
  - Implement UI tests for deep link navigation scenarios
  - Create UI tests for drawer navigation integration with Navigation Component
  - Write UI tests for Safe Args parameter passing between fragments
  - _Requirements: 7.5_

- [ ] 13. Performance validation and optimization
- [ ] 13.1 Validate memory usage with existing monitoring
  - Ensure proper fragment lifecycle management using existing BaseFragment patterns
  - Validate no memory leaks using existing performance monitoring infrastructure
  - Verify image loading and caching performance using existing Glide configuration
  - Check database query performance using existing DBHelper patterns
  - Validate background task performance using existing coroutines infrastructure
  - _Requirements: 8.1, 8.4_

- [ ] 13.2 Optimize Navigation Component performance
  - Implement efficient fragment back stack management
  - Optimize fragment transition animations for smooth navigation
  - Validate navigation performance compared to activity-based navigation
  - Implement lazy loading strategies for heavy fragments where needed
  - Monitor startup performance with Navigation Component integration
  - _Requirements: 8.2, 8.3_

- [ ] 14. Migration safety using existing infrastructure
- [ ] 14.1 Implement gradual migration approach
  - Create feature flag in existing DataCenter for enabling single activity architecture
  - Use existing Firebase Analytics for monitoring migration success
  - Implement gradual rollout strategy starting with less critical screens
  - Create migration monitoring using existing error handling and logging infrastructure
  - _Requirements: 9.1, 9.4_

- [ ] 14.2 Ensure data preservation and rollback safety
  - Validate that existing DataCenter preferences are preserved during migration
  - Ensure existing backup and restore functionality continues to work
  - Create migration validation using existing data validation patterns
  - Implement rollback procedures that can revert to activity-based navigation
  - Use existing error reporting infrastructure for migration monitoring
  - _Requirements: 9.1, 9.5_

- [ ] 15. Documentation and final integration
- [ ] 15.1 Update architecture documentation
  - Update existing architecture documentation to reflect Navigation Component integration
  - Create Navigation Component usage guide building on existing Hilt and ViewBinding patterns
  - Document migration from activities to fragments following established patterns
  - Write troubleshooting guide for Navigation Component integration issues
  - Update existing developer onboarding guide with Navigation Component patterns
  - _Requirements: 11.1, 11.2, 11.3, 11.4_

- [ ] 15.2 Final integration and validation
  - Integrate all converted fragments into single MainActivity with Navigation Component
  - Write end-to-end testing for complete user flows using existing test infrastructure
  - Validate performance compared to multi-activity architecture using existing monitoring
  - Create final validation that all existing functionality is preserved
  - Prepare deployment with existing CI/CD and release infrastructure
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 11.5_