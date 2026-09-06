-dontobfuscate
-ignorewarnings
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class androidx.media3.** { *; }
-keep class com.example.tsuki.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class kotlinx.coroutines.** { *; }
-keep class coil3.** { *; }
-keep class androidx.datastore.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keep class org.brotli.** { *; }
-keep class com.google.re2j.** { *; }
-keep class com.grack.nanojson.** { *; }
-keep class kotlinx.serialization.** { *; }
-keep class * implements androidx.glance.action.ActionCallback { *; }
-keep class com.example.tsuki.ui.widget.glance.** { *; }
