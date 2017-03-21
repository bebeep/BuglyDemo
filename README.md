# BuglyDemo
腾讯热更新框架Bugly的使用
1.集成腾讯Bugly SDK

1.配置项目的gradle(添加插件依赖)
工程根目录下“build.gradle”文件中添加：
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        // tinkersupport插件, 其中lastest.release指拉取最新版本，也可以指定明确版本号，例如1.0.4
        classpath "com.tencent.bugly:tinker-support:latest.release"
    }
}

/****************************************/
大坑：
1、在app的build.gradle中必须配置signingConfig
2、release中必须加上signingConfig signingConfigs.config
否则打正式补丁包会报错

android {
    signingConfigs {
        config {
            keyAlias 'fix'
            keyPassword '654231'
            storeFile file('D:/Kanwotao/fixtest/fix.jks')
            storePassword '654231'
        }
    }


    compileSdkVersion 23
    buildToolsVersion "25.0.0"

    defaultConfig {
        applicationId "com.bebeep.hotfixtestwithbugly"
        minSdkVersion 14
        targetSdkVersion 22
        versionCode 1
        versionName "1.0"
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.config
        }
    }
}
/****************************************/

注意：自tinkersupport 1.0.3版本起无需再配tinker插件的classpath。

2.在app目录下创建tinker-support.gradle文件

tinker-support.gradle内容如下所示（示例配置）：

apply plugin: 'com.tencent.bugly.tinker-support'

def bakPath = file("${buildDir}/bakApk/")

/**
 * 此处填写每次构建生成的基准包目录
 */
def baseApkDir = "app-0208-15-10-00"

/**
 * 对于插件各参数的详细解析请参考
 */
tinkerSupport {

    // 开启tinker-support插件，默认值true
    enable = true

    // 指定归档目录，默认值当前module的子目录tinker
    autoBackupApkDir = "${bakPath}"

    // 是否启用覆盖tinkerPatch配置功能，默认值false
    // 开启后tinkerPatch配置不生效，即无需添加tinkerPatch
    overrideTinkerPatchConfiguration = true

    // 编译补丁包时，必需指定基线版本的apk，默认值为空
    // 如果为空，则表示不是进行补丁包的编译
    // @{link tinkerPatch.oldApk }
    baseApk = "${bakPath}/${baseApkDir}/app-release.apk"

    // 对应tinker插件applyMapping
    baseApkProguardMapping = "${bakPath}/${baseApkDir}/app-release-mapping.txt"

    // 对应tinker插件applyResourceMapping
    baseApkResourceMapping = "${bakPath}/${baseApkDir}/app-release-R.txt"

    // 构建基准包和补丁包都要指定不同的tinkerId，并且必须保证唯一性
    tinkerId = "base-1.0.1"

    // 构建多渠道补丁时使用
    // buildAllFlavorsDir = "${bakPath}/${baseApkDir}"

    // 是否开启反射Application模式
    enableProxyApplication = false

}

/**
 * 一般来说,我们无需对下面的参数做任何的修改
 * 对于各参数的详细介绍请参考:
 * https://github.com/Tencent/tinker/wiki/Tinker-%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97
 */
tinkerPatch {
    //oldApk ="${bakPath}/${appName}/app-release.apk"
    ignoreWarning = false
    useSign = true
    dex {
        dexMode = "jar"
        pattern = ["classes*.dex"]
        loader = []
    }
    lib {
        pattern = ["lib/*/*.so"]
    }

    res {
        pattern = ["res/*", "r/*", "assets/*", "resources.arsc", "AndroidManifest.xml"]
        ignoreChange = []
        largeModSize = 100
    }

    packageConfig {
    }
    sevenZip {
        zipArtifact = "com.tencent.mm:SevenZip:1.1.10"
//        path = "/usr/local/bin/7za"
    }
    buildConfig {
        keepDexApply = false
        //tinkerId = "1.0.1-base"
        //applyMapping = "${bakPath}/${appName}/app-release-mapping.txt" //  可选，设置mapping文件，建议保持旧apk的proguard混淆方式
        //applyResourceMapping = "${bakPath}/${appName}/app-release-R.txt" // 可选，设置R.txt文件，通过旧apk文件保持ResId的分配
    }
}

3.配置app文件夹下的gradle(配置SDK)

在app module的“build.gradle”文件中添加（示例配置）：

// 依赖插件脚本
apply from: 'tinker-support.gradle'//与dependencies和android平级
dependencies {
    compile "com.android.support:multidex:1.0.1" // 多dex配置
    compile 'com.tencent.bugly:crashreport_upgrade:latest.release' // 升级SDK
}

4.自定义ApplicationLike(初始化SDK)

这一步是集合进自己项目中比较难的一步,特别是原来有JNI调用了自己的Application类的时候更加是.我自己的情况是将原来的application类继承为TinkerApplication类,方法全部移动到一个ApplicationLike类中.JNI调用的时候,配置为这个Like类就可以了.我试了直接将原来的Application类直接改为继承Like类是不行的,JNI调用Application类和Like类都不行,不知道是什么原因.Like类调用getApplication类就可以获得Application对象了. 
enableProxyApplication = false 的情况,这也是推荐的默认情况. 
①自定义ApplicationLike:

