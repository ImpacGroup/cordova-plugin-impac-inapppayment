package de.impacgroup.inapppayment;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class IMPSharedPreferencesHelper {

    private static String validationKey = "de.impacgroup.openValidations";
    private static String refreshKey = "de.impacgroup.refreshStatus";
    private SharedPreferences sharedPreferences;

    IMPSharedPreferencesHelper(Context context) {
        this.sharedPreferences = getShared(context);
    }

    void storeRefreshDate(Date date) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(refreshKey, date.getTime());
        editor.apply();
    }

    @Nullable Date getRefreshDate() {
        long dateLong = sharedPreferences.getLong(refreshKey, 0);
        if (dateLong != 0) {
            return new Date(dateLong);
        }
        return null;
    }

    Set<String> getTokensForValidation() {
        return new HashSet<>(sharedPreferences.getStringSet(validationKey, new HashSet<String>()));
    }

    void storeTokenForValidation(Set<String> tokens) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(validationKey, tokens);
        editor.apply();
    }

    private SharedPreferences getShared(Context context) {
        String preferencesKey = "de.impacgroup.preferernces";
        return context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE);
    }
}
