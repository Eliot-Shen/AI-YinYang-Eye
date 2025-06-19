# AI阴阳眼 - 通用实时AI伪造检测APP  
**AI Yin-Yang Eyes: Universal Real-time AI Forgery Detection APP**

## 🌐 项目简介 | Project Overview

随着AI技术的迅猛发展，AI生成的虚假视频与图像在社交、通信等领域日益泛滥，尤其对中老年人与青少年构成严重安全威胁。  
**AI阴阳眼**是一款面向大众用户、特别是易受骗人群的AI伪造检测APP，可实现：

- 📱 **实时视频通话检测**：在视频通话中实时分析对方画面是否由AI合成；
- 🖼️ **图片识别分析**：快速判断用户上传的图片是否为AI生成内容；
- 🚨 **实时防护提醒**：提供直观提示，降低受骗风险，提升网络安全意识。

本项目基于 Ojha 等人提出的论文 [*Towards Universal Fake Image Detectors that Generalize Across Generative Models*](https://github.com/WisconsinAIVision/UniversalFakeDetect) 的开源实现进行二次开发与优化。

安卓部分完全为自主开发。


## News
- **2025.5.26** 项目荣获全国大学生软件创新大赛全国总决赛三等奖 && 华东赛区一等奖

## 效果图
![本地图片](./Server/example_img/teaser.jpg "效果图")

## 🖥️ 服务端部署指南 | Server-Side Deployment

### 1. 环境配置

- **硬件**：NVIDIA Tesla T4 (16GB 显存), 16GB 内存  
- **系统**：Ubuntu 22.04

### 2. 软件安装步骤

#### ✅ 系统更新
```bash
sudo apt update
sudo apt upgrade -y
````

#### ✅ 安装 NVIDIA 驱动

```bash
sudo apt install nvidia-driver-535
sudo reboot
```

#### ✅ 安装 CUDA Toolkit 12.2

```bash
wget https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2204/x86_64/cuda-keyring_1.1-1_all.deb
sudo dpkg -i cuda-keyring_1.1-1_all.deb
sudo apt-get update
sudo apt-get -y install cuda-toolkit-12-9
sudo apt-get install -y cuda-drivers
```

#### ✅ 安装 Python 3.10

```bash
sudo apt install python3.10 python3.10-venv python3.10-dev -y
python3.10 --version
```

#### ✅ （推荐）创建虚拟环境

```bash
conda create -n myenv
conda activate myenv
```


### 3. 服务代码部署与启动

#### 🔧 安装依赖

```bash
cd /your/service/code/path
pip install -r requirements.txt
```

#### 🚀 启动服务

```bash
python app.py
```

#### 📊 监控与日志

使用以下命令监控资源：

```bash
nvidia-smi
```



## 📱 安卓客户端部署 | Android Client Setup

可以直接使用仓库提供的apk文件，在安卓手机上安装即可。

### 🧰 开发环境

* IDE：Android Studio
* 编程语言：Java
* 构建工具：Gradle（Kotlin DSL）

### 📦 主要依赖（可在 `build.gradle.kts` 查看）

```kotlin
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.retrofit)
    implementation(libs.retrofit2.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    implementation(libs.androidx.activity.activity.ktx)
    implementation(libs.com.google.code.gson.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
```

### 📱 APK 生成

通过 Android Studio 的“Build APK”功能一键打包，即可在 Android 设备中手动安装 APK 文件进行体验。



🌐 服务器地址配置
首次打开 APP 时，用户需在界面中手动输入后端服务器的 IP 地址 与 端口号（例如：http://192.168.1.100:5000）。
该信息用于客户端与服务器端的通信连接，并可在设置页面中随时修改。



## 📖 引用来源 | Citation

本项目基于以下论文与代码仓库进行开发：

> **Towards Universal Fake Image Detectors that Generalize Across Generative Models**
> \[Ojha et al., Wisconsin AI Vision Lab]
> 🔗 GitHub链接：[https://github.com/WisconsinAIVision/UniversalFakeDetect](https://github.com/WisconsinAIVision/UniversalFakeDetect)



## 🤝 团队愿景 | Our Vision

我们致力于用技术守护大众安全。**AI阴阳眼**不仅是技术成果，更是一项社会责任。欢迎更多开发者参与共建，推动AI安全走入寻常百姓家！



## 📬 联系方式 | Contact

如需合作、建议或报告问题，请通过 GitHub Issues 或邮箱联系我们。



