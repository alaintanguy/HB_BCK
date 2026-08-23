# Keep classes related to sensors and battery monitoring
-keep class android.hardware.Sensor { *; }
-keep class android.hardware.SensorEvent { *; }
-keep class android.os.BatteryManager { *; }

# Keep our main activity
-keep class com.healthbridge.wear.MainActivity { *; }

# Keep Android R class
-keep class **.R$* {
    <fields>;
}

# Standard Android optimization rules
-optimizationpasses 5
-verbose

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
