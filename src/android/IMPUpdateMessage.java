package de.impacgroup.inapppayment;

import androidx.annotation.Nullable;

public class IMPUpdateMessage {
    public String status;
    public String description;

    IMPUpdateMessage(String status, @Nullable String description) {
        this.status = status;
        this.description = description;
    }
}
