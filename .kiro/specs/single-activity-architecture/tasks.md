# Implementation Plan

- [ ] 1. Setup foundation architecture and base classes
  - Create new MainActivity with Navigation Component integration
  - Implement BaseFragment and BaseViewModel abstract classes with common functionality
  - Setup navigation graph structure with placeholder destinations
  - Create UI state management classes (UiState sealed classes)
  - Update dependency injection modules for new architecture patterns
  - _Requirements: 1.1, 1.2, 3.1, 3.2, 3.3_

- [ ] 2. Implement core navigation infrastructure
- [ ] 2.1 Create Navigation Component setup
  - Write navigation graph XML with main destinations (library, search, settings)
  - Implement NavigationManager class for centralized navigation logic
  - Create DeepLinkHandler for processing external intents and notifications
  - Write navigation actions and safe args for type-safe navigation
  - _Requirements: 1.1, 1.3, 9.2_

- [ ] 2.2 Implement global UI management
  - Create MainActivity layout with NavHostFragment, toolbar, and drawer
  - Implement toolbar management system for fragment-specific toolbars
  - Write drawer navigation integration with Navigation Component
  - Create bottom navigation setup (if needed for future use)
  - _Requirements: 2.1, 2.2_

- [ ] 2.3 Create state management foundation
  - Implement UiState sealed classes for different UI states (Loading, Success, Error)
  - Write ErrorHandler class for consistent error processing
  - Create GlobalErrorHandler for app-wide error management
  - Implement state persistence utilities for configuration changes
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 3. Migrate core library functionality
- [ ] 3.1 Create LibraryFragment and ViewModel
  - Write LibraryFragment extending BaseFragment with RecyclerView setup
  - Implement LibraryViewModel with novel loading and state management
  - Create LibraryUiState sealed class for library-specific states
  - Write novel list adapter with ViewBinding and click handling
  - Implement pull-to-refresh and loading states
  - _Requirements: 5.3, 6.1, 6.4_

- [ ] 3.2 Implement library business logic
  - Create GetLibraryNovelsUseCase for domain layer business logic
  - Write NovelRepository interface and implementation
  - Implement novel data mapping between database and domain models
  - Create library novel filtering and sorting logic
  - Write unit tests for LibraryViewModel and use cases
  - _Requirements: 3.1, 3.2, 3.4, 7.1, 7.2_

- [ ] 3.3 Migrate LibraryPagerFragment functionality
  - Extract pager functionality from existing LibraryPagerFragment
  - Implement tab-based navigation within library (if needed)
  - Write library search and filter functionality
  - Create novel selection and bulk operations
  - Integrate with existing sync functionality
  - _Requirements: 5.3, 2.3_

- [ ] 4. Migrate search functionality
- [ ] 4.1 Create SearchFragment and ViewModel
  - Write SearchFragment with search UI and PersistentSearchView integration
  - Implement SearchViewModel with search state management
  - Create SearchUiState for search results and loading states
  - Write search results adapter with novel display
  - Implement search history and suggestions
  - _Requirements: 5.1, 6.1, 6.4_

- [ ] 4.2 Implement search business logic
  - Create SearchNovelsUseCase for search operations
  - Write search repository with multiple source support
  - Implement search result caching and pagination
  - Create search query validation and processing
  - Write unit tests for search functionality
  - _Requirements: 3.1, 3.2, 7.1_

- [ ] 4.3 Integrate search with navigation
  - Implement navigation from search results to novel details
  - Write search URL handling for external search links
  - Create search term fragment integration
  - Implement source-specific search functionality
  - _Requirements: 2.1, 2.2, 9.2_

- [ ] 5. Create novel details and chapters functionality
- [ ] 5.1 Migrate NovelDetailsActivity to fragment
  - Create NovelDetailsFragment with novel information display
  - Implement NovelDetailsViewModel with novel loading and actions
  - Write novel details UI with image loading, description, and metadata
  - Create action buttons for library management and reading
  - Implement novel details state management
  - _Requirements: 5.1, 5.3, 6.1_

