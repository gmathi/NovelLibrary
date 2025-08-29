package io.github.gmathi.novellibrary.util.error

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gmathi.novellibrary.util.Logs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive troubleshooting guide for common Hilt migration issues
 * Provides step-by-step solutions and code examples
 */
@Singleton
class HiltTroubleshootingGuide @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "HiltTroubleshootingGuide"
    }
    
    /**
     * Get troubleshooting information for a specific error type
     */
    fun getTroubleshootingInfo(errorType: HiltErrorType): TroubleshootingInfo {
        return when (errorType) {
            HiltErrorType.MISSING_BINDING -> getMissingBindingGuide()
            HiltErrorType.CIRCULAR_DEPENDENCY -> getCircularDependencyGuide()
            HiltErrorType.WRONG_SCOPE -> getWrongScopeGuide()
            HiltErrorType.MISSING_ENTRY_POINT -> getMissingEntryPointGuide()
            HiltErrorType.MODULE_NOT_INSTALLED -> getModuleNotInstalledGuide()
            HiltErrorType.QUALIFIER_MISMATCH -> getQualifierMismatchGuide()
            HiltErrorType.INJECTION_TIMING -> getInjectionTimingGuide()
            HiltErrorType.TESTING_ISSUES -> getTestingIssuesGuide()
        }
    }
    
    /**
     * Generate a comprehensive migration checklist
     */
    fun generateMigrationChecklist(): String = buildString {
        appendLine("=== HILT MIGRATION CHECKLIST ===")
        appendLine()
        appendLine("□ 1. PROJECT SETUP")
        appendLine("  □ Add Hilt plugin to app/build.gradle")
        appendLine("  □ Add Hilt dependencies")
        appendLine("  □ Configure KSP (recommended over KAPT)")
        appendLine("  □ Update proguard rules")
        appendLine()
        appendLine("□ 2. APPLICATION CLASS")
        appendLine("  □ Add @HiltAndroidApp annotation")
        appendLine("  □ Remove Injekt initialization")
        appendLine("  □ Keep existing functionality")
        appendLine()
        appendLine("□ 3. CREATE HILT MODULES")
        appendLine("  □ DatabaseModule (@InstallIn(SingletonComponent::class))")
        appendLine("  □ NetworkModule (@InstallIn(SingletonComponent::class))")
        appendLine("  □ SourceModule (@InstallIn(SingletonComponent::class))")
        appendLine("  □ AnalyticsModule (@InstallIn(SingletonComponent::class))")
        appendLine("  □ CoroutineModule (@InstallIn(SingletonComponent::class))")
        appendLine()
        appendLine("□ 4. MIGRATE VIEWMODELS")
        appendLine("  □ Add @HiltViewModel annotation")
        appendLine("  □ Replace 'by injectLazy()' with constructor injection")
        appendLine("  □ Update ViewModel creation in Fragments/Activities")
        appendLine("  □ Test ViewModel injection")
        appendLine()
        appendLine("□ 5. MIGRATE ACTIVITIES")
        appendLine("  □ Add @AndroidEntryPoint annotation")
        appendLine("  □ Replace 'by injectLazy()' with '@Inject lateinit var'")
        appendLine("  □ Test activity lifecycle integration")
        appendLine()
        appendLine("□ 6. MIGRATE FRAGMENTS")
        appendLine("  □ Add @AndroidEntryPoint annotation")
        appendLine("  □ Replace injection patterns")
        appendLine("  □ Test fragment lifecycle integration")
        appendLine()
        appendLine("□ 7. MIGRATE SERVICES")
        appendLine("  □ Add @AndroidEntryPoint annotation")
        appendLine("  □ Update service injection patterns")
        appendLine("  □ Test service lifecycle")
        appendLine()
        appendLine("□ 8. UPDATE TESTS")
        appendLine("  □ Create test modules with @TestInstallIn")
        appendLine("  □ Update unit tests to use Hilt")
        appendLine("  □ Create integration tests")
        appendLine("  □ Test migration validation")
        appendLine()
        appendLine("□ 9. CLEANUP")
        appendLine("  □ Remove Injekt dependencies")
        appendLine("  □ Delete AppModule.kt")
        appendLine("  □ Remove Injekt imports")
        appendLine("  □ Final validation tests")
        appendLine("===============================")
    }
    
    private fun getMissingBindingGuide(): TroubleshootingInfo {
        return TroubleshootingInfo(
            errorType = HiltErrorType.MISSING_BINDING,
            title = "Missing Binding Error",
            description = "Hilt cannot find a provider for the requested dependency",
            symptoms = listOf(
                "Compilation error: 'Missing binding for [Type]'",
                "Build fails with Hilt processor errors",
                "Dependency not available at injection site"
            ),
            causes = listOf(
                "No @Provides method for the dependency",
                "Module not installed in correct component",
                "Incorrect scoping or qualifiers",
                "Typo in dependency type"
            ),
            solutions = listOf(
                "Create a Hilt module with @Provides method",
                "Add @InstallIn annotation to module",
                "Verify dependency type matches exactly",
                "Check for missing qualifiers"
            ),
            codeExample = """
                // Create a module for the missing dependency
                @Module
                @InstallIn(SingletonComponent::class)
                object MissingDependencyModule {
                    
                    @Provides
                    @Singleton
                    fun provideMissingDependency(
                        @ApplicationContext context: Context
                    ): MissingDependency {
                        return MissingDependency(context)
                    }
                }
            """.trimIndent(),
            preventionTips = listOf(
                "Always create modules before using dependencies",
                "Use consistent naming conventions",
                "Document all provided dependencies",
                "Use compile-time validation"
            )
        )
    }
    
    private fun getCircularDependencyGuide(): TroubleshootingInfo {
        return TroubleshootingInfo(
            errorType = HiltErrorType.CIRCULAR_DEPENDENCY,
            title = "Circular Dependency Error",
            description = "Two or more dependencies depend on each other, creating a cycle",
            symptoms = listOf(
                "Compilation error: 'Circular dependency detected'",
                "Build fails with dependency cycle message",
                "Stack overflow during dependency resolution"
            ),
            causes = listOf(
                "Direct circular reference (A → B → A)",
                "Indirect circular reference (A → B → C → A)",
                "Poor architecture design",
                "Incorrect dependency relationships"
            ),
            solutions = listOf(
                "Use @Lazy<T> to break the cycle",
                "Use Provider<T> for optional dependencies",
                "Refactor architecture to eliminate cycle",
                "Extract common functionality to separate component"
            ),
            codeExample = """
                // Before: Circular dependency
                class ServiceA @Inject constructor(private val serviceB: ServiceB)
                class ServiceB @Inject constructor(private val serviceA: ServiceA)
                
                // After: Using @Lazy to break cycle
                class ServiceA @Inject constructor(private val serviceB: ServiceB)
                class ServiceB @Inject constructor(private val serviceA: Lazy<ServiceA>)
                
                // Alternative: Using Provider
                class ServiceB @Inject constructor(private val serviceAProvider: Provider<ServiceA>)
            """.trimIndent(),
            preventionTips = listOf(
                "Design clear dependency hierarchies",
                "Use dependency inversion principle",
                "Avoid bidirectional dependencies",
                "Regular architecture reviews"
            )
        )
    }
    
    private fun getWrongScopeGuide(): TroubleshootingInfo {
        return TroubleshootingInfo(
            errorType = HiltErrorType.WRONG_SCOPE,
            title = "Wrong Scope Error",
            description = "Dependency scope doesn't match the component scope",
            symptoms = listOf(
                "Compilation error about scope mismatch",
                "Dependencies not available in expected scope",
                "Memory leaks or unexpected behavior"
            ),
            causes = listOf(
                "Using @Singleton in non-singleton component",
                "Injecting activity-scoped dependency in fragment",
                "Incorrect @InstallIn component",
                "Mismatched provider and injection scopes"
            ),
            solutions = listOf(
                "Match provider scope with component scope",
                "Use correct @InstallIn component",
                "Review dependency lifecycle requirements",
                "Use appropriate scope annotations"
            ),
            codeExample = """
                // Correct scoping examples
                
                // Application-level singleton
                @Module
                @InstallIn(SingletonComponent::class)
                object AppModule {
                    @Provides
                    @Singleton
                    fun provideAppLevelService(): AppLevelService = AppLevelService()
                }
                
                // Activity-scoped dependency
                @Module
                @InstallIn(ActivityComponent::class)
                object ActivityModule {
                    @Provides
                    @ActivityScoped
                    fun provideActivityService(): ActivityService = ActivityService()
                }
            """.trimIndent(),
            preventionTips = listOf(
                "Understand Hilt component hierarchy",
                "Document scope decisions",
                "Use appropriate lifecycle scopes",
                "Test scope behavior"
            )
        )
    }
    
    private fun getMissingEntryPointGuide(): TroubleshootingInfo {
        return TroubleshootingInfo(
            errorType = HiltErrorType.MISSING_ENTRY_POINT,
            title = "Missing Entry Point Error",
            description = "Android component is not annotated with @AndroidEntryPoint",
            symptoms = listOf(
                "Injection doesn't work in Activity/Fragment/Service",
                "UninitializedPropertyAccessException for @Inject fields",
                "Dependencies are null at runtime"
            ),
            causes = listOf(
                "Missing @AndroidEntryPoint annotation",
                "Incorrect base class inheritance",
                "Hilt not properly configured",
                "Component not supported by Hilt"
            ),
            solutions = listOf(
                "Add @AndroidEntryPoint annotation",
                "Extend proper base classes",
                "Verify Hilt setup in Application class",
                "Check component compatibility"
            ),
            codeExample = """
                // Correct entry point setup
                
                @AndroidEntryPoint
                class MainActivity : AppCompatActivity() {
                    @Inject lateinit var dbHelper: DBHelper
                    
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        // Dependencies are automatically injected
                    }
                }
                
                @AndroidEntryPoint
                class LibraryFragment : Fragment() {
                    @Inject lateinit var dataCenter: DataCenter
                    
                    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                        super.onViewCreated(view, savedInstanceState)
                        // Dependencies are available here
                    }
                }
            """.trimIndent(),
            preventionTips = listOf(
                "Always annotate Android components",
                "Use consistent base classes",
                "Verify Hilt setup early",
                "Test injection in all components"
            )
        )
    }
    
    private fun getModuleNotInstalledGuide(): TroubleshootingInfo {
        return TroubleshootingInfo(
            errorType = HiltErrorType.MODULE_NOT_INSTALLED,
            title = "Module Not Installed Error",
            description = "Hilt module is not properly installed in a component",
            symptoms = listOf(
                "Bindings from module not available",
                "Compilation errors about missing providers",
                "Module providers not found"
            ),
            causes = listOf(
                "Missing @InstallIn annotation",
                "Wrong component in @InstallIn",
                "Module not in classpath",
                "Incorrect module structure"
            ),
            solutions = listOf(
                "Add @InstallIn annotation to module",
                "Use correct component type",
                "Verify module is compiled",
                "Check module class structure"
            ),
            codeExample = """
                // Correct module installation
                
                @Module
                @InstallIn(SingletonComponent::class)  // Install in correct component
                object DatabaseModule {
                    
                    @Provides
                    @Singleton
                    fun provideDBHelper(@ApplicationContext context: Context): DBHelper {
                        return DBHelper.getInstance(context)
                    }
                }
                
                // For activity-scoped dependencies
                @Module
                @InstallIn(ActivityComponent::class)
                object ActivityModule {
                    // Activity-specific providers
                }
            """.trimIndent(),
            preventionTips = listOf(
                "Always use @InstallIn annotation",
                "Choose appropriate component scope",
                "Verify module compilation",
                "Document module purposes"
            )
        )
    }
    
    private fun getQualifierMismatchGuide(): TroubleshootingInfo {
        return TroubleshootingInfo(
            errorType = HiltErrorType.QUALIFIER_MISMATCH,
            title = "Qualifier Mismatch Error",
            description = "Qualifiers don't match between provider and injection site",
            symptoms = listOf(
                "Multiple bindings error for same type",
                "Wrong dependency injected",
                "Compilation errors about ambiguous bindings"
            ),
            causes = listOf(
                "Missing qualifiers on providers or injection sites",
                "Typos in qualifier names",
                "Inconsistent qualifier usage",
                "Multiple unqualified providers"
            ),
            solutions = listOf(
                "Use consistent qualifiers",
                "Add @Named or custom qualifiers",
                "Verify qualifier spelling",
                "Document qualifier usage"
            ),
            codeExample = """
                // Using qualifiers correctly
                
                @Module
                @InstallIn(SingletonComponent::class)
                object NetworkModule {
                    
                    @Provides
                    @Singleton
                    @Named("api")
                    fun provideApiOkHttpClient(): OkHttpClient {
                        return OkHttpClient.Builder()
                            .addInterceptor(ApiInterceptor())
                            .build()
                    }
                    
                    @Provides
                    @Singleton
                    @Named("image")
                    fun provideImageOkHttpClient(): OkHttpClient {
                        return OkHttpClient.Builder()
                            .addInterceptor(ImageInterceptor())
                            .build()
                    }
                }
                
                // Injection with qualifiers
                class NetworkService @Inject constructor(
                    @Named("api") private val apiClient: OkHttpClient,
                    @Named("image") private val imageClient: OkHttpClient
                )
            """.trimIndent(),
            preventionTips = listOf(
                "Use descriptive qualifier names",
                "Document qualifier purposes",
                "Be consistent with qualifier usage",
                "Avoid unnecessary qualifiers"
            )
        )
    }
    
    private fun getInjectionTimingGuide(): TroubleshootingInfo {
        return TroubleshootingInfo(
            errorType = HiltErrorType.INJECTION_TIMING,
            title = "Injection Timing Issues",
            description = "Dependencies accessed before injection is complete",
            symptoms = listOf(
                "UninitializedPropertyAccessException",
                "Null dependencies in early lifecycle methods",
                "Inconsistent injection behavior"
            ),
            causes = listOf(
                "Accessing @Inject fields too early",
                "Incorrect lifecycle method usage",
                "Race conditions in initialization",
                "Improper super() call timing"
            ),
            solutions = listOf(
                "Access dependencies after super.onCreate()",
                "Use proper lifecycle methods",
                "Initialize in onViewCreated() for Fragments",
                "Check injection timing in tests"
            ),
            codeExample = """
                // Correct injection timing
                
                @AndroidEntryPoint
                class MainActivity : AppCompatActivity() {
                    @Inject lateinit var dbHelper: DBHelper
                    
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)  // Injection happens here
                        setContentView(R.layout.activity_main)
                        
                        // Safe to use dependencies after super.onCreate()
                        dbHelper.initialize()
                    }
                }
                
                @AndroidEntryPoint
                class LibraryFragment : Fragment() {
                    @Inject lateinit var dataCenter: DataCenter
                    
                    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                        super.onViewCreated(view, savedInstanceState)
                        
                        // Safe to use dependencies in onViewCreated()
                        dataCenter.loadLibrary()
                    }
                }
            """.trimIndent(),
            preventionTips = listOf(
                "Understand Android lifecycle",
                "Always call super methods first",
                "Test injection timing",
                "Use lifecycle-aware components"
            )
        )
    }
    
    private fun getTestingIssuesGuide(): TroubleshootingInfo {
        return TroubleshootingInfo(
            errorType = HiltErrorType.TESTING_ISSUES,
            title = "Hilt Testing Issues",
            description = "Problems with Hilt in unit and integration tests",
            symptoms = listOf(
                "Test dependencies not injected",
                "Production dependencies used in tests",
                "Test modules not working",
                "Hilt test runner issues"
            ),
            causes = listOf(
                "Missing @HiltAndroidTest annotation",
                "Incorrect test module setup",
                "Wrong test runner configuration",
                "Missing HiltAndroidRule"
            ),
            solutions = listOf(
                "Use @HiltAndroidTest for integration tests",
                "Create proper test modules with @TestInstallIn",
                "Configure Hilt test runner",
                "Add HiltAndroidRule to tests"
            ),
            codeExample = """
                // Correct Hilt test setup
                
                @HiltAndroidTest
                class ChaptersViewModelTest {
                    
                    @get:Rule
                    var hiltRule = HiltAndroidRule(this)
                    
                    @Inject
                    lateinit var dbHelper: DBHelper
                    
                    @Before
                    fun init() {
                        hiltRule.inject()
                    }
                    
                    @Test
                    fun testChaptersLoading() {
                        // Test implementation
                    }
                }
                
                // Test module
                @Module
                @TestInstallIn(
                    components = [SingletonComponent::class],
                    replaces = [DatabaseModule::class]
                )
                object TestDatabaseModule {
                    
                    @Provides
                    @Singleton
                    fun provideTestDBHelper(): DBHelper {
                        return mockk<DBHelper>()
                    }
                }
            """.trimIndent(),
            preventionTips = listOf(
                "Set up test infrastructure early",
                "Use consistent test patterns",
                "Mock external dependencies",
                "Test both unit and integration scenarios"
            )
        )
    }
}

