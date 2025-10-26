plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "benicio.solucoes.enfermaguia"
    compileSdk = 35

    defaultConfig {
        applicationId = "benicio.solucoes.enfermaguia"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {



    // Activity e SavedState compatíveis
    implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.3")
    implementation("androidx.activity:activity:1.7.2")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.savedstate:savedstate:1.2.1")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")

    // Firebase
    implementation("com.google.firebase:firebase-database:22.0.1")

    // Outras dependências do projeto
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.oguzdev:CircularFloatingActionMenu:1.0.2")
    implementation("com.itextpdf:itext7-core:7.1.15")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // Testes
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}


