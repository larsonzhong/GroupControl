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
 * Implements the logical AND operation over two or more message filters. In other words, messages
 * pass this filter if they pass <b>all</b> of the filters.
 *
 * @author larosnzhong@163.com
 */
public class AndFilter implements MessageFilter {
    /**
     * The list of filters
     */
    private final List<MessageFilter> mFilters;

    /**
     * Creates an empty AND filter. Filters should be added using the {@link
     * #addFilter(MessageFilter)} method.
     */
    public AndFilter() {
        mFilters = new ArrayList<>();
    }

    /**
     * Creates an AND filter using the specified filters.
     *
     * @param filters the filters to add
     */
    public AndFilter(MessageFilter... filters) {
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
     * Adds a filter to the filter list for the AND operation. A message will pass the filter if all
     * of the filters in the list accept it.
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
            if (!filter.accept(msg)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return mFilters.toString();
    }

}