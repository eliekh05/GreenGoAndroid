// Top-level build file — configuration shared across all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    id("org.sonarqube") version "7.2.3.7755"
}
