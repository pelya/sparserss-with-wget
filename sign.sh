#!/bin/sh
# Set path to your Android keystore and your keystore alias here, or put them in your environment
[ -z "$ANDROID_KEYSTORE_FILE" ] && ANDROID_KEYSTORE_FILE=~/.android/debug.keystore
[ -z "$ANDROID_KEYSTORE_ALIAS" ] && ANDROID_KEYSTORE_ALIAS=androiddebugkey

cd bin

# Remove old certificate
zip -d MainTabActivity-debug-unaligned.apk "META-INF/*"
# Sign with the new certificate
echo Using keystore $ANDROID_KEYSTORE_FILE and alias $ANDROID_KEYSTORE_ALIAS
stty -echo
jarsigner -verbose -keystore $ANDROID_KEYSTORE_FILE -sigalg MD5withRSA -digestalg SHA1 MainTabActivity-debug-unaligned.apk $ANDROID_KEYSTORE_ALIAS || exit 1
stty echo
echo
rm -f ../SparseRSS.apk
zipalign 4 MainTabActivity-debug-unaligned.apk ../SparseRSS.apk