/**
 * Types of Hilt errors that can occur
 */
enum class HiltErrorType {
    MISSING_BINDING,
    CIRCULAR_DEPENDENCY,
    WRONG_SCOPE,
    MISSING_ENTRY_POINT,
    MODULE_NOT_INSTALLED,
    QUALIFIER_MISMATCH,
    INJECTION_TIMING,
    TESTING_ISSUES
}

/**
 * Comprehensive troubleshooting information for a specific error type
 */
data class TroubleshootingInfo(
    val errorType: HiltErrorType,
    val title: String,
    val description: String,
    val symptoms: List<String>,
    val causes: List<String>,
    val solutions: List<String>,
    val codeExample: String,
    val preventionTips: List<String>
) {
    fun getFormattedGuide(): String = buildString {
        appendLine("=== $title ===")
        appendLine()
        appendLine("DESCRIPTION:")
        appendLine(description)
        appendLine()
        appendLine("SYMPTOMS:")
        symptoms.forEach { symptom ->
            appendLine("  • $symptom")
        }
        appendLine()
        appendLine("COMMON CAUSES:")
        causes.forEach { cause ->
            appendLine("  • $cause")
        }
        appendLine()
        appendLine("SOLUTIONS:")
        solutions.forEach { solution ->
            appendLine("  • $solution")
        }
        appendLine()
        appendLine("CODE EXAMPLE:")
        appendLine(codeExample)
        appendLine()
        appendLine("PREVENTION TIPS:")
        preventionTips.forEach { tip ->
            appendLine("  • $tip")
        }
        appendLine("=" + "=".repeat(title.length + 6))
    }
}