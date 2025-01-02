package appeng.util;

import appeng.integration.abstraction.ItemListMod;
import com.google.common.base.Strings;

public final class ExternalSearch {
    private ExternalSearch() {
    }

    public static boolean isExternalSearchAvailable() {
        return ItemListMod.isEnabled();
    }

    public static String getExternalSearchText() {
        if (ItemListMod.isEnabled()) {
            return Strings.nullToEmpty(ItemListMod.getSearchText());
        } else {
            return "";
        }
    }

    public static void setExternalSearchText(String text) {
        ItemListMod.setSearchText(Strings.nullToEmpty(text));
    }

    public static void clearExternalSearchText() {
        ItemListMod.setSearchText("");
    }

    public static boolean isExternalSearchFocused() {
        return ItemListMod.hasSearchFocus();
    }
}
