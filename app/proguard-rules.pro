# 配るアプリでは、動作確認用のログ（Log.d / v / i）を消す。注意・エラー（w / e）は残す
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
