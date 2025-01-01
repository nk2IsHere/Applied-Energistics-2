package appeng.client.gui.me.search;

import appeng.api.stacks.AEKeyType;
import appeng.menu.me.common.GridInventoryEntry;
import net.minecraft.tags.TagKey;

import java.util.*;
import java.util.function.Predicate;

final class TagSearchPredicate implements Predicate<GridInventoryEntry> {
    private final String term;
    /**
     * Stores the tag keys we found for each AE key type we encountered.
     */
    private final Map<AEKeyType, List<TagKey<?>>> tagCache = new IdentityHashMap<>();

    public TagSearchPredicate(String term) {
        this.term = term.toLowerCase(Locale.ROOT);
    }

    /**
     * Finds all tags for all AE key types that match the given search pattern.
     */
    private List<TagKey<?>> getTagsMatchingTerm(AEKeyType keyType) {
        return keyType.getTagNames()
                .filter(tagKey -> {
                    // ResourceLocations require namespace and path to already be lowercase
                    var tagId = tagKey.location();
                    if (term.contains(":")) {
                        return tagId.toString().contains(term);
                    } else {
                        return tagId.getNamespace().contains(term) || tagId.getPath().contains(term);
                    }
                })
                .toList();
    }

    @Override
    public boolean test(GridInventoryEntry entry) {
        var what = Objects.requireNonNull(entry.getWhat());
        var tags = tagCache.computeIfAbsent(what.getType(), this::getTagsMatchingTerm);

        for (var tag : tags) {
            if (what.isTagged(tag)) {
                return true;
            }
        }

        return false;
    }
}