- [ ] 5.2 Implement chapters functionality
  - Create ChaptersFragment for chapter list display
  - Write ChaptersViewModel with chapter loading and management
  - Implement chapter list adapter with read status and download indicators
  - Create chapter selection and bulk operations
  - Write chapter download progress tracking
  - _Requirements: 5.4, 6.1, 6.4_

- [ ] 5.3 Create novel and chapter business logic
  - Write GetNovelDetailsUseCase and GetChaptersUseCase
  - Implement ChapterRepository with read status and download management
  - Create novel metadata updating and syncing logic
  - Write chapter content loading and caching
  - Implement unit tests for novel and chapter functionality
  - _Requirements: 3.1, 3.2, 7.1, 7.2_

- [ ] 6. Migrate reader functionality
- [ ] 6.1 Create ReaderFragment from ReaderDBPagerActivity
  - Write ReaderFragment with WebView-based content display
  - Implement ReaderViewModel with chapter content and navigation
  - Create reader UI controls (font size, theme, navigation)
  - Write chapter pagination and smooth scrolling
  - Implement reading progress tracking and persistence
  - _Requirements: 5.2, 6.1, 6.4_

- [ ] 6.2 Implement reader business logic
  - Create ReadChapterUseCase and UpdateReadingProgressUseCase
  - Write reader settings management and persistence
  - Implement chapter content processing and cleaning
  - Create reading statistics and progress calculation
  - Write unit tests for reader functionality
  - _Requirements: 3.1, 3.2, 5.2, 7.1_

- [ ] 6.3 Integrate TTS functionality
  - Migrate TTS service integration to work with fragment-based reader
  - Create TTS controls UI within reader fragment
  - Implement TTS state management and playback controls
  - Write TTS settings and voice configuration
  - Create TTS notification and media session integration
  - _Requirements: 5.6, 6.1_

- [ ] 7. Migrate settings architecture
- [ ] 7.1 Create settings navigation graph
  - Write settings navigation graph with all settings destinations
  - Create MainSettingsFragment as entry point for settings
  - Implement settings category navigation structure
  - Write settings fragment base class for common functionality
  - _Requirements: 1.1, 1.2, 2.1_

- [ ] 7.2 Migrate core settings fragments
  - Create GeneralSettingsFragment from GeneralSettingsActivity
  - Write ReaderSettingsFragment from ReaderSettingsActivity
  - Implement BackupSettingsFragment from BackupSettingsActivity
  - Create SyncSettingsFragment from SyncSettingsActivity
  - Write TTSSettingsFragment from TTSSettingsActivity
  - _Requirements: 5.1, 5.7_

- [ ] 7.3 Implement settings business logic
  - Create settings use cases for preference management
  - Write settings repository for preference persistence
  - Implement settings validation and default value management
  - Create settings backup and restore functionality
  - Write unit tests for settings functionality
  - _Requirements: 3.1, 3.2, 7.1_

- [ ] 8. Migrate downloads functionality
- [ ] 8.1 Create DownloadsFragment from NovelDownloadsActivity
  - Write DownloadsFragment with download queue display
  - Implement DownloadsViewModel with download state management
  - Create download progress tracking and UI updates
  - Write download queue management and prioritization
  - Implement download cancellation and retry functionality
  - _Requirements: 5.4, 6.1, 6.4_

- [ ] 8.2 Integrate download service
  - Update DownloadNovelService to work with fragment-based architecture
  - Create download notification management
  - Implement download progress broadcasting to fragments
  - Write download completion handling and user feedback
  - Create download error handling and recovery
  - _Requirements: 5.4, 6.2_

- [ ] 9. Migrate extensions functionality
- [ ] 9.1 Create ExtensionsFragment from ExtensionsPagerActivity
  - Write ExtensionsFragment with extension list display
  - Implement ExtensionsViewModel with extension management
  - Create extension installation and update UI
  - Write extension source management and configuration
  - Implement extension testing and validation
  - _Requirements: 5.5, 6.1_

- [ ] 9.2 Implement extension business logic
  - Create extension management use cases
  - Write extension repository for installation and updates
  - Implement extension source validation and security checks
  - Create extension configuration and settings management
  - Write unit tests for extension functionality
  - _Requirements: 3.1, 3.2, 5.5, 7.1_

