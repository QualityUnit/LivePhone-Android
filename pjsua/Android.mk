LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := libpjsua2
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_SRC_FILES := \
	/Users/rasto/Development/git/LivePhone-Android/pjsua/src/main/jni/Android.mk \
	/Users/rasto/Development/git/LivePhone-Android/pjsua/src/main/jni/Application.mk \

LOCAL_C_INCLUDES += /Users/rasto/Development/git/LivePhone-Android/pjsua/src/debug/jni
LOCAL_C_INCLUDES += /Users/rasto/Development/git/LivePhone-Android/pjsua/src/main/jni

include $(BUILD_SHARED_LIBRARY)
