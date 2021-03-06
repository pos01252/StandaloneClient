package org.stt.text;

import org.stt.IntRange;
import org.stt.StopWatch;
import org.stt.config.CommonPrefixGrouperConfig;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Learns common prefixes and uses them to determine groups.
 * Note that items are split at 'space' unless the resulting subgroup would have less than
 * 3 characters, in which case the group gets expanded.
 */
@Singleton
class CommonPrefixGrouper implements ItemGrouper, ExpansionProvider {
    static final int MINIMUM_GROUP_LENGTH = 3;
    private final TimeTrackingItemQueries queries;
    private final CommonPrefixGrouperConfig config;
    private boolean initialized;
    private PrefixTree root = new PrefixTree();

    @Inject
    CommonPrefixGrouper(TimeTrackingItemQueries queries,
                        CommonPrefixGrouperConfig config) {
        this.queries = Objects.requireNonNull(queries);
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public List<Group> getGroupsOf(String text) {
        Objects.requireNonNull(text);
        checkInitialized();
        return new GroupHelper(text, root).parse();
    }

    private void checkInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;

        StopWatch stopWatch = new StopWatch("Item grouper");
        queries.queryAllItems()
                .map(TimeTrackingItem::getActivity)
                .forEach(this::insert);

        config.getBaseLine()
                .forEach(this::insert);


        stopWatch.stop();
    }

    void insert(String item) {
        PrefixTree node = root;
        int i = 0;
        int n = item.length();

        char[] chars = item.toCharArray();
        while (i < n) {
            PrefixTree child = node.child(chars[i]);
            if (child != null) {
                node = child;
                i++;
            } else {
                break;
            }
        }
        while (i < n) {
            PrefixTree newChild = new PrefixTree();
            node.child(chars[i], newChild);
            i++;
            node = newChild;
        }
        node.child(null, null);
    }


    @Override
    public List<String> getPossibleExpansions(String text) {
        Objects.requireNonNull(text);
        checkInitialized();

        char[] chars = text.toCharArray();
        PrefixTree node = root;
        int i = 0;
        int n = chars.length;
        while (i < n && node != null) {
            node = node.child(chars[i]);
            i++;
        }
        return node == null ? Collections.emptyList() :
                node.allChildren().stream()
                        .map(entry -> {
                            PrefixTree tree = entry.getValue();
                            StringBuilder current = new StringBuilder();
                            current.append(entry.getKey());
                            while (tree != null && tree.numChildren() == 1) {
                                Character childChar = tree.anyChild();
                                tree = tree.child(childChar);
                                if (childChar != null) {
                                    current.append(childChar);
                                }
                            }
                            return current.toString();
                        })
                        .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "";
    }

    private static class PrefixTree {
        private Map<Character, PrefixTree> child;

        PrefixTree child(Character c) {
            if (child == null) {
                child = new HashMap<>();
            }
            return child.get(c);
        }

        void child(Character c, PrefixTree newChild) {
            if (child == null) {
                child = new HashMap<>();
            }
            child.put(c, newChild);
        }

        int numChildren() {
            return child == null ? 0 : child.size();
        }

        Character anyChild() {
            return child.keySet().iterator().next();
        }

        Set<Map.Entry<Character, PrefixTree>> allChildren() {
            return child == null ? Collections.emptySet() : child.entrySet();
        }
    }

    private static class GroupHelper {
        private final String text;
        private final List<Group> groups = new ArrayList<>();
        private final char[] chars;
        private final int n;
        private PrefixTree node;
        private int i = 0;
        private int start = 0;
        private int lastGood;

        GroupHelper(String text, PrefixTree root) {
            this.text = text;
            this.chars = text.toCharArray();
            this.n = chars.length;
            this.node = root;
        }

        public List<Group> parse() {
            while (i < n && node != null) {
                lastGood = start;
                parseToNextBranch();
                setLastGoodToNextWhitespace();
                i = Math.max(i, lastGood);
                skipWhitespace();
                groups.add(new Group(Type.MATCH, text.substring(start, lastGood), new IntRange(start, lastGood)));
                start = i;
            }
            if (start < n) {
                groups.add(new Group(Type.REMAINDER, text.substring(start, n), new IntRange(start, n)));
            }
            return groups;
        }

        private void skipWhitespace() {
            while (i < n && Character.isWhitespace(chars[i])) {
                if (node != null) {
                    node = node.child(chars[i]);
                }
                i++;
            }
        }

        private void setLastGoodToNextWhitespace() {
            do {
                if (lastGood >= i && node != null) {
                    node = node.child(chars[lastGood]);
                }
                lastGood++;
            }
            while (lastGood < n && (!Character.isWhitespace(chars[lastGood]) || lastGood - start < MINIMUM_GROUP_LENGTH));
        }

        private void parseToNextBranch() {
            do {
                if (!Character.isWhitespace(chars[i])) {
                    lastGood = i;
                }
                node = node.child(chars[i]);
                i++;
            } while (i < n && node != null && node.numChildren() <= 1);
        }
    }
}