- [ ] 10. Implement advanced UI features
- [ ] 10.1 Create image preview functionality
  - Write ImagePreviewFragment to replace ImagePreviewActivity
  - Implement image zoom and pan functionality
  - Create image sharing and saving options
  - Write image loading optimization and caching
  - _Requirements: 5.1, 8.1_

- [ ] 10.2 Implement WebView functionality
  - Create WebViewFragment to replace WebViewActivity
  - Write WebView configuration and security settings
  - Implement JavaScript injection and content manipulation
  - Create WebView navigation and history management
  - _Requirements: 5.1, 9.2_

- [ ] 11. Update deep link and intent handling
- [ ] 11.1 Migrate deep link processing
  - Update AndroidManifest.xml to route all deep links to MainActivity
  - Implement deep link processing in MainActivity onCreate
  - Write deep link navigation to appropriate fragments
  - Create deep link validation and security checks
  - Test all existing deep link patterns
  - _Requirements: 1.4, 9.2, 9.3_

- [ ] 11.2 Handle external intents
  - Update intent filters to work with single activity architecture
  - Implement external intent processing (sharing, file opening)
  - Write intent data extraction and validation
  - Create intent-based navigation to appropriate fragments
  - Test external app integration scenarios
  - _Requirements: 9.3, 9.4_

- [ ] 12. Implement comprehensive testing
- [ ] 12.1 Write unit tests
  - Create unit tests for all ViewModels with mock dependencies
  - Write unit tests for use cases and business logic
  - Implement unit tests for repositories and data sources
  - Create unit tests for navigation and state management
  - Write unit tests for error handling and edge cases
  - _Requirements: 7.1, 7.2, 7.3_

- [ ] 12.2 Write integration tests
  - Create integration tests for fragment navigation flows
  - Write integration tests for database operations
  - Implement integration tests for network operations
  - Create integration tests for service integration
  - Write integration tests for external intent handling
  - _Requirements: 7.1, 7.4_

- [ ] 12.3 Implement UI tests
  - Create UI tests for main navigation flows
  - Write UI tests for fragment interactions and state changes
  - Implement UI tests for complex user scenarios
  - Create UI tests for error states and recovery
  - Write UI tests for accessibility and usability
  - _Requirements: 7.5_

- [ ] 13. Performance optimization and monitoring
- [ ] 13.1 Optimize memory usage
  - Implement proper fragment lifecycle management
  - Write memory leak detection and prevention
  - Optimize image loading and caching strategies
  - Create efficient database query patterns
  - Implement background task optimization
  - _Requirements: 8.1, 8.4_

- [ ] 13.2 Optimize navigation performance
  - Implement fragment caching for frequently accessed screens
  - Write lazy loading for heavy fragments and data
  - Optimize fragment transition animations
  - Create efficient back stack management
  - Implement preloading strategies for better UX
  - _Requirements: 8.2, 8.3_

- [ ] 14. Migration safety and rollback
- [ ] 14.1 Implement feature flags
  - Create feature flag system for gradual rollout
  - Write feature flag configuration and management
  - Implement A/B testing infrastructure for architecture comparison
  - Create feature flag monitoring and analytics
  - _Requirements: 9.1, 9.4_

- [ ] 14.2 Create migration utilities
  - Write data migration scripts for user preferences
  - Implement backup and restore functionality for migration
  - Create migration validation and verification tools
  - Write rollback procedures and safety measures
  - Implement migration monitoring and error reporting
  - _Requirements: 9.1, 9.5_

- [ ] 15. Documentation and final integration
- [ ] 15.1 Create comprehensive documentation
  - Write architecture documentation with diagrams and examples
  - Create migration guide for developers
  - Implement code documentation and inline comments
  - Write troubleshooting guide and FAQ
  - Create architecture decision records (ADRs)
  - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [ ] 15.2 Final integration and testing
  - Integrate all migrated components into single activity
  - Write end-to-end testing for complete user flows
  - Implement performance benchmarking and comparison
  - Create final validation of all existing functionality
  - Write deployment and release preparation
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_