public class SampleApplicationLike extends DefaultApplicationLike {

    public static final String TAG = "Tinker.SampleApplicationLike";

    public SampleApplicationLike(Application application, int tinkerFlags,
            boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime,
            long applicationStartMillisTime, Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        // 这里实现SDK初始化，appId替换成你的在Bugly平台申请的appId
        // 调试时，将第三个参数改为true
        Bugly.init(getApplication(), "900029763", true);
    }


    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onBaseContextAttached(Context base) {
        super.onBaseContextAttached(base);
        // you must install multiDex whatever tinker is installed!
        MultiDex.install(base);

        // 安装tinker
        // TinkerManager.installTinker(this); 替换成下面Bugly提供的方法
        Beta.installTinker(this);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void registerActivityLifecycleCallback(Application.ActivityLifecycleCallbacks callbacks) {
        getApplication().registerActivityLifecycleCallbacks(callbacks);
    }

}

SampleApplicationLike这个类是Application的代理类，以前所有在Application的实现必须要全部拷贝到这里，在onCreate方法调用SDK的初始化方法，在onBaseContextAttached中调用Beta.installTinker(this);。
②.自定义Application:

public class SampleApplication extends TinkerApplication {
    public SampleApplication() {
        super(ShareConstants.TINKER_ENABLE_ALL, "xxx.xxx.SampleApplicationLike",
                "com.tencent.tinker.loader.TinkerLoader", false);
    }
}

修改第二项参数为你的ApplicationLike的文件目录地址,其他默认就可以了.

5.AndroidManifest.xml配置

①权限配置 
如果还想使用app内弹窗升级的功能就还需要配置BetaActivity和FileProvider.

<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.READ_LOGS" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

②将以前的Applicaton配置为继承TinkerApplication的类:

 <application
        android:name=".SampleApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

6.混淆配置

为了避免混淆SDK，在Proguard混淆文件中增加以下配置：

-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}
//如果你使用了support-v4包，你还需要配置以下混淆规则：
-keep class android.support.**{*;}

2.开始使用Bugly Android热更新功能

接入流程主要是以下几个步骤：

打基准包安装并上报联网（注：填写唯一的tinkerId） 
对基准包的bug修复（可以是Java代码变更，资源的变更） 
修改基准包路径、填写补丁包tinkerId、mapping文件路径、resId文件路径 
执行tinkerPatchRelease打Release版本补丁包 
选择app/build/outputs/patch目录下的补丁包并上传（注：不要选择tinkerPatch目录下的补丁包，不然上传会有问题） 
编辑下发补丁规则，点击立即下发 
重启基准包，请求补丁策略（SDK会自动下载补丁并合成） 
再次重启基准包，检验补丁应用结果
1.普通打包

①配置基准包的tinkerId 
tinkerId最好是一个唯一标识，例如Git版本号、versionName等等。 如果你要测试热更新，你需要对基线版本进行联网上报。 
②执行assembleRelease/assembleDebug编译生成基准包：注意debug和release包的区别,自己百度… 
另外注意用USB线连接真机进行调试…还有就是在ApplicationLike类中的Oncreat方法中设置第三个参数为true… 
③启动apk，上报联网数据 
我们每次冷启动都会请求补丁策略，会上报当前版本号和tinkerId，这样我们后台就能将这个唯一的tinkerId对应到一个版本. 
④根据基线版本生成补丁包: 
这里写图片描述 
然后打开tinker-support.gradle文件,修改baseApkDir名称为上图的名称 
这里写图片描述 
然后修改以下四个文件名称,特别是不要忘记tinkerid的名字了: 
这里写图片描述 
⑤执行构建补丁包的task(这快细心点,我就在这遇到坑了…): 
一定要注意,这里使用的是tinker_support文件夹里面的任务,不是tinker文件夹里面的,否則打不出patch文件夹!!! 
这里写图片描述 
生成的补丁包在build/outputs/patch目录下：有signed和zip字样的就是我们要上传的补丁包了!!! 
详细可参照:https://bugly.qq.com/docs/user-guide/instruction-manual-android-hotfix-demo/

2.多渠道打包

3.加固打包（仅支持tinker 1.7.5以下）

3.常见错误和易掉的坑
大坑：app的buld.gradle中没有配置签名！！！
1.没在Mannifest文件中正确配置Application类; 
2.bug修复完后没有正确修改tinker-support文件相关名称,特别是tinkerId; 
3.SampleApplication类没有正确配置SampleApplicationLike的完全地址; 
4.SampleApplicationLike类中没有正确配置appId(没有填写自己申请的Id或者填写错,填的是secretId等); 
5.SampleApplicationLike在调试的时候没有将第三个参数修改为true; 
6.生成基准包之后没有联网上报,导致上传补丁包的时候报错: 
这里写图片描述 
上传基准包的时候,打开app,一定要观察log,有可能由于网络原因导致上报失败,比如: 
这里写图片描述 
如果不看log,其实上传失败了你也不知道.所以这时候就要检查一下网络了. 
7.使用了错误的命令打补丁包: 
比如不是使用tinker-support里面的命令,使用的是tinker里面的命令,就会导致无法生成patch文件夹; 
