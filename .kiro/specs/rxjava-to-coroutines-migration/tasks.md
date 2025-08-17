# Implementation Plan

- [x] 1. Set up coroutines infrastructure and testing framework





  - Create coroutine scope providers for different app layers
  - Set up testing infrastructure with TestCoroutineDispatcher
  - Create error handling utilities for coroutines
  - Write unit tests for coroutine infrastructure components
  - _Requirements: 1.1, 1.3, 7.1, 7.2, 8.1_

- [x] 2. Create coroutine-based network layer foundation






- [x] 2.1 Update Retrofit configuration for coroutines





  - Remove RxJava adapter from Retrofit builder in NetworkHelper
  - Configure Retrofit to use native coroutines support
  - Update OkHttpClient configuration for coroutine compatibility
  - Write unit tests for updated network configuration
  - _Requirements: 3.1, 1.1, 7.1_

- [x] 2.2 Create coroutine-based API interface templates



  - Convert existing API interfaces from RxJava Single/Observable to suspend functions
  - Implement proper error handling for network operations
  - Create Flow-based interfaces for streaming data
  - Write unit tests for API interface conversions
  - _Requirements: 3.1, 3.2, 8.2_

- [x] 2.3 Migrate network error handling to coroutines


  - Replace RxJava error operators with try-catch exception handling
  - Implement coroutine-specific network error handling patterns
  - Update timeout and retry logic using coroutines
  - Write unit tests for error handling scenarios
  - _Requirements: 3.2, 8.2, 7.3_

- [x] 3. Migrate core network operations





- [x] 3.1 Convert WebPageDocumentFetcher to coroutines


  - Replace RxJava observables with suspend functions in WebPageDocumentFetcher
  - Update document fetching logic to use coroutines
  - Implement proper cancellation handling for web requests
  - Write unit tests for document fetching operations
  - _Requirements: 3.1, 3.4, 6.4_

- [x] 3.2 Migrate AppUpdateGithubApi to coroutines


  - Convert GitHub API calls from RxJava to suspend functions
  - Update app update checking logic to use coroutines
  - Implement proper error handling for API failures
  - Write unit tests for GitHub API operations
  - _Requirements: 3.1, 3.2, 7.1_

- [x] 3.3 Update NetworkHelper utility methods


  - Convert utility methods in NetworkHelper to use coroutines
  - Replace reactive network monitoring with coroutine-based alternatives
  - Update connection state checking to use Flow
  - Write unit tests for network utility functions
  - _Requirements: 3.1, 4.4, 7.1_

- [x] 4. Migrate database layer to coroutines




- [x] 4.1 Convert database DAO interfaces to coroutines


  - Replace RxJava observables with Flow in DAO interfaces
  - Convert blocking database operations to suspend functions
  - Update query methods to use appropriate coroutine patterns
  - Write unit tests for DAO interface conversions
  - _Requirements: 4.1, 4.2, 4.4, 7.1_

- [x] 4.2 Update repository pattern for coroutines


  - Convert repository classes to use suspend functions and Flow
  - Implement proper database transaction handling with coroutines
  - Update data caching logic to use coroutine-based patterns
  - Write unit tests for repository operations
  - _Requirements: 4.1, 4.2, 4.3, 7.1_

- [x] 4.3 Migrate database operations in services


  - Update background database operations to use coroutines
  - Convert sync operations to use structured concurrency
  - Implement proper error handling for database failures
  - Write unit tests for service database operations
  - _Requirements: 4.2, 4.3, 6.3, 7.1_

- [x] 5. Migrate UI layer to coroutines




- [x] 5.1 Convert ViewModels to use coroutines


  - Replace RxJava subscriptions with viewModelScope.launch
  - Convert data streams to use StateFlow and SharedFlow
  - Update UI state management to use coroutine patterns
  - Write unit tests for ViewModel coroutine operations
  - _Requirements: 5.1, 5.3, 5.4, 7.1_

- [x] 5.2 Update Activities and Fragments for coroutines


  - Replace RxJava disposables with lifecycleScope coroutines
  - Convert UI update logic to use coroutine patterns
  - Implement proper lifecycle-aware coroutine management
  - Write unit tests for Activity/Fragment coroutine usage
  - _Requirements: 5.1, 5.2, 5.4, 7.1_

