# P16.1 R8 / ProGuard keep rules — minimal and evidence-based only.
# NOTE: R8 is currently DEFERRED (minifyEnabled=false, see P16_1 report).
# These rules are dormant preparation for P17 to enable minification with
# device verification.

# android:onClick handlers are resolved reflectively at runtime by the View
# system (android:onClick="methodName(View)" in XML).
# Evidence: activity_write_tag.xml (WriteTag) and activity_dump_editor.xml
# (DumpEditor) use setContentView + android:onClick.
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# IMPORTANT (P16.1 finding): shrinkResources also removes string resources
# referenced ONLY via Resources.getIdentifier() at runtime. Proven to remove
# ac_sector_trailer_* and ac_data_block_no_keyb_* (AccessConditionScreen) and
# risk tag_* (TagInfoTool). Before enabling R8, add the following keep.xml:
#
#   res/values/keep.xml:
#   <resources xmlns:tools="http://schemas.android.com/tools"
#       tools:keep="@string/ac_*,@string/tag_*" />
#
# This is not expressed as a ProGuard rule because it is a resource shrinker
# concern, not a code concern.

