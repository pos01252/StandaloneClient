package org.stt.text;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.config.ConfigRoot;
import org.stt.config.ConfigService;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

public class CommonPrefixGrouperTest {
    @Mock
    private TimeTrackingItemQueries queries;
    @Mock
    private ConfigService configService;
    private CommonPrefixGrouper sut;

    @Before
    public void setup() {
		MockitoAnnotations.initMocks(this);

        sut = new CommonPrefixGrouper(queries, new ConfigRoot().getPrefixGrouper());
    }

	@Test
	public void shouldFindExpansion() {
		// GIVEN
        givenReaderReturnsItemsWithComment("group subgroup one",
                "group subgroup two");

		// WHEN
		List<String> expansions = sut.getPossibleExpansions("gr");

		// THEN
        assertThat(expansions, is(Collections.singletonList("oup subgroup ")));
    }

	@Test
	public void shouldFindSubgroupExpansion() {
		// GIVEN
        givenReaderReturnsItemsWithComment("group subgroup one",
                "group subgroup two");

		// WHEN
		List<String> expansions = sut.getPossibleExpansions("group subgroup o");

		// THEN
        assertThat(expansions, is(Collections.singletonList("ne")));
    }

	@Test
	public void shouldFindMultipleExpansions() {
		givenReaderReturnsItemsWithComment("group subgroup one",
				"group subgroup two");

		// WHEN
		List<String> expansions = sut.getPossibleExpansions("group subgroup ");

		// THEN
		assertThat(expansions, hasItems("two", "one"));
	}

	@Test
	public void shouldHandleGroupsWithLongerTextThanGivenComment() {
		// GIVEN
		givenReaderReturnsItemsWithComment("test");

		// WHEN
        List<String> result = groupsAsString("t");

		// THEN
        assertThat(result, is(Collections.singletonList("t")));
    }

    private List<String> groupsAsString(String t) {
        return sut.getGroupsOf(t).stream().map(g -> g.content).collect(Collectors.toList());
    }

    @Test
    public void shouldFindGroupsWithSpaces() {
        // GIVEN
		String firstComment = "group subgroup one";
		givenReaderReturnsItemsWithComment(firstComment, "group subgroup two");

		// WHEN
        List<String> result = groupsAsString(firstComment);

		// THEN
        assertThat(result, is(Arrays.asList("group subgroup", "one")));

	}

	@Test
	public void shouldFindSubGroups() {
		// GIVEN
		String firstComment = "group subgroup one";
		String thirdComment = "group subgroup2 one";
		givenReaderReturnsItemsWithComment(firstComment, "group subgroup two",
				thirdComment);

		// WHEN
        List<String> withThreeGroups = groupsAsString(firstComment);
        List<String> withTwoGroups = groupsAsString(thirdComment);

		// THEN
		assertThat(withThreeGroups,
                is(Arrays.asList("group subgroup", "one")));
        assertThat(withTwoGroups, is(Arrays.asList("group subgroup2", "one")));
    }

	@Test
	public void shouldFindLongestCommonPrefix() {
		// GIVEN
		String firstComment = "group one";
		givenReaderReturnsItemsWithComment(firstComment, "group two");

		// WHEN
        List<String> groups = groupsAsString(firstComment);

		// THEN
        assertThat(groups, is(Arrays.asList("group", "one")));

	}

	@Test
	public void shouldFindGroups() {
		// GIVEN
		String firstComment = "group";
		givenReaderReturnsItemsWithComment(firstComment, firstComment);

		// WHEN
        List<String> groups = groupsAsString(firstComment);

		// THEN
        assertThat(groups, is(Collections.singletonList(firstComment)));
    }

	@Test
	public void shouldCutGroupAtShorterItem()
	{
		// GIVEN
        givenReaderReturnsItemsWithComment();
        sut.insert("aaaa");
        sut.insert("aaaa bbbb");
        sut.insert("aaaa bbbb cccc");

		// WHEN
        List<String> result = groupsAsString("aaaa bbbb cccc dddd");

		// THEN
        assertThat(result, is(Arrays.asList("aaaa", "bbbb", "cccc", "dddd")));
    }

    private void givenReaderReturnsItemsWithComment(
            String... comments) {
		TimeTrackingItem items[] = new TimeTrackingItem[comments.length];
		for (int i = 0; i < comments.length; i++) {
            items[i] = new TimeTrackingItem(comments[i], LocalDateTime.now());
        }
        given(queries.queryAllItems()).willReturn(Stream.of(items));
    }

}