- [x] 5.3 Migrate data binding and UI updates


  - Convert observable data binding to use Flow collection
  - Update UI update mechanisms to use coroutines
  - Implement proper error handling for UI operations
  - Write unit tests for UI data binding with coroutines
  - _Requirements: 5.3, 5.4, 7.1_

- [x] 6. Migrate background services and workers




- [x] 6.1 Convert DownloadNovelService to coroutines


  - Replace RxJava observables with coroutines in download service
  - Update download progress reporting to use Flow
  - Implement proper cancellation handling for downloads
  - Write unit tests for download service operations
  - _Requirements: 6.1, 6.3, 6.4, 7.1_

- [x] 6.2 Migrate Firebase messaging service


  - Convert Firebase messaging operations to use coroutines
  - Update notification handling to use coroutine patterns
  - Implement proper background processing with coroutines
  - Write unit tests for messaging service operations
  - _Requirements: 6.1, 6.2, 7.1_

- [x] 6.3 Update WorkManager tasks for coroutines


  - Convert Worker classes to use CoroutineWorker
  - Update background sync operations to use coroutines
  - Implement proper error handling and retry logic
  - Write unit tests for WorkManager coroutine operations
  - _Requirements: 6.2, 6.3, 6.4, 7.1_

- [x] 7. Migrate TTS service to coroutines






- [x] 7.1 Convert TTSService to use coroutines


  - Replace RxJava observables with coroutines in TTS service
  - Update audio playback control to use coroutine patterns
  - Implement proper lifecycle management for TTS operations
  - Write unit tests for TTS service operations
  - _Requirements: 6.1, 6.2, 7.1_

- [x] 7.2 Update TTS controls and UI integration





  - Convert TTS control activities to use coroutines
  - Update media session handling to use coroutine patterns
  - Implement proper state management for TTS controls
  - Write unit tests for TTS control operations
  - _Requirements: 5.1, 5.2, 7.1_
- [x] 8. Update extension system for coroutines







- [ ] 8. Update extension system for coroutines

- [x] 8.1 Migrate extension loading to coroutines


  - Convert extension installation and loading to use coroutines
  - Update extension API calls to use suspend functions
  - Implement proper error handling for extension operations
  - Write unit tests for extension system operations
  - _Requirements: 3.1, 6.2, 7.1_

- [x] 8.2 Update extension communication patterns


  - Convert extension data exchange to use Flow
  - Update extension lifecycle management with coroutines
  - Implement proper cancellation handling for extension operations
  - Write unit tests for extension communication
  - _Requirements: 4.4, 6.4, 7.1_


- [x] 9. Comprehensive testing and validation






- [x] 9.1 Create integration tests for migrated components


  - Write end-to-end tests for complete user workflows
  - Test data flow from network through database to UI
  - Validate error handling across all app layers
  - Test performance and memory usage improvements
  - _Requirements: 7.3, 2.1, 2.2, 2.3_

- [x] 9.2 Perform regression testing


  - Test all existing app functionality with coroutines
  - Validate that user experience remains unchanged
  - Test edge cases and error scenarios
  - Verify background operations work 
correctly
  - _Requirements: 1.4, 2.1, 2.2, 2.4_

- [x] 10. Remove RxJava dependencies and cleanup




- [x] 10.1 Remove RxJava dependencies from build.gradle


  - Remove all RxJava-related dependencies from app/build.gradle
  - Remove Retrofit RxJava adapter dependency
  - Remove ReactiveNetwork dependency
  - Verify app builds successfully without RxJava
  - _Requirements: 1.1, 1.3_

- [x] 10.2 Clean up bridging code and unused imports


  - Remove RxCoroutineBridge.kt file
  - Clean up any remaining RxJava imports
  - Remove unused RxJava-related utility classes
  - Update documentation to reflect coroutines usage
  - _Requirements: 1.1, 1.3, 8.4_

- [x] 10.3 Final optimization and code review


  - Optimize coroutine usage patterns across the app
  - Review and improve error handling implementations
  - Ensure proper structured concurrency throughout
  - Conduct final code review for coroutines best practices
  - _Requirements: 8.1, 8.2, 8.3, 8.4_