����Ӧ��ʹ�õ����������£���Ҳ������build.gradle.kts�ļ��в鿴��
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

����ʹ��IDEΪAndroidStudio
ʹ�ô���ΪJAVA

����Apk��������AndroidStudio����Я��������apk���ܽ���һ��ʽ���ɣ��õ�apk���ֻ��ļ�����ϵͳ�е����װ���ɡ�

