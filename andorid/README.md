整个应用使用到的依赖如下：（也可以在build.gradle.kts文件中查看）
dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.retrofit)
    implementation(libs.retrofit2.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    implementation(libs.androidx.activity.activity.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.com.google.code.gson.gson)

}

代码使用IDE为AndroidStudio
使用代码为JAVA

生成Apk：依赖于AndroidStudio自身携带的生成apk功能进行一键式生成，得到apk在手机文件管理系统中点击安装即可。

