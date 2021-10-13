package larson.groupcontrol.app.filter;

import larson.groupcontrol.app.message.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ......................-~~~~~~~~~-._       _.-~~~~~~~~~-.
 * ............... _ _.'              ~.   .~              `.__
 * ..............'//     NO           \./      BUG         \\`.
 * ............'//                     |                     \\`.
 * ..........'// .-~"""""""~~~~-._     |     _,-~~~~"""""""~-. \\`.
 * ........'//.-"                 `-.  |  .-'                 "-.\\`.
 * ......'//______.============-..   \ | /   ..-============.______\\`.
 * ....'______________________________\|/______________________________`.
 * ..larsonzhong@163.com      created in 2018/8/28     @author : larsonzhong
 * <p>
 * Implements the logical OR operation over two or more message filters. In other words, messages
 * pass this filter if they pass <b>any</b> of the filters.
 */
public class OrFilter implements MessageFilter {

    /**
     * The list of filters
     */
    private final List<MessageFilter> mFilters;

    /**
     * Creates an empty OR filter. Filters should be added using the {@link #addFilter(MessageFilter)}
     * method.
     */
    public OrFilter() {
        mFilters = new ArrayList<>();
    }

    /**
     * Creates an OR filter using the specified filters.
     *
     * @param filters the filters to add
     */
    public OrFilter(MessageFilter... filters) {
        if (filters == null) {
            throw new NullPointerException("Message filter is null.");
        }
        for (MessageFilter filter : filters) {
            if (filter == null) {
                throw new NullPointerException("Message filter is null.");
            }
        }

        mFilters = new ArrayList<>(Arrays.asList(filters));
    }

    /**
     * Adds a filter to the filter list for the OR operation. A message will pass the filter if any
     * filter in the list accepts it.
     *
     * @param filter a filter to add to the filter list
     */
    public void addFilter(MessageFilter filter) {
        if (filter == null) {
            throw new NullPointerException("Message filter is null.");
        }

        mFilters.add(filter);
    }

    @Override
    public boolean accept(Message msg) {
        for (MessageFilter filter : mFilters) {
            if (filter.accept(msg)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return mFilters.toString();
    }

}