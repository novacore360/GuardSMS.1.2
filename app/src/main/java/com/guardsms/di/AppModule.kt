package com.guardsms.di

// GuardRepository is provided directly via its @Inject constructor and
// @Singleton scope (see GuardRepository.kt), so no explicit Hilt module
// binding is needed here. Declaring a duplicate @Provides for the same
// type here would cause a Dagger "duplicate bindings" compile error.
