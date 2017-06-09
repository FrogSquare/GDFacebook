
def can_build(plat):
    #return False;
    return plat == "android";

def configure(env):
    if env["platform"] == "android":
        env.android_add_maven_repository("url 'https://oss.sonatype.org/content/repositories/snapshots'")

        env.android_add_gradle_classpath("com.google.gms:google-services:3.0.0")
        env.android_add_gradle_plugin("com.google.gms.google-services")

        env.android_add_dependency("compile 'com.android.support:support-annotations:25.0.1'")
        env.android_add_dependency("compile 'com.facebook.android:facebook-android-sdk:4.18.0'")

	env.android_add_java_dir("android");
	env.android_add_res_dir("res");
	env.android_add_to_manifest("android/AndroidManifestChunk.xml");
	env.android_add_default_config("minSdkVersion 15")
	env.android_add_default_config("applicationId 'com.froglogics.dotsndots'")
	#env.disable_module()
