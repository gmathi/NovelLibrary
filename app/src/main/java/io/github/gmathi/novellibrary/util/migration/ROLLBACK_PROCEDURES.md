# Hilt Migration Rollback Procedures

This document provides comprehensive procedures for rolling back the Hilt migration in case of issues or failures.

## Overview

The rollback system provides multiple mechanisms to revert from Hilt back to Injekt dependency injection:

1. **Emergency Rollback** - Immediate reversion of all components to Injekt
2. **Selective Rollback** - Revert specific components while keeping others on Hilt
3. **Checkpoint Restoration** - Restore to a previously saved migration state
4. **Last Successful State** - Revert to the last known working configuration

## Emergency Rollback

### When to Use
- Critical application crashes related to dependency injection
- Widespread functionality failures after Hilt migration
- Production issues requiring immediate resolution

### Procedure

#### Programmatic Emergency Rollback
```kotlin
@Inject
lateinit var rollbackManager: MigrationRollbackManager

// Perform emergency rollback
val result = rollbackManager.performEmergencyRollback("Critical production issue")

when (result) {
    is RollbackResult.Success -> {
        // All components reverted to Injekt
        // Application should be stable
        Log.i("Migration", "Emergency rollback successful: ${result.message}")
    }
    is RollbackResult.Failure -> {
        // Rollback failed - manual intervention required
        Log.e("Migration", "Emergency rollback failed: ${result.error}")
    }
}
```

#### Manual Emergency Rollback
If programmatic rollback fails, use manual steps:

1. **Disable Migration Globally**
   ```kotlin
   featureFlags.setMigrationEnabled(false)
   ```

2. **Activate Rollback Mode**
   ```kotlin
   featureFlags.setRollbackMode(true)
   ```

3. **Restart Application**
   - Force close the application
   - Clear app cache if necessary
   - Restart to ensure Injekt is used

### Verification
After emergency rollback:
- Verify `featureFlags.isRollbackMode()` returns `true`
- Confirm all Hilt flags are disabled
- Test critical app functionality
- Check logs for Injekt dependency resolution

## Selective Rollback

### When to Use
- Specific component types causing issues (e.g., only Services)
- Gradual rollback to isolate problematic areas
- Maintaining partial Hilt benefits while fixing issues

### Procedure

```kotlin
// Rollback specific components
val componentsToRollback = listOf("Services", "Workers")
val result = rollbackManager.performSelectiveRollback(
    components = componentsToRollback,
    reason = "Service injection causing memory leaks"
)

// Verify selective rollback
val status = injektHiltBridge.getMigrationStatus()
// Check that only specified components are reverted
```

### Available Components
- `"ViewModels"` - All ViewModel classes
- `"Activities"` - All Activity classes  
- `"Fragments"` - All Fragment classes
- `"Services"` - All Service classes
- `"Workers"` - All Worker classes
- `"Repositories"` - All repository and data layer components

## Checkpoint Restoration

### Creating Checkpoints
Create checkpoints at stable migration points:

```kotlin
// Before migrating a new component type
rollbackManager.createMigrationCheckpoint("before_services_migration")

// After successful component migration
rollbackManager.createMigrationCheckpoint("after_viewmodels_stable")

// Before major changes
rollbackManager.createMigrationCheckpoint("pre_production_deployment")
```

### Restoring from Checkpoint
```kotlin
val result = rollbackManager.restoreFromCheckpoint("before_services_migration")

when (result) {
    is RollbackResult.Success -> {
        // Migration state restored to checkpoint
        // Continue from stable point
    }
    is RollbackResult.Failure -> {
        // Checkpoint restoration failed
        // Try alternative rollback method
    }
}
```

### Managing Checkpoints
```kotlin
// Get available checkpoints
val history = rollbackManager.getRollbackHistory()
val checkpoints = history.availableCheckpoints

// List all checkpoints
checkpoints.forEach { checkpoint ->
    Log.i("Migration", "Available checkpoint: $checkpoint")
}
```

## Last Successful State Restoration

### Marking Successful States
Mark stable states for future rollback:

```kotlin
// After successful migration phase
rollbackManager.markCurrentStateAsSuccessful()

// After production validation
rollbackManager.markCurrentStateAsSuccessful()
```

### Restoring to Last Successful State
```kotlin
val result = rollbackManager.restoreToLastSuccessfulState()

if (result is RollbackResult.Success) {
    // Restored to last known good configuration
    // Validate functionality before proceeding
}
```

## Rollback Validation

### Automatic Validation
The rollback system automatically validates:
- Rollback mode activation
- Hilt flag states
- Injekt dependency availability
- Component state consistency

### Manual Validation Steps

1. **Check Feature Flags**
   ```kotlin
   val flags = featureFlags.getAllFlags()
   flags.forEach { (flag, enabled) ->
       Log.d("Migration", "$flag: $enabled")
   }
   ```

2. **Verify Dependency Resolution**
   ```kotlin
   // Test critical dependencies
   val dbHelper = injektHiltBridge.getDBHelper()
   val dataCenter = injektHiltBridge.getDataCenter()
   // Ensure they resolve correctly
   ```

