
import os

from colors import *

# Set your Android app ID
p_app_id = "com.example.game"
module_name = "GodotFacebook"

def can_build(env_plat, plat = None):
    #return False
    if plat == None:
        if isinstance(env_plat, basestring):
            plat = env_plat
        else:
            print("%s: %sDisabled%s (Platform not set)" % (module_name, RED, RESET))
            return False

    if plat == "android":
        print("%s: %sEnabled%s" % (module_name, GREEN, RESET))
        return True
    else:
        print("%s: %sDisabled%s" % (module_name, RED, RESET))
        return False
    pass   

def configure(env):
    global p_app_id
    if env["platform"] == "android":

        if env.get("application_id", None) != None:
            p_app_id = env["application_id"]

        env.android_add_maven_repository("url 'https://oss.sonatype.org/content/repositories/snapshots'")

        env.android_add_gradle_classpath("com.google.gms:google-services:4.1.0")
        env.android_add_gradle_plugin("com.google.gms.google-services")

        env.android_add_dependency("implementation 'com.android.support:support-annotations:25.0.1'")
        env.android_add_dependency("implementation 'com.facebook.android:facebook-android-sdk:[4,5)'")

        env.android_add_java_dir("android");
        env.android_add_res_dir("res");

        if "frogutils" in [os.path.split(path)[1] for path in env.android_java_dirs]: pass
        else: env.android_add_java_dir("frogutils");

        env.android_add_to_manifest("android/AndroidManifestChunk.xml");
        env.android_add_default_config("minSdkVersion 15")
        env.android_add_default_config("applicationId '"+p_app_id+"'")
        #env.disable_module()