3. **Test Core Functionality**
   - Launch main screens
   - Test data loading
   - Verify network operations
   - Check background services

## Troubleshooting Rollback Issues

### Rollback Fails to Activate

**Symptoms:**
- `performEmergencyRollback()` returns failure
- Hilt flags remain enabled
- Application still crashes

**Solutions:**
1. **Force Rollback Mode**
   ```kotlin
   // Directly activate rollback mode
   featureFlags.setRollbackMode(true)
   
   // Manually disable all Hilt flags
   featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, false)
   featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, false)
   // ... repeat for all flags
   ```

2. **Clear Migration Preferences**
   ```kotlin
   // Reset all migration state
   featureFlags.resetToDefaults()
   featureFlags.setRollbackMode(true)
   ```

3. **Application Restart**
   - Force close application
   - Clear application cache
   - Restart application

### Injekt Dependencies Not Available

**Symptoms:**
- Rollback succeeds but app still crashes
- "No binding found" errors for Injekt
- Dependencies return null

**Solutions:**
1. **Verify Injekt Initialization**
   ```kotlin
   // In Application.onCreate()
   if (featureFlags.isRollbackMode() || !featureFlags.isMigrationEnabled()) {
       // Ensure Injekt is initialized
       Injekt = InjektScope(DefaultRegistrar())
       Injekt.importModule(AppModule(this))
   }
   ```

2. **Check AppModule Configuration**
   - Ensure AppModule.kt exists and is complete
   - Verify all required dependencies are registered
   - Test Injekt dependency resolution

### Partial Rollback Issues

**Symptoms:**
- Some components use Hilt, others use Injekt
- Inconsistent dependency instances
- Data synchronization issues

**Solutions:**
1. **Validate Dependency Parity**
   ```kotlin
   val validationResult = injektHiltBridge.validateDependencyParity()
   if (!validationResult.isValid) {
       // Address dependency inconsistencies
       validationResult.issues.forEach { issue ->
           Log.e("Migration", "Dependency issue: $issue")
       }
   }
   ```

2. **Force Complete Rollback**
   ```kotlin
   // If partial rollback causes issues, go full rollback
   rollbackManager.performEmergencyRollback("Partial rollback instability")
   ```

## Monitoring and Logging

### Rollback History
```kotlin
val history = rollbackManager.getRollbackHistory()

Log.i("Migration", "Rollback Statistics:")
Log.i("Migration", "  Total rollbacks: ${history.rollbackCount}")
Log.i("Migration", "  Last reason: ${history.lastRollbackReason}")
Log.i("Migration", "  Last timestamp: ${Date(history.lastRollbackTimestamp)}")
Log.i("Migration", "  Has successful state: ${history.hasLastSuccessfulState}")
Log.i("Migration", "  Available checkpoints: ${history.availableCheckpoints}")
```

### Migration Status Monitoring
```kotlin
val status = injektHiltBridge.getMigrationStatus()

Log.i("Migration", "Current Migration Status:")
Log.i("Migration", "  Global migration enabled: ${status.isGlobalMigrationEnabled}")
Log.i("Migration", "  Rollback mode: ${status.isRollbackMode}")
status.componentStatus.forEach { (component, usingHilt) ->
    val framework = if (usingHilt) "Hilt" else "Injekt"
    Log.i("Migration", "  $component: $framework")
}
```

## Recovery Procedures

### After Successful Rollback

1. **Identify Root Cause**
   - Review logs for error patterns
   - Analyze crash reports
   - Identify problematic components

2. **Fix Issues**
   - Address dependency injection problems
   - Fix component-specific issues
   - Update migration approach

3. **Gradual Re-migration**
   ```kotlin
   // Start with stable components
   featureFlags.setRollbackMode(false)
   featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES, true)
   
   // Test thoroughly before proceeding
   // Add components incrementally
   ```

### Production Rollback Checklist

- [ ] Identify and document the issue
- [ ] Perform appropriate rollback (emergency/selective)
- [ ] Validate application stability
- [ ] Monitor error rates and user reports
- [ ] Communicate status to stakeholders
- [ ] Plan fix and re-migration strategy
- [ ] Document lessons learned

## Best Practices

1. **Create Checkpoints Frequently**
   - Before each component migration
   - After successful testing phases
   - Before production deployments

2. **Test Rollback Procedures**
   - Regularly test rollback mechanisms
   - Validate in staging environments
   - Ensure team familiarity with procedures

3. **Monitor Migration Health**
   - Track rollback frequency
   - Monitor error patterns
   - Maintain rollback documentation

4. **Gradual Migration Approach**
   - Migrate components incrementally
   - Validate each phase thoroughly
   - Maintain rollback readiness

## Emergency Contacts

In case of critical production issues:

1. **Immediate Actions**
   - Perform emergency rollback
   - Monitor application stability
   - Document the incident

2. **Escalation**
   - Notify development team
   - Engage DevOps for deployment rollback if needed
   - Coordinate with QA for validation

3. **Communication**
   - Update stakeholders on status
   - Provide timeline for resolution
   - Document post-mortem findings